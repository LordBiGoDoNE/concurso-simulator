"""Verification only: separate JVM/test classpath against an owned disposable Compose DB."""
import json
from local_app import ROOT, OUTPUT, OWNER_LABEL, docker, owned


def fixture(path, config, mode="seed", user=None):
    assert path.resolve().is_relative_to((OUTPUT / "environments").resolve())
    assert path.name.startswith("check-") and config["project"].startswith("concurso-simulator-app-local-check")
    owned(config)
    classes = ROOT / "backend/build/local-delivery-fixture"
    if not (classes / "classes/br/com/concursosimulator/mockexam/LocalDeliveryFixture.class").is_file():
        raise ValueError("Execute backend/gradlew localDeliveryFixtureClasses antes do ensaio")
    image = json.loads((ROOT / "infra/local-app/images.lock.json").read_text())["images"]["java-build"]
    raw = docker("run", "--rm", "--network", config["project"] + "_database", "--read-only",
                 "--label", OWNER_LABEL + "=" + config["owner"], "--tmpfs", "/tmp:rw,noexec,nosuid,size=64m",
                 "--mount", f"type=bind,source={classes},target=/fixture,readonly",
                 "--mount", f"type=bind,source={path / 'secrets/db_password'},target=/run/secrets/db_password,readonly",
                 image, "java", "-cp", "/fixture/classes:/fixture/lib/*",
                 "br.com.concursosimulator.mockexam.LocalDeliveryFixture", mode, *([user] if user else []))
    lines = raw.decode().splitlines()
    return json.loads(next(line.split("=", 1)[1] for line in lines if line.startswith("LOCAL_FIXTURE_JSON=")))
