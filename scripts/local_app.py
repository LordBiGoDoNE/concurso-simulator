"""Operate only the owned, loopback local application. No secret is passed as an argument."""
import argparse
import json
import os
import re
import secrets
import socket
import subprocess
import uuid
from pathlib import Path
from local_app_images import ROOT, OUTPUT, origin

DEFAULT_STATE = ROOT / "infra/local-app/.local"
OWNER_LABEL = "com.concursosimulator.local.owner"
SECRET_NAMES = ("db_password", "google_enabled", "google_client_id", "google_client_secret", "google_callback", "frontend_url")


def docker(*args, data=None):
    return subprocess.run(["docker", *args], input=data, capture_output=True, check=True).stdout


def local_engine():
    endpoint = os.environ.get("DOCKER_HOST")
    if not endpoint:
        context = docker("context", "show").decode().strip()
        endpoint = json.loads(docker("context", "inspect", context, "--format", "{{json .Endpoints.docker.Host}}"))
    if not endpoint.startswith(("unix://", "npipe://")):
        raise ValueError("Operação restrita ao Docker local por unix socket/named pipe")
    if docker("version", "--format", "{{.Server.Os}}").decode().strip() != "linux":
        raise ValueError("Use containers Linux")


def state_path(path):
    resolved = Path(path).resolve()
    if resolved != DEFAULT_STATE.resolve() and not resolved.is_relative_to((OUTPUT / "environments").resolve()):
        raise ValueError("Estado deve ficar em infra/local-app/.local ou backend/build/local-app/environments")
    if any(c in str(resolved) for c in "\r\n$"):
        raise ValueError("Path de estado inválido")
    return resolved


def port_available(port):
    with socket.socket() as check:
        check.bind(("127.0.0.1", port))


def owned(config):
    """Fail before mutations if a project label points at any foreign resource."""
    project = config["project"]
    if not re.fullmatch(r"concurso-simulator-app-local(?:-[a-z0-9]{1,24})?", project):
        raise ValueError("Nome de projeto fora do namespace local")
    for kind, listing, field in [("container", ("ps", "-aq"), ".Config.Labels"),
                                 ("volume", ("volume", "ls", "-q"), ".Labels"),
                                 ("network", ("network", "ls", "-q"), ".Labels")]:
        resources = docker(*listing, "--filter", "label=com.docker.compose.project=" + project).decode().split()
        for resource in resources:
            token = docker(kind, "inspect", resource, "--format", '{{index ' + field + ' "' + OWNER_LABEL + '"}}').decode().strip()
            if token != config["owner"]:
                raise ValueError("Recurso existente não pertence a este estado; nenhuma adoção automática")


def validate_images(metadata):
    if not re.fullmatch(r"[0-9a-f]{40}", metadata["source_sha"]):
        raise ValueError("SHA de imagem inválido")
    for item in metadata["images"].values():
        if not re.fullmatch(r"sha256:[0-9a-f]{64}", item["id"]):
            raise ValueError("ID de imagem inválido")
        actual = json.loads(docker("image", "inspect", item["id"], "--format", "{{json .Config}}"))
        if actual.get("User", "").split(":")[0] in ("", "0", "root") or actual.get("Labels", {}).get("org.opencontainers.image.revision") != metadata["source_sha"]:
            raise ValueError("Imagem não corresponde ao relatório ou executa como root")


def initialize(path, images_file, port, project):
    local_engine()
    path = state_path(path)
    public_origin = origin(f"http://127.0.0.1:{port}")
    metadata = json.loads(Path(images_file).read_text(encoding="utf-8"))
    if metadata["origin"] != public_origin:
        raise ValueError("Reconstrua as imagens com a mesma origem/porta")
    validate_images(metadata)
    if path.exists() and any(path.iterdir()):
        raise ValueError("Estado já existe; use start/status/stop, não inicialize novamente")
    port_available(port)
    config = {"version": 1, "project": project, "owner": uuid.uuid4().hex, "port": port,
              "origin": public_origin, "generation_enabled": True, "images": metadata}
    owned(config)
    path.mkdir(parents=True, exist_ok=True, mode=0o700)
    secret_dir = path / "secrets"
    secret_dir.mkdir(mode=0o700)
    values = (secrets.token_urlsafe(36), "false", "", "", public_origin + "/login/oauth2/code/google", public_origin)
    for name, value in zip(SECRET_NAMES, values):
        file = secret_dir / name
        with file.open("x", encoding="utf-8", newline="\n") as stream:
            stream.write(value)
        # Parent 0700 limits host access; container non-root can read explicit bind mounts.
        file.chmod(0o644)
    (path / "empty.env").write_text("# Deliberately empty; never load another .env\n", encoding="utf-8")
    (path / "state.json").write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
    print("Estado local criado; segredos somente em arquivos privados do estado.")


def load(path):
    local_engine()
    path = state_path(path)
    config = json.loads((path / "state.json").read_text(encoding="utf-8"))
    if config.get("version") != 1 or not re.fullmatch(r"[0-9a-f]{32}", config["owner"]):
        raise ValueError("Estado inválido")
    if origin(config["origin"]) != f'http://127.0.0.1:{config["port"]}':
        raise ValueError("Origem divergente")
    owned(config)
    return path, config


def compose(path, config, *args):
    env = {k: v for k, v in os.environ.items() if not k.startswith("COMPOSE_")}
    env.update({"LOCAL_OWNER": config["owner"], "LOCAL_HTTP_PORT": str(config["port"]),
                "LOCAL_ORIGIN": config["origin"], "LOCAL_SECRET_DIR": (path / "secrets").as_posix(),
                "LOCAL_API_IMAGE": config["images"]["images"]["api"]["id"],
                "LOCAL_WEB_IMAGE": config["images"]["images"]["web"]["id"],
                "LOCAL_POSTGRES_IMAGE": config["images"]["base_images"]["postgres"],
                "LOCAL_GENERATION_ENABLED": str(config["generation_enabled"]).lower()})
    return subprocess.run(["docker", "compose", "--env-file", str(path / "empty.env"),
                           "-f", str(ROOT / "infra/local-app/compose.yaml"), "-p", config["project"], *args],
                          env=env, capture_output=True, check=True).stdout


def start(path, config):
    if (path / "recovery.json").exists():
        raise ValueError("Recuperação incompleta; destino não pode ser liberado")
    validate_images(config["images"])
    web = compose(path, config, "ps", "-q", "web").decode().strip()
    if not web:
        port_available(config["port"])
    compose(path, config, "up", "-d", "--no-build", "--wait", "--wait-timeout", "150")
    print("Aplicação local pronta em " + config["origin"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--state", type=Path, default=DEFAULT_STATE)
    actions = parser.add_subparsers(dest="action", required=True)
    init = actions.add_parser("init")
    init.add_argument("--images", type=Path, default=OUTPUT / "images.json")
    init.add_argument("--port", type=int, default=8088)
    init.add_argument("--project", default="concurso-simulator-app-local")
    for action in ("start", "stop", "status"):
        actions.add_parser(action)
    args = parser.parse_args()
    try:
        if args.action == "init":
            initialize(args.state, args.images, args.port, args.project)
        else:
            path, config = load(args.state)
            if args.action == "start":
                start(path, config)
            elif args.action == "stop":
                compose(path, config, "stop")
                print("Containers parados; volume e segredos preservados.")
            else:
                print(compose(path, config, "ps", "--format", "json").decode())
    except (ValueError, OSError, KeyError, subprocess.CalledProcessError):
        parser.exit(1, "Operação local recusada/falhou; confira estado, configuração, portas e health no Docker. Nenhum segredo é impresso.\n")


if __name__ == "__main__":
    main()
