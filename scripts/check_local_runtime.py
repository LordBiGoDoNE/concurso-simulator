"""Real local Compose smoke with an isolated empty database; no operator state is used."""
import http.cookiejar
import json
import subprocess
import time
import uuid
from urllib.error import HTTPError
from urllib.request import Request, build_opener, HTTPCookieProcessor
from local_app import initialize, load, start, compose, docker, owned
from local_app_images import OUTPUT, write_report


def request(client, base, path, method="GET", headers=None):
    req = Request(base + path, data=b"" if method == "POST" else None, method=method, headers=headers or {})
    try:
        response = client.open(req, timeout=8)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read()
        return response.code, response.headers, json.loads(raw) if raw and "json" in response.headers.get("Content-Type", "") else raw


def sql(path, config, statement):
    container = compose(path, config, "ps", "-aq", "postgres").decode().strip()
    return docker("exec", container, "psql", "-X", "-U", "concurso", "-d", "concurso_simulator", "-At", "-v", "ON_ERROR_STOP=1", "-c", statement).decode().strip()


def cleanup(path, config):
    # Verification-only deletion, limited to an owned disposable namespace created here.
    assert path.resolve().is_relative_to((OUTPUT / "environments").resolve())
    assert path.name.startswith("check-") and config["project"].startswith("concurso-simulator-app-local-check")
    owned(config)
    compose(path, config, "down", "--volumes")


def check():
    images = OUTPUT / "images.json"
    metadata = json.loads(images.read_text(encoding="utf-8"))
    port = int(metadata["origin"].rsplit(":", 1)[1])
    token = uuid.uuid4().hex[:12]
    state = OUTPUT / "environments" / ("check-" + token)
    initialize(state, images, port, "concurso-simulator-app-local-check" + token)
    path, config = load(state)
    try:
        start(path, config)
        base = config["origin"]
        client = build_opener(HTTPCookieProcessor(http.cookiejar.CookieJar()))
        status, _, body = request(client, base, "/api/v1/status")
        assert status == 200 and body == {"status": "UP"}
        assert request(client, base, "/api/v1/auth/config")[2] == {"googleEnabled": False}
        status, headers, _ = request(client, base, "/api/v1/me")
        assert status == 401 and "no-store" in headers["Cache-Control"]
        status, headers, csrf = request(client, base, "/api/v1/csrf")
        cookie = headers["Set-Cookie"]
        assert status == 200 and "HttpOnly" in cookie and "SameSite=Lax" in cookie and "Secure" not in cookie
        assert request(client, base, "/api/v1/auth/logout", "POST")[0] == 403
        assert request(client, base, "/api/v1/auth/logout", "POST", {csrf["headerName"]: csrf["token"]})[0] == 204
        for route in ["/oauth2/authorization/google", "/login/oauth2/code/google?code=synthetic&state=synthetic"]:
            assert request(client, base, route)[0] == 404
            status, headers, body = request(client, base, route, "POST")
            assert status == 403 and "Location" not in headers and not isinstance(body, bytes)
        assert request(client, base, "/api/unknown")[0] == 403
        assert request(client, base, "/api/v1/status", headers={"Forwarded": "host=evil.invalid;proto=https", "X-Forwarded-Host": "evil.invalid"})[0] == 200
        assert sql(path, config, "select count(*) from flyway_schema_history where success") == "10"
        assert sql(path, config, "select (select count(*) from qb_question)+(select count(*) from pq_version)+(select count(*) from me_attempt)") == "0"
        for service in ["api", "postgres"]:
            container = compose(path, config, "ps", "-q", service).decode().strip()
            assert json.loads(docker("inspect", container, "--format", "{{json .HostConfig.PortBindings}}")) in ({}, None)
        web = compose(path, config, "ps", "-q", "web").decode().strip()
        assert json.loads(docker("inspect", web, "--format", "{{json .HostConfig.PortBindings}}"))["8080/tcp"] == [{"HostIp": "127.0.0.1", "HostPort": str(port)}]
        try:
            initialize(OUTPUT / "environments" / ("collision-" + token), images, port, "concurso-simulator-app-local-collision")
            raise AssertionError("Porta ocupada aceita")
        except OSError:
            assert request(client, base, "/api/v1/status")[0] == 200
        compose(path, config, "stop", "postgres")
        status, headers, body = request(client, base, "/api/v1/status", headers={"Cookie": "SESSION=synthetic-unknown"})
        assert status == 503 and body == {"status": "DOWN"} and "no-store" in headers["Cache-Control"]
        assert request(client, base, "/material/")[0] == 200
        compose(path, config, "up", "-d", "--wait", "postgres")
        deadline = time.monotonic() + 30
        while request(client, base, "/api/v1/status")[0] != 200:
            assert time.monotonic() < deadline
            time.sleep(.2)
        compose(path, config, "stop")
        start(path, config)
        assert sql(path, config, "select count(*) from flyway_schema_history where success") == "10"
        assert sql(path, config, "select count(*) from qb_question") == "0"
        write_report(OUTPUT / "runtime-check.json", {"status": "passed", "source_sha": metadata["source_sha"],
                     "checks": ["single-origin", "empty-schema-v10", "csrf-cookie", "oauth-methods", "no-host-api-db", "collision", "database-outage", "material-independent", "restart"],
                     "limits": "Synthetic empty database, no Google login; browser/account flows tested separately."})
        print("RUNTIME_CHECK_PASS: isolated Compose, security, empty V10, outage and restart")
    except subprocess.CalledProcessError as failure:
        (OUTPUT / "runtime-failure.log").write_bytes((failure.stdout or b"") + (failure.stderr or b""))
        raise
    finally:
        cleanup(path, config)


if __name__ == "__main__":
    check()
