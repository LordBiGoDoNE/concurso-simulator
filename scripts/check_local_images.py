"""Exercise built image artifacts and all 51 lessons without a database or Vite."""
import io
import json
import socket
import subprocess
import tarfile
import tempfile
from pathlib import Path, PurePosixPath
import time
import uuid
import zipfile
from urllib.error import HTTPError
from urllib.parse import quote
from urllib.request import urlopen
from local_app_images import OUTPUT, ROOT, write_report
from check_attempt_artifacts import check as inspect_artifacts
from local_docker import run as docker_run, check_output as docker_output


def copied(container, path):
    raw = docker_output(["cp", container + ":" + path, "-"])
    with tarfile.open(fileobj=io.BytesIO(raw)) as archive:
        return archive.extractfile(archive.getmembers()[0]).read()


def check(report):
    metadata = json.loads(report.read_text(encoding="utf-8"))
    images = metadata["images"]
    for item in images.values():
        identity = docker_output(["image", "inspect", item["tag"], "--format", "{{.Id}}|{{.Config.User}}"], text=True).strip().split("|")
        assert identity == [item["id"], item["user"]] and item["user"].split(":")[0] not in ("", "0", "root")
    api = docker_output(["create", "--label", "com.concursosimulator.check=images", images["api"]["id"]], text=True).strip()
    web = None
    try:
        with zipfile.ZipFile(io.BytesIO(copied(api, "/app/application.jar"))) as jar:
            names = jar.namelist()
            assert any(n.endswith("ReviewController.class") for n in names)
            assert any(n.endswith("V10__completed_subject_window_index.sql") for n in names)
            assert not any(n.endswith(("Fixture.class", "Tests.class")) for n in names)
            assert not any(n.startswith("BOOT-INF/lib/") and any(t in n for t in ("junit", "mockito", "testcontainers")) for n in names)
        # Missing configuration must fail before serving; this isolated container has no network or secrets.
        probe = docker_output(["create", "--network", "none", images["api"]["id"]], text=True).strip()
        try:
            docker_run(["start", "-a", probe], capture_output=True, timeout=60)
            assert docker_output(["inspect", probe, "--format", "{{.State.ExitCode}}"], text=True).strip() != "0"
        finally:
            docker_run(["rm", "-f", probe], check=True, stdout=subprocess.DEVNULL)
        with socket.socket() as reservation:
            reservation.bind(("127.0.0.1", 0))
            port = reservation.getsockname()[1]
        web = docker_output(["run", "-d", "--name", "cs-image-check-" + uuid.uuid4().hex[:12],
                                       "--label", "com.concursosimulator.check=images", "-p", f"127.0.0.1:{port}:8080", images["web"]["id"]], text=True).strip()
        base = f"http://127.0.0.1:{port}"
        OUTPUT.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="artifact-", dir=OUTPUT) as temporary:
            directory = Path(temporary).resolve()
            assert directory.is_relative_to(OUTPUT.resolve())
            jar = directory / "application.jar"; jar.write_bytes(copied(api, "/app/application.jar"))
            dist = directory / "dist"; dist.mkdir()
            raw = docker_output(["cp", web + ":/usr/share/nginx/html/.", "-"])
            with tarfile.open(fileobj=io.BytesIO(raw)) as archive:
                # Material intentionally contains public legacy solutions; inspect the SPA independently.
                members = [m for m in archive.getmembers() if "material" not in PurePosixPath(m.name).parts]
                archive.extractall(dist, members=members, filter="data")
            inspect_artifacts(jar, dist)
        deadline = time.monotonic() + 30
        while True:
            try:
                with urlopen(base, timeout=3) as response:
                    assert response.status == 200 and b"<div id=\"root\">" in response.read()
                break
            except (OSError, AssertionError):
                if time.monotonic() > deadline:
                    raise
                time.sleep(.2)
        lessons = json.loads((ROOT / "frontend/src/lesson-paths.json").read_text(encoding="utf-8"))
        assert len(lessons) == 51
        for path in lessons.values():
            with urlopen(base + "/material/" + quote(path, safe="/"), timeout=5) as response:
                assert response.status == 200 and b"<html" in response.read().lower()
        try:
            urlopen(base + "/api/v1/status", timeout=6)
            raise AssertionError("API ausente não pode parecer disponível")
        except HTTPError as error:
            assert error.code == 503 and "no-store" in error.headers["Cache-Control"]
            assert json.loads(error.read()) == {"error": "unavailable"}
        write_report(OUTPUT / "image-check.json", {"status": "passed", "source_sha": metadata["source_sha"],
                     "images": images, "lessons": 51, "api_absent": 503, "missing_config_rejected": True, "spa_artifacts_inspected": True,
                     "limits": "JAR/static/proxy inspection; database/account flows are separate checks."})
        print("IMAGE_CHECK_PASS: non-root, production JAR, 51 lessons and bounded API failure")
    finally:
        # Only IDs returned by our create/run; no volumes or other projects are removed.
        if web:
            docker_run(["rm", "-f", web], check=True, stdout=subprocess.DEVNULL)
        docker_run(["rm", api], check=True, stdout=subprocess.DEVNULL)


if __name__ == "__main__":
    check(OUTPUT / "images.json")
