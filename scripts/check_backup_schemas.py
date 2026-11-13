"""Exercise real historical dumps/restores in two owned, networkless PostgreSQL containers."""
import json
import re
import subprocess
import tempfile
import time
import uuid
from pathlib import Path
from unittest.mock import patch
import local_app_data as recovery
from local_app import docker, start
from local_app_images import ROOT, OUTPUT, write_report

LABEL = "com.concursosimulator.backup-schema-check"


def owned(container, token):
    assert re.fullmatch(r"[0-9a-f]{64}", container)
    labels = json.loads(docker("inspect", "--format", "{{json .Config.Labels}}", container))
    assert labels.get(LABEL) == token


def sql(container, statement):
    return docker("exec", "-i", container, "psql", "-X", "-U", "concurso", "-d", "concurso_simulator",
                  "-qAt", "-v", "ON_ERROR_STOP=1", data=statement.encode())


def reset(container, token):
    owned(container, token)
    sql(container, "drop schema public cascade;create schema public")


def migrate(container, version):
    sql(container, "create table flyway_schema_history(installed_rank int primary key, version varchar(50), "
                   "description varchar(200), type varchar(20), script varchar(1000), checksum int, "
                   "installed_by varchar(100), installed_on timestamptz default now(), execution_time int, success boolean)")
    files = sorted((ROOT / "backend/src/main/resources/db/migration").glob("V*__*.sql"),
                   key=lambda p: int(re.match(r"V(\d+)__", p.name)[1]))
    for file in files:
        number = int(re.match(r"V(\d+)__", file.name)[1])
        if number <= version:
            sql(container, file.read_text(encoding="utf-8"))
            sql(container, f"insert into flyway_schema_history(installed_rank,version,success) values({number},'{number}',true)")
    actor, attempt, session = (str(uuid.uuid4()) for _ in range(3))
    sql(container, f"insert into app_user(id) values('{actor}');"
                   f"insert into external_identity values('{actor}','google','https://accounts.google.com','synthetic-subject');"
                   f"insert into me_attempt(id,owner_kind,owner_id,account_id,created_at,payload) "
                   f"values('{attempt}','ACCOUNT','{actor}','{actor}',now(),'{{\"version\":0}}');"
                   f"insert into spring_session values('{session}','{session}',1,1,1800,1800001,'synthetic');"
                   f"insert into spring_session_attributes values('{session}','synthetic',decode('01','hex'))")
    if version >= 12:
        sql(container, f"insert into qb_import_receipt values('{uuid.uuid4()}','synthetic','{actor}',now(),'{{}}')")
    if version >= 13:
        sql(container, f"update admin_registration set owner_id='{actor}',username='synthetic',password_hash='synthetic-noncredential'")


def check():
    token = uuid.uuid4().hex
    containers = []
    versions = []
    directory = OUTPUT / "environments"
    directory.mkdir(parents=True, exist_ok=True)
    try:
        for _ in range(2):
            container = docker("run", "-d", "--rm", "--network", "none", "--label", f"{LABEL}={token}",
                               "-e", "POSTGRES_USER=concurso", "-e", "POSTGRES_DB=concurso_simulator",
                               "-e", "POSTGRES_HOST_AUTH_METHOD=trust", "postgres:18.6").decode().strip()
            assert re.fullmatch(r"[0-9a-f]{64}", container)
            containers.append(container)
            for attempt in range(100):
                try:
                    # Ignore the temporary init server; wait for the final PID 1 process.
                    if docker("exec", container, "cat", "/proc/1/comm").strip() == b"postgres":
                        sql(container, "select 1")
                        break
                except subprocess.CalledProcessError:
                    pass
                if attempt == 99:
                    raise ValueError("PostgreSQL sintético não ficou pronto")
                time.sleep(0.2)
        source_container, target_container = containers
        for version in (10, 11, 12, 13, 14, 15):
            reset(source_container, token)
            reset(target_container, token)
            migrate(source_container, version)
            with tempfile.TemporaryDirectory(prefix="backup-schema-", dir=directory) as folder:
                parent = Path(folder).resolve()
                assert parent.is_relative_to(directory.resolve())
                source, target = parent / "source", parent / "target"
                source.mkdir(); target.mkdir()
                image = {"source_sha": "b" * 40, "images": {"api": {"id": "sha256:" + "c" * 64}}}
                origin = {"project": "concurso-simulator-app-local-checksource" + token[:8], "owner": "a" * 32, "images": image}
                destination = {"project": "concurso-simulator-app-local-checktarget" + token[:8], "owner": "d" * 32, "images": image}
                # Only lifecycle wiring is substituted; SQL, pg_dump/restore and validations are real.
                def postgres(path, config):
                    return source_container if path == source else target_container
                with patch.object(recovery, "owned"), patch.object(recovery, "compose", return_value=b""), patch.object(recovery, "postgres", side_effect=postgres):
                    before = recovery.fingerprints(source, origin)
                    dump = recovery.backup(source, origin)
                    metadata = recovery.verified_backup(dump)
                    assert metadata["schema_version"] == version and metadata["sessions"] == 1
                    assert recovery.restore_backup(target, destination, dump, None) is False
                    assert recovery.tables(target, destination) == set()
                    assert recovery.restore_backup(target, destination, dump, recovery.confirmation(metadata, destination))
                    assert recovery.fingerprints(target, destination) == before
                    assert recovery.fingerprints(source, origin) == before
                    assert recovery.sql(target, destination, "select count(*) from spring_session").strip() == b"0"
                    try:
                        recovery.restore_backup(target, destination, dump, recovery.confirmation(metadata, destination))
                        raise AssertionError("Occupied destination accepted")
                    except ValueError:
                        assert recovery.fingerprints(target, destination) == before
                    if version == 10:
                        reset(target_container, token)
                        wrong = dict(metadata, schema_version=11)  # Same tables, different history.
                        Path(str(dump) + ".json").write_text(json.dumps(wrong), encoding="utf-8")
                        try:
                            recovery.restore_backup(target, destination, dump, recovery.confirmation(wrong, destination))
                            raise AssertionError("Divergent restored schema accepted")
                        except ValueError:
                            assert (target / "recovery.json").is_file()
                            try:
                                start(target, destination)
                                raise AssertionError("Incomplete recovery released")
                            except ValueError:
                                pass
                versions.append(version)
    finally:
        for container in containers:
            owned(container, token)
            docker("rm", "-f", "-v", container)
    sha = subprocess.check_output(["git", "-C", str(ROOT), "rev-parse", "HEAD"], text=True).strip()
    write_report(OUTPUT / "backup-schema-check.json", {"status": "passed", "source_sha": sha, "schemas": versions,
                 "checks": ["real-migrations", "custom-dump-restore", "business-fingerprints-preserved", "source-unchanged",
                            "read-only-preview", "restored-sessions-cleared", "occupied-destination-rejected", "schema-mismatch-blocked"],
                 "limits": "Synthetic PostgreSQL 18.6 without network/host ports. Lifecycle wiring substituted; no historical API image startup or operator data."})
    print("BACKUP_SCHEMAS_PASS: V10/V11/V12/V13/V14/V15 dumps and restores validated")


if __name__ == "__main__":
    check()
