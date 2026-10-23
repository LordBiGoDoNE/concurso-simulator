import tempfile
import hashlib
import json
import unittest
from pathlib import Path
from unittest.mock import patch
import local_app
import local_app_data


class RecoveryGuards(unittest.TestCase):
    def test_corrupt_dump_rejected_before_database_operation(self):
        directory = local_app.OUTPUT / "environments"
        directory.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="backup-", dir=directory) as name:
            path = Path(name).resolve()
            self.assertTrue(path.is_relative_to(directory.resolve()))
            backups = path / "backups"; backups.mkdir()
            dump = backups / "synthetic.dump"; dump.write_bytes(b"changed")
            metadata = {"version": 1, "kind": "local-app-backup", "schema_version": 10,
                        "source_project": "concurso-simulator-app-local-source", "source_owner": "a" * 32,
                        "source_sha": "b" * 40, "api_image": "sha256:" + "c" * 64,
                        "postgres_version": "18.6", "sessions": 1,
                        "tables": {t: {"rows": 0, "sha256": "d" * 64} for t in local_app_data.TABLES - local_app_data.SESSIONS},
                        "sha256": hashlib.sha256(b"original").hexdigest()}
            Path(str(dump) + ".json").write_text(json.dumps(metadata))
            with patch.object(local_app_data, "owned"), patch.object(local_app_data, "compose") as command:
                with self.assertRaises(ValueError):
                    local_app_data.restore_backup(path, {"project": "target"}, dump, "yes")
                command.assert_not_called()

    def test_operation_lock_serializes_mutating_commands(self):
        directory = local_app.OUTPUT / "environments"
        directory.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="lock-", dir=directory) as name:
            path = Path(name).resolve()
            self.assertTrue(path.is_relative_to(directory.resolve()))
            with local_app.operation(path):
                with self.assertRaises(FileExistsError):
                    with local_app.operation(path):
                        self.fail("Second writer entered")
            self.assertFalse((path / "operation.lock").exists())

    def test_wrong_confirmation_never_starts_database(self):
        metadata = {"source_project": "concurso-simulator-app-local-source", "source_owner": "a" * 32,
                    "sha256": "b" * 64, "api_image": "sha256:" + "c" * 64}
        config = {"project": "concurso-simulator-app-local-target", "owner": "d" * 32}
        with patch.object(local_app_data, "owned"), patch.object(local_app_data, "verified_backup", return_value=metadata), patch.object(local_app_data, "compose") as command:
            with self.assertRaises(ValueError):
                local_app_data.restore_backup(Path("unused"), config, Path("unused"), "yes")
            command.assert_not_called()

    def test_restore_preview_is_read_only(self):
        metadata = {"source_project": "source", "sha256": "a" * 64}
        with patch.object(local_app_data, "owned"), patch.object(local_app_data, "verified_backup", return_value=metadata), patch.object(local_app_data, "compose") as command, patch("builtins.print"):
            self.assertFalse(local_app_data.restore_backup(Path("unused"), {"project": "target"}, Path("unused"), None))
            command.assert_not_called()


if __name__ == "__main__":
    unittest.main()
