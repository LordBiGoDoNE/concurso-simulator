"""Build local images from a restricted committed-source tar; never send workspace secrets."""
import argparse
import hashlib
import io
import json
import re
import subprocess
import tarfile
from pathlib import Path, PurePosixPath
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "backend/build/local-app"
SCRIPTS = {"build_pages.py", "enrich_math.py", "math_content.py", "compact_sources.py", "check_math.py", "check_links.py"}
BUILD_FILES = {"backend/build.gradle.kts", "backend/settings.gradle.kts", "backend/gradlew",
               "frontend/package.json", "frontend/package-lock.json", "frontend/tsconfig.json",
               "frontend/index.html", "frontend/vite.config.ts"}
CONTAINER_FILES = {"backend.Dockerfile", "web.Dockerfile", "nginx.conf", "Health.java", "images.lock.json"}


def origin(value):
    try:
        uri = urlsplit(value)
        if (uri.scheme != "http" or uri.hostname != "127.0.0.1" or uri.username or uri.password
                or uri.path not in ("", "/") or uri.query or uri.fragment or not uri.port):
            raise ValueError()
        if not 1024 <= uri.port <= 65535:
            raise ValueError()
    except ValueError:
        raise ValueError("Origem deve ser http://127.0.0.1:<porta entre 1024 e 65535>") from None
    return f"http://127.0.0.1:{uri.port}"


def permitted(name):
    path = PurePosixPath(name)
    if path.is_absolute() or ".." in path.parts or "\\" in name:
        raise ValueError("Path inválido no arquivo de fontes")
    if name == ".nojekyll":
        return True
    if any(p.startswith(".") for p in path.parts) or path.suffix in {".pem", ".key", ".pfx", ".p12", ".log"}:
        return False
    if name in BUILD_FILES:
        return True
    if name.startswith(("backend/src/main/", "backend/gradle/wrapper/")):
        return True
    if name.startswith("frontend/src/"):
        return not re.search(r"(?:\.test\.[^/]+$|/test-setup\.ts$)", name)
    if path.parent.as_posix() == "scripts" and path.name in SCRIPTS:
        return True
    if path.parent.as_posix() == "infra/local-app" and path.name in CONTAINER_FILES:
        return True
    return (name in {"index.html", "manifesto_v3.json", "catalogo_fontes_questoes.csv"}
            or len(path.parts) == 1 and re.fullmatch(r"00_[^/]+\.html", name) is not None
            or path.parts[0] == "assets" or re.fullmatch(r"0[1-5]_[^/]+", path.parts[0]) is not None)


def restricted_tar(raw):
    """Only regular allowlisted tracked files; links and traversal fail closed."""
    output = io.BytesIO()
    selected = {}
    with tarfile.open(fileobj=io.BytesIO(raw)) as source, tarfile.open(fileobj=output, mode="w") as target:
        for member in source:
            if member.isdir():
                continue
            if not permitted(member.name):
                continue
            if not member.isfile():
                raise ValueError("Link ou arquivo especial no contexto permitido")
            data = source.extractfile(member).read()
            info = tarfile.TarInfo(member.name)
            info.mode = 0o755 if member.name == "backend/gradlew" else 0o644
            info.size = len(data)
            target.addfile(info, io.BytesIO(data))
            selected[member.name] = hashlib.sha256(data).hexdigest()
    return output.getvalue(), selected


def committed_context(revision="HEAD", root=ROOT):
    sha = subprocess.check_output(["git", "rev-parse", "--verify", "--end-of-options", revision + "^{commit}"], cwd=root, text=True).strip()
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Commit inválido")
    raw = subprocess.check_output(["git", "archive", "--format=tar", sha], cwd=root)
    context, selected = restricted_tar(raw)
    return sha, context, selected


def write_report(path, value):
    path = Path(path).resolve()
    if not path.is_relative_to(OUTPUT.resolve()):
        raise ValueError("Relatórios devem ficar em backend/build/local-app")
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    temporary.replace(path)


def build(revision, public_origin, report):
    public_origin = origin(public_origin)
    if subprocess.check_output(["docker", "version", "--format", "{{.Server.Os}}"], text=True).strip() != "linux":
        raise ValueError("Docker deve executar containers Linux")
    sha, context, selected = committed_context(revision)
    with tarfile.open(fileobj=io.BytesIO(context)) as archive:
        lock = json.load(archive.extractfile("infra/local-app/images.lock.json"))
        for image in lock["images"].values():
            if not re.fullmatch(r"[a-z0-9/-]+@sha256:[0-9a-f]{64}", image):
                raise ValueError("Imagem base sem digest fixado")
    suffix = sha[:12] + "-" + hashlib.sha256(public_origin.encode()).hexdigest()[:8]
    images = {}
    for service, dockerfile in [("api", "backend"), ("web", "web")]:
        tag = f"concurso-simulator-{service}:{suffix}"
        command = ["docker", "build", "--progress=plain", "-f", f"infra/local-app/{dockerfile}.Dockerfile",
                   "--label", f"org.opencontainers.image.revision={sha}", "-t", tag]
        if service == "web":
            command += ["--build-arg", f"LOCAL_APP_ORIGIN={public_origin}"]
        subprocess.run(command + ["-"], input=context, check=True)
        identity = subprocess.check_output(["docker", "image", "inspect", tag, "--format", "{{.Id}}|{{.Config.User}}"], text=True).strip().split("|")
        if identity[1].split(":")[0] in ("", "0", "root"):
            raise ValueError("Runtime não pode executar como root")
        images[service] = {"tag": tag, "id": identity[0], "user": identity[1]}
    result = {"version": 1, "source_sha": sha, "origin": public_origin, "images": images,
              "base_images": lock["images"], "context_sha256": hashlib.sha256(context).hexdigest(),
              "files": selected, "limits": "IDs identificam esta construção; não garantem reprodução byte a byte."}
    write_report(report, result)
    print(f"Imagens construídas do commit {sha}; relatório: {report}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--revision", default="HEAD")
    parser.add_argument("--origin", default="http://127.0.0.1:8088")
    parser.add_argument("--report", type=Path, default=OUTPUT / "images.json")
    args = parser.parse_args()
    try:
        build(args.revision, args.origin, args.report)
    except (ValueError, subprocess.CalledProcessError, OSError) as error:
        parser.exit(1, f"Build local recusado/falhou: {type(error).__name__}. Confira a configuração e os logs sem imprimir segredos.\n")


if __name__ == "__main__":
    main()
