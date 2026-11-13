"""Operate the production runtime on the VPS (concurso-dev, rootless Docker). No secret is printed or passed as argument."""
import argparse
import hashlib
import io
import json
import os
import re
import secrets
import subprocess
import tarfile
import uuid
from contextlib import contextmanager
from pathlib import Path, PurePosixPath

from local_app import docker, port_available, owned, validate_images
from local_app_images import ROOT, committed_context, permitted
from local_docker import run as docker_run, check_output as docker_output, endpoint

HOST = "concurso-simulator.reemsoftworks.com"
ORIGIN = "https://" + HOST
PROJECT = "concurso-simulator-prod"
NAMESPACE = re.escape(PROJECT)
OWNER_LABEL = "com.concursosimulator.production.owner"
PORT = 18090
DEFAULT_STATE = Path.home() / ".local/share/concurso-simulator/production"
PRODUCTION_FILES = {"web.Dockerfile", "nginx.conf"}
SECRET_NAMES = ("db_password", "admin_password", "google_enabled", "google_client_id",
                "google_client_secret", "google_callback", "frontend_url")


def production_permitted(name):
    path = PurePosixPath(name)
    if path.parent.as_posix() == "infra/vps-production" and path.name in PRODUCTION_FILES:
        return True
    return permitted(name)


def state_path(path):
    if not Path(path).expanduser().is_absolute():
        raise ValueError("Estado deve ser um caminho absoluto")
    resolved = Path(path).expanduser().resolve()
    if any(c in str(resolved) for c in "\r\n$"):
        raise ValueError("Path de estado inválido")
    if resolved.is_relative_to(ROOT.resolve()):
        raise ValueError("Estado de produção não pode ficar dentro do checkout")
    return resolved


def on_main(sha, root=ROOT):
    """Release candidates must already be in the fetched origin/main."""
    result = subprocess.run(["git", "merge-base", "--is-ancestor", sha, "refs/remotes/origin/main"],
                            cwd=root, capture_output=True)
    if result.returncode not in (0, 1):
        raise ValueError("Não foi possível conferir origin/main; execute git fetch origin")
    return result.returncode == 0


def write_private_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    temporary = path.with_suffix(".tmp")
    with temporary.open("w", encoding="utf-8", newline="\n") as stream:
        stream.write(json.dumps(value, indent=2, ensure_ascii=False) + "\n")
    temporary.chmod(0o600)
    temporary.replace(path)


def linux_engine():
    endpoint()
    if docker_output(["version", "--format", "{{.Server.Os}}"], text=True).strip() != "linux":
        raise ValueError("Docker deve executar containers Linux")


def build(revision, state):
    linux_engine()
    state = state_path(state)
    sha, context, selected = committed_context(revision, allow=production_permitted)
    with tarfile.open(fileobj=io.BytesIO(context)) as archive:
        names = set(archive.getnames())
        if not {"infra/vps-production/web.Dockerfile", "infra/vps-production/nginx.conf"} <= names:
            raise ValueError("Commit sem os arquivos de produção")
        lock = json.load(archive.extractfile("infra/local-app/images.lock.json"))
    for image in lock["images"].values():
        if not re.fullmatch(r"[a-z0-9/-]+@sha256:[0-9a-f]{64}", image):
            raise ValueError("Imagem base sem digest fixado")
    images = {}
    for service, dockerfile in [("api", "infra/local-app/backend.Dockerfile"), ("web", "infra/vps-production/web.Dockerfile")]:
        tag = f"{PROJECT}-{service}:{sha[:12]}"
        command = ["build", "--builder", "default", "--progress=plain", "-f", dockerfile,
                   "--label", f"org.opencontainers.image.revision={sha}", "-t", tag]
        if service == "web":
            command += ["--build-arg", f"APP_ORIGIN={ORIGIN}"]
        docker_run(command + ["-"], input=context, check=True)
        identity = docker_output(["image", "inspect", tag, "--format", "{{.Id}}|{{.Config.User}}"], text=True).strip().split("|")
        if identity[1].split(":")[0] in ("", "0", "root"):
            raise ValueError("Runtime não pode executar como root")
        images[service] = {"tag": tag, "id": identity[0], "user": identity[1]}
    manifest = {"version": 1, "source_sha": sha, "origin": ORIGIN, "on_main": on_main(sha),
                "images": images, "base_images": lock["images"],
                "context_sha256": hashlib.sha256(context).hexdigest(), "files": selected}
    target = state.parent / "releases" / f"{sha[:12]}.json"
    write_private_json(target, manifest)
    print(f"Release {sha[:12]} construída (main={manifest['on_main']}); manifesto: {target}")


def initialize(state, images_file, rehearsal):
    linux_engine()
    state = state_path(state)
    metadata = json.loads(Path(images_file).read_text(encoding="utf-8"))
    if metadata.get("origin") != ORIGIN:
        raise ValueError("Manifesto construído para outra origem")
    if not metadata.get("on_main") and not rehearsal:
        raise ValueError("Commit fora de origin/main; somente --rehearsal aceita")
    validate_images(metadata)
    if (state / "state.json").exists() or state.exists() and any(state.iterdir()):
        raise ValueError("Estado já existe; não inicialize novamente")
    port_available(PORT)
    config = {"version": 1, "project": PROJECT, "owner": uuid.uuid4().hex, "port": PORT,
              "origin": ORIGIN, "rehearsal": bool(rehearsal), "generation_enabled": True, "images": metadata}
    owned(config, NAMESPACE, OWNER_LABEL)
    state.mkdir(parents=True, exist_ok=True, mode=0o700)
    state.chmod(0o700)
    secret_dir = state / "secrets"
    secret_dir.mkdir(mode=0o700)
    values = (secrets.token_urlsafe(36), secrets.token_urlsafe(24), "false", "", "",
              ORIGIN + "/login/oauth2/code/google", ORIGIN)
    for name, value in zip(SECRET_NAMES, values):
        with (secret_dir / name).open("x", encoding="utf-8", newline="\n") as stream:
            stream.write(value)
        # Parent 0700 limits host access; non-root container users read explicit bind mounts.
        (secret_dir / name).chmod(0o644)
    (state / "empty.env").write_text("# Deliberately empty; never load another .env\n", encoding="utf-8")
    write_private_json(state / "state.json", config)
    print("Estado de produção criado; senha inicial do admin em secrets/admin_password (não impressa).")


def load(state):
    linux_engine()
    state = state_path(state)
    config = json.loads((state / "state.json").read_text(encoding="utf-8"))
    if (config.get("version") != 1 or config.get("project") != PROJECT or config.get("port") != PORT
            or config.get("origin") != ORIGIN or not re.fullmatch(r"[0-9a-f]{32}", config.get("owner", ""))):
        raise ValueError("Estado inválido")
    if not isinstance(config.get("generation_enabled"), bool) or not isinstance(config.get("rehearsal"), bool):
        raise ValueError("Estado inválido")
    if not config["images"].get("on_main") and not config["rehearsal"]:
        raise ValueError("Release fora de origin/main exige estado de ensaio")
    owned(config, NAMESPACE, OWNER_LABEL)
    return state, config


@contextmanager
def operation(state):
    """Serialize operator commands; never break a stale lock automatically."""
    lock = state / "operation.lock"
    token = uuid.uuid4().hex
    with lock.open("x", encoding="utf-8") as stream:
        stream.write(token)
    try:
        yield
    finally:
        if lock.read_text(encoding="utf-8") != token:
            raise ValueError("Lock alterado; preserve o estado e investigue")
        lock.unlink()


def compose_env(state, config):
    env = {k: v for k, v in os.environ.items() if not k.startswith("COMPOSE_")}
    env.update({"PROD_OWNER": config["owner"], "PROD_HTTP_PORT": str(config["port"]),
                "PROD_SECRET_DIR": (state / "secrets").as_posix(),
                "PROD_API_IMAGE": config["images"]["images"]["api"]["id"],
                "PROD_WEB_IMAGE": config["images"]["images"]["web"]["id"],
                "PROD_POSTGRES_IMAGE": config["images"]["base_images"]["postgres"],
                "PROD_GENERATION_ENABLED": str(config["generation_enabled"]).lower()})
    return env


def compose(state, config, *args):
    return docker_run(["compose", "--env-file", str(state / "empty.env"),
                       "-f", str(ROOT / "infra/vps-production/compose.yaml"), "-p", PROJECT, *args],
                      env=compose_env(state, config), capture_output=True, check=True).stdout


def start(state, config):
    validate_images(config["images"])
    if not compose(state, config, "ps", "-q", "web").decode().strip():
        port_available(config["port"])
    compose(state, config, "up", "-d", "--no-build", "--wait", "--wait-timeout", "180")
    print(f"Produção pronta em 127.0.0.1:{config['port']} (release {config['images']['source_sha'][:12]}).")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--state", type=Path, default=DEFAULT_STATE)
    actions = parser.add_subparsers(dest="action", required=True)
    build_action = actions.add_parser("build")
    build_action.add_argument("--revision", required=True)
    init = actions.add_parser("init")
    init.add_argument("--images", type=Path, required=True)
    init.add_argument("--rehearsal", action="store_true")
    for action in ("start", "stop", "status"):
        actions.add_parser(action)
    args = parser.parse_args()
    try:
        if args.action == "build":
            build(args.revision, args.state)
        elif args.action == "init":
            initialize(args.state, args.images, args.rehearsal)
        else:
            state, config = load(args.state)
            if args.action == "status":
                print(compose(state, config, "ps", "--format", "{{.Service}} {{.Status}}").decode())
            else:
                with operation(state):
                    if args.action == "start":
                        start(state, config)
                    else:
                        compose(state, config, "stop")
                        print("Containers parados; volume e segredos preservados.")
    except (ValueError, OSError, KeyError, subprocess.CalledProcessError):
        parser.exit(1, "Operação de produção recusada/falhou; confira estado, release e health. Nenhum segredo é impresso.\n")


if __name__ == "__main__":
    main()
