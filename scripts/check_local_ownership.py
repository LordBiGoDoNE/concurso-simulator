"""Reproduce an unlabelled Compose-name collision and preserve its synthetic contents."""
import json
import uuid
from check_local_recovery import environment
from local_app import OUTPUT, docker, start, compose
from local_app_images import write_report


def check():
    images = OUTPUT / "images.json"
    metadata = json.loads(images.read_text(encoding="utf-8"))
    path, config = environment(images, int(metadata["origin"].rsplit(":", 1)[1]))
    name = config["project"] + "_postgres_data"
    token = uuid.uuid4().hex
    label = "com.concursosimulator.test-ownership"
    assert docker("volume", "create", "--label", label + "=" + token, name).decode().strip() == name
    image = metadata["base_images"]["python-build"]
    try:
        docker("run", "--rm", "--mount", f"type=volume,source={name},target=/evidence", image, "python", "-c",
               "from pathlib import Path;Path('/evidence/sentinel').write_text('SYNTHETIC_FOREIGN_VOLUME')")
        try:
            start(path, config)
            raise AssertionError("Unowned exact-name volume accepted")
        except ValueError:
            pass
        assert not compose(path, config, "ps", "-aq").strip()
        assert docker("run", "--rm", "--mount", f"type=volume,source={name},target=/evidence,readonly", image, "python", "-c",
                      "from pathlib import Path;print(Path('/evidence/sentinel').read_text())").strip() == b"SYNTHETIC_FOREIGN_VOLUME"
        write_report(OUTPUT / "ownership-check.json", {"status": "passed", "source_sha": metadata["source_sha"],
                     "checks": ["unlabelled-exact-name-volume-refused-before-compose-up", "preexisting-synthetic-contents-preserved"]})
        print("OWNERSHIP_CHECK_PASS: foreign volume refused and preserved")
    finally:
        # Only the exact volume created above, additionally identified by this test's unique label.
        labels = json.loads(docker("volume", "inspect", name, "--format", "{{json .Labels}}"))
        assert labels.get(label) == token
        docker("volume", "rm", name)


if __name__ == "__main__":
    check()
