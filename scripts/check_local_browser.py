"""Browser against production images; synthetic identity exists only in the separate helper."""
import json
import os
import subprocess
from check_local_recovery import environment
from check_local_runtime import cleanup
from local_app import ROOT, OUTPUT, start
from local_app_data import private_json
from local_app_fixture import fixture
from local_app_images import write_report


def check():
    images = OUTPUT / "images.json"
    metadata = json.loads(images.read_text(encoding="utf-8"))
    path, config = environment(images, int(metadata["origin"].rsplit(":", 1)[1]))
    try:
        start(path, config)
        report = path / "browser-fixture-private.json"
        private_json(report, fixture(path, config))
        env = dict(os.environ, LOCAL_APP_FIXTURE=str(report), LOCAL_APP_ORIGIN=config["origin"])
        npx = "npx.cmd" if os.name == "nt" else "npx"
        subprocess.run([npx, "playwright", "test", "--config", "playwright.local.config.ts"],
                       cwd=ROOT / "frontend", env=env, check=True)
        write_report(OUTPUT / "browser-check.json", {"status": "passed", "source_sha": metadata["source_sha"],
                     "checks": ["visitor-csrf-retry-finish-review", "account-two-sessions-conflict-resume", "other-account-404", "51-subjects-lessons-directed-practice", "mobile-desktop"],
                     "limits": "Synthetic sessions injected by separate test helper; actual built SPA/proxy/JAR/PostgreSQL, no Google claim or Vite."})
    finally:
        cleanup(path, config)


if __name__ == "__main__":
    check()
