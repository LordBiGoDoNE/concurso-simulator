"""Run the local procedure from a clean committed export, retaining private reports locally."""
import io
import json
import os
import re
import subprocess
import sys
import tarfile
import uuid
from local_app_images import ROOT, OUTPUT, write_report


def check():
    sha = subprocess.check_output(["git", "-C", str(ROOT), "rev-parse", "HEAD"], text=True).strip()
    git_dir = subprocess.check_output(["git", "-C", str(ROOT), "rev-parse", "--absolute-git-dir"], text=True).strip()
    raw = subprocess.check_output(["git", "-C", str(ROOT), "archive", "--format=tar", sha])
    # Keep Windows bind-mount paths short; deep exported secret paths failed for non-root Desktop guests.
    # Only committed public source is exported here; test state/secrets retain their own private permissions.
    destination = ROOT / (".local-app-export-" + uuid.uuid4().hex[:8])
    destination.mkdir(mode=0o755)
    with tarfile.open(fileobj=io.BytesIO(raw)) as archive:
        if any(not (item.isfile() or item.isdir()) for item in archive.getmembers()):
            raise ValueError("Exportação recusa links/arquivos especiais")
        archive.extractall(destination, filter="data")
    # Git is read-only provenance for the image builder; the exported worktree has no .git/private files.
    env = dict(os.environ, GIT_DIR=git_dir, GIT_WORK_TREE=str(destination))
    npm = "npm.cmd" if os.name == "nt" else "npm"
    gradle = str(destination / "backend" / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    commands = [
        ([sys.executable, "scripts/local_app_images.py", "--revision", sha, "--origin", "http://127.0.0.1:18088"], destination),
        ([gradle, "--no-daemon", "--console=plain", "localDeliveryFixtureClasses"], destination / "backend"),
        ([npm, "ci"], destination / "frontend"),
        ([sys.executable, "scripts/check_local_images.py"], destination),
        ([sys.executable, "scripts/check_local_ownership.py"], destination),
        ([sys.executable, "scripts/check_local_runtime.py"], destination),
        ([sys.executable, "scripts/check_local_recovery.py"], destination),
        ([sys.executable, "scripts/check_local_browser.py"], destination),
    ]
    for number, (command, cwd) in enumerate(commands, 1):
        print(f"Exportação limpa: verificação {number}/{len(commands)}", flush=True)
        with (destination / f"operation-{number}.log").open("wb") as log:
            try:
                subprocess.run(command, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
            except subprocess.CalledProcessError:
                log.flush()
                # Public diagnostics contain only source filename/line and known error categories, no log payloads.
                text = (destination / f"operation-{number}.log").read_text(encoding="utf-8", errors="replace")
                locations = re.findall(r'File "([^"\n]+\.py)", line (\d+)', text)
                if locations:
                    file, line = locations[-1]
                    print(f"Falha etapa {number}; origem {file.replace(chr(92), '/').rsplit('/', 1)[-1]}:{line}", flush=True)
                diagnostic = destination / "backend/build/local-app/runtime-container-private.log"
                if command[-1] == "scripts/check_local_runtime.py" and diagnostic.is_file():
                    private = diagnostic.read_text(encoding="utf-8", errors="replace")
                    for category in ("Input/output error", "Permission denied", "AccessDeniedException", "OutOfMemoryError"):
                        if category in private:
                            print("Categoria de falha: " + category, flush=True)
                raise
    reports = {name: json.loads((destination / "backend/build/local-app" / (name + ".json")).read_text(encoding="utf-8"))
               for name in ("image-check", "ownership-check", "runtime-check", "recovery-check", "browser-check")}
    assert all(report["status"] == "passed" and report["source_sha"] == sha for report in reports.values())
    write_report(OUTPUT / "clean-delivery-check.json", {"status": "passed", "source_sha": sha,
                 "export": str(destination.relative_to(ROOT)), "reports": reports,
                 "limits": "Committed export with read-only Git provenance; dependencies downloaded, Windows/Linux use Docker Linux. No operator database, deployment or byte reproducibility claim."})
    print("CLEAN_DELIVERY_PASS: image/build/start/stop/backup/restore/restart/browser from committed export")


if __name__ == "__main__":
    check()
