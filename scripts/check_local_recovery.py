"""Real backup/logout/restore check; all accounts, captures and volumes are disposable."""
import json
import subprocess
import uuid
from urllib.request import build_opener
from local_app import initialize, load, start, compose, OUTPUT
from local_app_data import backup, restore_backup, verified_backup, confirmation, fingerprints, sql
from local_app_fixture import fixture
from check_local_runtime import request, cleanup
from local_app_images import write_report


def environment(images, port):
    token = uuid.uuid4().hex[:12]
    state = OUTPUT / "environments" / ("check-" + token)
    initialize(state, images, port, "concurso-simulator-app-local-check" + token)
    return load(state)


def account(base, cookie, route="/api/v1/me"):
    return request(build_opener(), base, route, headers={"Cookie": cookie})


def check():
    images = OUTPUT / "images.json"
    metadata = json.loads(images.read_text(encoding="utf-8"))
    port = int(metadata["origin"].rsplit(":", 1)[1])
    environments = []
    try:
        source, config = environment(images, port); environments.append((source, config))
        start(source, config)
        data = fixture(source, config)
        base = config["origin"]
        assert account(base, data["cookie"])[0] == 200
        routes = [f'/api/v1/attempts/{data[key]}/result' for key in ("legacyId", "finishedId")]
        routes += [f'/api/v1/attempts/{data["activeId"]}', "/api/v1/me/subject-review?roleId=assistente-administrativo", "/api/v1/me/attempts"]
        expected = {route: account(base, data["cookie"], route)[2] for route in routes}
        assert all(account(base, data["cookie"], route)[0] == 200 for route in routes)
        dump = backup(source, config)
        saved = verified_backup(dump)
        assert saved["sessions"] >= 3
        assert fingerprints(source, config) == saved["tables"]
        # Ordinary restart keeps a valid session. Logout after backup revokes its live cookie.
        start(source, config)
        assert account(base, data["cookie"])[0] == 200
        csrf = account(base, data["cookie"], "/api/v1/csrf")[2]
        assert request(build_opener(), base, "/api/v1/auth/logout", "POST", {"Cookie": data["cookie"], csrf["headerName"]: csrf["token"]})[0] == 204
        assert account(base, data["cookie"])[0] == 401
        compose(source, config, "stop")
        target, restored = environment(images, port); environments.append((target, restored))
        assert not restore_backup(target, restored, dump, None)
        assert not compose(target, restored, "ps", "-aq").strip()
        try:
            restore_backup(target, restored, dump, "yes")
            raise AssertionError("Wrong confirmation accepted")
        except ValueError:
            pass
        assert restore_backup(target, restored, dump, confirmation(saved, restored))
        assert fingerprints(target, restored) == saved["tables"]
        assert sql(target, restored, "select count(*) from spring_session").strip() == b"0"
        restored["generation_enabled"] = False
        (target / "state.json").write_text(json.dumps(restored), encoding="utf-8")
        start(target, restored)
        assert account(base, data["cookie"])[0] == 401
        new = fixture(target, restored, "session", data["userId"])
        assert account(base, new["cookie"])[0] == 200
        for route in routes:
            status, _, result = account(base, new["cookie"], route)
            assert status == 200 and result == expected[route], route
        assert sql(target, restored, "select string_agg(distinct coalesce(payload->>'format','1'),',' order by coalesce(payload->>'format','1')) from me_attempt").strip() == b"1,2"
        compose(target, restored, "stop")
        start(target, restored)
        assert account(base, new["cookie"])[0] == 200
        assert fingerprints(target, restored) == saved["tables"]
        compose(target, restored, "stop", "api")
        try:
            restore_backup(target, restored, dump, confirmation(saved, restored))
            raise AssertionError("Occupied destination accepted")
        except ValueError:
            assert fingerprints(target, restored) == saved["tables"]
        # Failed post-restore validation leaves a new destination blocked, never partially released.
        compose(target, restored, "stop")
        failed, failed_config = environment(images, port); environments.append((failed, failed_config))
        manifest = dump.with_suffix(".dump.json")
        original = manifest.read_bytes()
        tampered = dict(saved); tampered["sessions"] = saved["sessions"] + 1
        manifest.write_text(json.dumps(tampered), encoding="utf-8")
        try:
            try:
                restore_backup(failed, failed_config, dump, confirmation(tampered, failed_config))
                raise AssertionError("Invalid restored state accepted")
            except ValueError:
                assert (failed / "recovery.json").is_file()
                try:
                    start(failed, failed_config)
                    raise AssertionError("Incomplete recovery started")
                except ValueError:
                    pass
        finally:
            manifest.write_bytes(original)
        write_report(OUTPUT / "recovery-check.json", {"status": "passed", "source_sha": metadata["source_sha"],
                     "checks": ["consistent-custom-dump", "all-business-table-fingerprints", "restore-new-empty-only", "backup-logout-restore-old-cookie-401", "new-session-history-resume", "formats-1-2-v10", "compatible-image-restart-generation-disabled", "failed-recovery-blocked"],
                     "limits": "Synthetic accounts/sessions outside runtime; same compatible image, no schema downgrade or real operator data."})
        print("RECOVERY_CHECK_PASS: captures/history preserved, restored cookies revoked, compatible restart")
    except subprocess.CalledProcessError as failure:
        (OUTPUT / "recovery-failure.log").write_bytes((failure.stdout or b"") + (failure.stderr or b""))
        raise
    finally:
        for path, config in reversed(environments):
            cleanup(path, config)


if __name__ == "__main__":
    check()
