import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import vps_production as production
import vps_production_data as data
from local_app_data import SESSIONS, TABLES_BY_SCHEMA

OWNER = "a" * 32
OTHER = "b" * 32
API = "sha256:" + "1" * 64


def config(owner=OWNER, project=production.PROJECT, port=production.PORT, api=API, rehearsal=False):
    return {"version": 1, "project": project, "port": port, "owner": owner, "origin": production.ORIGIN,
            "rehearsal": rehearsal, "generation_enabled": True,
            "images": {"source_sha": "c" * 40, "on_main": True, "origin": production.ORIGIN,
                       "images": {"api": {"id": api}, "web": {"id": "sha256:" + "2" * 64}},
                       "base_images": {"postgres": "postgres@sha256:" + "3" * 64}}}


class TargetTests(unittest.TestCase):
    def test_production_is_fixed_and_recovery_is_separate(self):
        self.assertEqual(production.target(), (production.PROJECT, 18090))
        self.assertEqual(production.target("ensaio", 18091), ("concurso-simulator-prod-recovery-ensaio", 18091))
        for recovery, port in ((None, 18091), ("ensaio", 18090), ("ensaio", None), ("Ensaio!", 18091), ("x" * 17, 18091)):
            with self.subTest(recovery=recovery, port=port):
                with self.assertRaises(ValueError):
                    production.target(recovery, port)

    def test_valid_target_rejects_mixed_project_and_port(self):
        self.assertTrue(production.valid_target(config()))
        self.assertTrue(production.valid_target(config(project="concurso-simulator-prod-recovery-a", port=18095)))
        self.assertFalse(production.valid_target(config(port=18091)))
        self.assertFalse(production.valid_target(config(project="concurso-simulator-prod-recovery-a", port=18090)))
        self.assertFalse(production.valid_target(config(project="concurso-simulator-app-local", port=18091)))


class BackupFileTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.backups = Path(self.directory.name).resolve() / "backups"
        self.backups.mkdir()

    def write(self, content=b"DUMP", **changes):
        dump = self.backups / "x.dump"
        dump.write_bytes(content)
        tables = {t: {"rows": 0, "sha256": "0" * 64} for t in TABLES_BY_SCHEMA[17] - SESSIONS}
        metadata = {"version": 1, "kind": data.KIND, "source_project": production.PROJECT, "source_owner": OWNER,
                    "source_sha": "c" * 40, "api_image": API, "rehearsal": False, "postgres_version": "18.6",
                    "schema_version": 17, "sessions": 0, "tables": tables, "sha256": hashlib.sha256(b"DUMP").hexdigest()}
        metadata.update(changes)
        Path(str(dump) + ".json").write_text(json.dumps(metadata), encoding="utf-8")
        return dump

    def test_valid_backup_is_accepted(self):
        self.assertEqual(data.verified_backup(self.write(), self.backups)["schema_version"], 17)

    def test_tampered_or_foreign_backups_are_refused(self):
        cases = [dict(content=b"CHANGED"), dict(kind="local-app-backup"), dict(source_project="concurso-simulator-app-local"),
                 dict(schema_version=99), dict(tables={}), dict(sessions=-1)]
        for case in cases:
            with self.subTest(case=case):
                with self.assertRaises(ValueError):
                    data.verified_backup(self.write(**case), self.backups)

    def test_backup_outside_backups_directory_is_refused(self):
        dump = self.write()
        with self.assertRaises(ValueError):
            data.verified_backup(dump, self.backups.parent)


class RestoreTests(BackupFileTests):
    def test_preview_never_writes(self):
        with mock.patch.object(production, "compose") as compose, mock.patch("builtins.print"):
            self.assertFalse(data.restore(Path(self.directory.name), config(owner=OTHER), self.write(), None, self.backups))
        compose.assert_not_called()

    def test_restore_over_the_source_state_or_other_image_is_refused(self):
        dump = self.write()
        same = config(owner=OWNER)
        with self.assertRaises(ValueError):
            data.restore(Path(self.directory.name), same, dump, data.confirmation(data.verified_backup(dump, self.backups), same), self.backups)
        other_image = config(owner=OTHER, api="sha256:" + "9" * 64)
        with self.assertRaises(ValueError):
            data.restore(Path(self.directory.name), other_image, dump,
                         data.confirmation(data.verified_backup(dump, self.backups), other_image), self.backups)

    def test_wrong_confirmation_is_refused(self):
        with self.assertRaises(ValueError):
            data.restore(Path(self.directory.name), config(owner=OTHER), self.write(), "RESTORE qualquer", self.backups)


class BackupFailureTests(unittest.TestCase):
    def test_failed_dump_leaves_no_partial_file(self):
        with tempfile.TemporaryDirectory() as directory:
            backups = Path(directory) / "backups"
            failure = data.subprocess.CalledProcessError(1, "pg_dump")
            with mock.patch.object(production, "compose", return_value=b""), \
                    mock.patch.object(data, "fingerprints", return_value={}), \
                    mock.patch.object(data, "schema_version", return_value=17), \
                    mock.patch.object(data, "sql", side_effect=[b"18.6", b"0"]), \
                    mock.patch.object(data, "postgres", return_value="abc123"), \
                    mock.patch.object(data, "docker_run", side_effect=failure):
                with self.assertRaises(data.subprocess.CalledProcessError):
                    data.backup(Path(directory), config(), directory=backups)
            self.assertEqual(list(backups.iterdir()), [])


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.state = Path(self.directory.name)
        self.calls = []
        patches = [mock.patch.object(production, "compose", side_effect=lambda s, c, *a: self.calls.append(("compose",) + a) or b""),
                   mock.patch.object(production, "validate_images"),
                   mock.patch.object(data, "backup", side_effect=self.fake_backup),
                   mock.patch("builtins.print")]
        for patch in patches:
            patch.start()
            self.addCleanup(patch.stop)

    def fake_backup(self, state, config, restart=True, directory=None):
        self.calls.append(("backup", restart))
        return Path("/b/x.dump"), {"schema_version": 17}

    def pin(self, images):
        compose = self.state / "release.compose.yaml"
        compose.write_bytes(b"services: {}")
        return images | {"compose": {"file": str(compose), "sha256": hashlib.sha256(b"services: {}").hexdigest()}}

    def manifest(self, api="sha256:" + "4" * 64, on_main=True, pinned=True):
        value = config()["images"] | {"on_main": on_main}
        value["images"] = {"api": {"id": api}, "web": {"id": "sha256:" + "5" * 64}}
        if pinned:
            value = self.pin(value)
        path = self.state / "m.json"
        path.write_text(json.dumps(value), encoding="utf-8")
        return path

    def test_release_without_pinned_compose_is_refused_before_any_change(self):
        current = config()
        with self.assertRaises(ValueError):
            data.release(self.state, current, self.manifest(pinned=False))
        self.assertEqual(self.calls, [])
        self.assertFalse((self.state / "state.json").exists())

    def test_rollback_refuses_previous_release_without_valid_compose_before_stopping(self):
        current = config()
        current["previous"] = {"images": config(api="sha256:" + "7" * 64)["images"], "backup": "/b/x.dump", "schema_version": 17}
        with self.assertRaises(ValueError):
            data.rollback(self.state, current)
        self.assertEqual(self.calls, [])

    def test_promote_requires_a_main_release_with_pinned_compose(self):
        rehearsal = config(rehearsal=True)
        with mock.patch.object(production, "on_main", return_value=True):
            with self.assertRaises(ValueError):
                data.promote(self.state, rehearsal)  # current release has no pinned compose
            rehearsal["images"] = self.pin(rehearsal["images"])
            rehearsal["previous"] = {"images": config()["images"] | {"on_main": False}, "backup": "/b", "schema_version": 17}
            data.promote(self.state, rehearsal)
        saved = json.loads((self.state / "state.json").read_text())
        self.assertFalse(saved["rehearsal"])
        self.assertIsNone(saved["previous"])  # rehearsal releases are not production rollback targets
        with mock.patch.object(production, "on_main", return_value=False):
            other = config(rehearsal=True)
            other["images"] = self.pin(other["images"])
            with self.assertRaises(ValueError):
                data.promote(self.state, other)

    def test_release_backs_up_before_switching_and_keeps_previous(self):
        current = config()
        data.release(self.state, current, self.manifest())
        self.assertEqual(self.calls[0], ("backup", False))
        self.assertEqual(self.calls[1][1], "up")
        saved = json.loads((self.state / "state.json").read_text())
        self.assertEqual(saved["previous"]["images"]["images"]["api"]["id"], API)
        self.assertEqual(saved["previous"]["schema_version"], 17)

    def test_release_cannot_change_the_postgres_image(self):
        path = self.manifest()
        value = json.loads(path.read_text())
        value["base_images"] = {"postgres": "postgres@sha256:" + "8" * 64}
        path.write_text(json.dumps(value), encoding="utf-8")
        with self.assertRaises(ValueError):
            data.release(self.state, config(), path)
        self.assertEqual(self.calls, [])

    def test_release_outside_main_is_refused_in_production(self):
        with self.assertRaises(ValueError):
            data.release(self.state, config(), self.manifest(on_main=False))
        self.assertEqual(self.calls, [])

    def test_rollback_requires_unchanged_schema(self):
        current = config()
        current["previous"] = {"images": self.pin(config(api="sha256:" + "7" * 64)["images"]), "backup": "/b/x.dump", "schema_version": 16}
        with mock.patch.object(data, "schema_version", return_value=17):
            with self.assertRaises(ValueError):
                data.rollback(self.state, current)
        self.assertEqual(current["images"]["images"]["api"]["id"], API)
        with mock.patch.object(data, "schema_version", return_value=16):
            data.rollback(self.state, current)
        self.assertEqual(current["images"]["images"]["api"]["id"], "sha256:" + "7" * 64)


if __name__ == "__main__":
    unittest.main()
