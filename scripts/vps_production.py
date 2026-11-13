"""Operate the production runtime on the VPS (concurso-dev, rootless Docker). No secret is printed or passed as argument."""
import argparse
import base64
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
NAMESPACE = re.escape(PROJECT) + r"(?:-recovery-[a-z0-9]{1,16})?"
OWNER_LABEL = "com.concursosimulator.production.owner"
PORT = 18090
RECOVERY_PORTS = range(18091, 18100)
DATA_ROOT = Path.home() / ".local/share/concurso-simulator"
DEFAULT_STATE = DATA_ROOT / "production"
PRODUCTION_FILES = {"web.Dockerfile", "nginx.conf"}
SECRET_NAMES = ("db_password", "admin_password", "google_enabled", "google_client_id",
                "google_client_secret", "google_callback", "frontend_url",
                "question_import_destination", "question_import_key")
# Generated values that an older state may lack; created only when missing, never rotated here.
GENERATED_SECRETS = {"question_import_destination": lambda config: config["project"],
                     "question_import_key": lambda config: base64.b64encode(secrets.token_bytes(48)).decode()}


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


def target(recovery=None, port=None):
    """Production serves 18090; recovery targets are separate projects on 18091–18099, never behind nginx."""
    if recovery is None:
        if port not in (None, PORT):
            raise ValueError("Produção usa somente a porta 18090")
        return PROJECT, PORT
    if not re.fullmatch(r"[a-z0-9]{1,16}", recovery):
        raise ValueError("Nome de recuperação inválido")
    if port not in RECOVERY_PORTS:
        raise ValueError("Recuperação exige --port entre 18091 e 18099")
    return f"{PROJECT}-recovery-{recovery}", port


def valid_target(config):
    project, port = config.get("project"), config.get("port")
    if project == PROJECT:
        return port == PORT
    return isinstance(project, str) and re.fullmatch(NAMESPACE, project) is not None and port in RECOVERY_PORTS


def initialize(state, images_file, rehearsal, recovery=None, port=None):
    linux_engine()
    state = state_path(state)
    project, port = target(recovery, port)
    metadata = json.loads(Path(images_file).read_text(encoding="utf-8"))
    if metadata.get("origin") != ORIGIN:
        raise ValueError("Manifesto construído para outra origem")
    if not metadata.get("on_main") and not rehearsal:
        raise ValueError("Commit fora de origin/main; somente --rehearsal aceita")
    validate_images(metadata)
    if (state / "state.json").exists() or state.exists() and any(state.iterdir()):
        raise ValueError("Estado já existe; não inicialize novamente")
    port_available(port)
    config = {"version": 1, "project": project, "owner": uuid.uuid4().hex, "port": port,
              "origin": ORIGIN, "rehearsal": bool(rehearsal), "generation_enabled": True, "images": metadata}
    owned(config, NAMESPACE, OWNER_LABEL)
    state.mkdir(parents=True, exist_ok=True, mode=0o700)
    state.chmod(0o700)
    secret_dir = state / "secrets"
    secret_dir.mkdir(mode=0o700)
    values = (secrets.token_urlsafe(36), secrets.token_urlsafe(24), "false", "", "",
              ORIGIN + "/login/oauth2/code/google", ORIGIN,
              GENERATED_SECRETS["question_import_destination"](config), GENERATED_SECRETS["question_import_key"](config))
    for name, value in zip(SECRET_NAMES, values, strict=True):
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
    if (config.get("version") != 1 or not valid_target(config)
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
                       "-f", str(ROOT / "infra/vps-production/compose.yaml"), "-p", config["project"], *args],
                      env=compose_env(state, config), capture_output=True, check=True).stdout


def ensure_generated_secrets(state, config):
    created = []
    for name, generate in GENERATED_SECRETS.items():
        path = state / "secrets" / name
        if path.is_symlink():
            raise ValueError("Segredo não pode ser link")
        if not path.exists():
            with path.open("x", encoding="utf-8", newline="\n") as stream:
                stream.write(generate(config))
            path.chmod(0o644)
            created.append(name)
    if created:
        print("Segredos gerados que faltavam: " + ", ".join(created) + " (valores não impressos).")


def start(state, config):
    ensure_generated_secrets(state, config)
    validate_images(config["images"])
    if not compose(state, config, "ps", "-q", "web").decode().strip():
        port_available(config["port"])
    compose(state, config, "up", "-d", "--no-build", "--wait", "--wait-timeout", "180")
    print(f"Produção pronta em 127.0.0.1:{config['port']} (release {config['images']['source_sha'][:12]}).")


GOOGLE_CLIENT_ID = re.compile(r"[0-9]{6,32}-[a-z0-9]{8,64}\.apps\.googleusercontent\.com")
GOOGLE_SECRET = re.compile(r"[A-Za-z0-9_-]{20,128}")


def rewrite_secret(state, name, value):
    """Rewrite in place: Compose bind-mounts the file, and a rename would leave the container on the old inode."""
    path = state / "secrets" / name
    if path.is_symlink() or not path.is_file():
        raise ValueError("Arquivo de segredo ausente ou inválido")
    with path.open("r+", encoding="utf-8", newline="\n") as stream:
        stream.seek(0)
        stream.write(value)
        stream.truncate()


def google_enabled_on_api(config):
    import urllib.request
    request = urllib.request.Request(f"http://127.0.0.1:{config['port']}/api/v1/auth/config", headers={"Host": HOST})
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.load(response).get("googleEnabled") is True


def restart_api(state, config):
    compose(state, config, "restart", "api")
    compose(state, config, "up", "-d", "--no-build", "--wait", "--wait-timeout", "180", "api")


def configure_google(state, config, read_line=input, read_secret=None):
    """Interactive only; values are never printed, logged or accepted as arguments."""
    import getpass
    read_secret = read_secret or getpass.getpass
    client_id = read_line("Client ID do Google: ").strip()
    secret = read_secret("Client secret (não aparece na tela): ").strip()
    if not GOOGLE_CLIENT_ID.fullmatch(client_id) or not GOOGLE_SECRET.fullmatch(secret):
        raise ValueError("Client ID ou secret em formato inesperado; nada foi alterado")
    for name, value in (("google_client_id", client_id), ("google_client_secret", secret),
                        ("google_callback", ORIGIN + "/login/oauth2/code/google"), ("frontend_url", ORIGIN)):
        rewrite_secret(state, name, value)
    rewrite_secret(state, "google_enabled", "true")
    try:
        restart_api(state, config)
        if not google_enabled_on_api(config):
            raise ValueError("API não confirmou googleEnabled")
    except Exception:
        rewrite_secret(state, "google_enabled", "false")
        restart_api(state, config)
        raise ValueError("Configuração Google recusada pela API; Google voltou a ficar desligado") from None
    print("Login Google ligado. Callback: " + ORIGIN + "/login/oauth2/code/google")


def disable_google(state, config):
    rewrite_secret(state, "google_enabled", "false")
    restart_api(state, config)
    print("Login Google desligado; client ID/secret preservados nos arquivos privados.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--state", type=Path, default=DEFAULT_STATE)
    actions = parser.add_subparsers(dest="action", required=True)
    build_action = actions.add_parser("build")
    build_action.add_argument("--revision", required=True)
    init = actions.add_parser("init")
    init.add_argument("--images", type=Path, required=True)
    init.add_argument("--rehearsal", action="store_true")
    init.add_argument("--recovery", help="nome do alvo de recuperação separado (a-z0-9)")
    init.add_argument("--port", type=int, help="porta do alvo de recuperação (18091–18099)")
    for action in ("start", "stop", "status", "backup", "rollback", "configure-google", "disable-google"):
        actions.add_parser(action)
    restore_action = actions.add_parser("restore")
    restore_action.add_argument("--backup", type=Path, required=True)
    restore_action.add_argument("--confirm")
    release_action = actions.add_parser("release")
    release_action.add_argument("--images", type=Path, required=True)
    args = parser.parse_args()
    try:
        if args.action == "build":
            build(args.revision, args.state)
        elif args.action == "init":
            initialize(args.state, args.images, args.rehearsal, args.recovery, args.port)
        else:
            state, config = load(args.state)
            if args.action == "status":
                print(compose(state, config, "ps", "--format", "{{.Service}} {{.Status}}").decode())
            else:
                with operation(state):
                    if args.action == "start":
                        if (state / "recovery.json").exists():
                            raise ValueError("Recuperação incompleta; destino bloqueado")
                        start(state, config)
                    elif args.action == "configure-google":
                        configure_google(state, config)
                    elif args.action == "disable-google":
                        disable_google(state, config)
                    elif args.action == "stop":
                        compose(state, config, "stop")
                        print("Containers parados; volume e segredos preservados.")
                    else:
                        import vps_production_data as data
                        if args.action == "backup":
                            data.backup(state, config)
                        elif args.action == "restore":
                            data.restore(state, config, args.backup, args.confirm)
                        elif args.action == "release":
                            data.release(state, config, args.images)
                        else:
                            data.rollback(state, config)
    except (ValueError, OSError, KeyError, subprocess.CalledProcessError):
        parser.exit(1, "Operação de produção recusada/falhou; confira estado, release e health. Nenhum segredo é impresso.\n")


if __name__ == "__main__":
    main()
