import tempfile
import hashlib
import json
import unittest
from contextlib import contextmanager
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
            metadata = {"version": 1, "kind": "local-app-backup", "schema_version": 13,
                        "source_project": "concurso-simulator-app-local-source", "source_owner": "a" * 32,
                        "source_sha": "b" * 40, "api_image": "sha256:" + "c" * 64,
                        "postgres_version": "18.6", "sessions": 1,
                        "tables": {t: {"rows": 0, "sha256": "d" * 64} for t in local_app_data.TABLES_BY_SCHEMA[13] - local_app_data.SESSIONS},
                        "sha256": hashlib.sha256(b"original").hexdigest()}
            Path(str(dump) + ".json").write_text(json.dumps(metadata))
            with patch.object(local_app_data, "owned"), patch.object(local_app_data, "compose") as command:
                with self.assertRaises(ValueError):
                    local_app_data.restore_backup(path, {"project": "target"}, dump, "yes")
                command.assert_not_called()

    def test_schema_backup_accepts_only_complete_supported_histories(self):
        for count in (10, 11, 12, 13, 14, 15, 16, 17):
            versions=("\n".join(str(i) for i in range(1,count+1))+"\n").encode()
            with patch.object(local_app_data, "sql", return_value=versions):
                self.assertEqual(count,local_app_data.schema_version(None,None))
        # Na épica combinada a V15 existe: histórico sem ela (ponte da #177 isolada) não é completo.
        for versions in (b"1\n11\n",b"1\n2\n",("\n".join(str(i) for i in range(1,19))).encode(),
                         ("\n".join(str(i) for i in [*range(1,15),16])).encode(),
                         ("\n".join(str(i) for i in [*range(1,15),16,17])).encode()):
            with patch.object(local_app_data,"sql",return_value=versions):
                with self.assertRaises(ValueError):local_app_data.schema_version(None,None)

    def test_current_backup_manifest_requires_all_evidence_tables_and_matching_checksum(self):
        directory = local_app.OUTPUT / "environments"
        directory.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="backup-", dir=directory) as name:
            path = Path(name).resolve()
            self.assertTrue(path.is_relative_to(directory.resolve()))
            backups = path / "backups"; backups.mkdir()
            dump = backups / "synthetic.dump"; dump.write_bytes(b"synthetic-current-backup")
            metadata = {"version": 1, "kind": "local-app-backup", "schema_version": 13,
                        "source_project": "concurso-simulator-app-local-source", "source_owner": "a" * 32,
                        "source_sha": "b" * 40, "api_image": "sha256:" + "c" * 64,
                        "postgres_version": "18.6", "sessions": 1,
                        "tables": {t: {"rows": 0, "sha256": "d" * 64} for t in local_app_data.TABLES_BY_SCHEMA[13] - local_app_data.SESSIONS},
                        "sha256": hashlib.sha256(dump.read_bytes()).hexdigest()}
            manifest = Path(str(dump) + ".json")
            manifest.write_text(json.dumps(metadata))
            self.assertEqual(13, local_app_data.verified_backup(dump)["schema_version"])
            for table in ("qb_import_receipt", "qb_import_blob", "qb_import_file", "qb_import_revision", "admin_registration"):
                missing = dict(metadata); missing["tables"] = {t: v for t, v in metadata["tables"].items() if t != table}
                manifest.write_text(json.dumps(missing))
                with self.assertRaises(ValueError): local_app_data.verified_backup(dump)
            manifest.write_text(json.dumps(metadata))
            dump.write_bytes(b"tampered")
            with self.assertRaises(ValueError): local_app_data.verified_backup(dump)

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


# Closed inventories captured from the supported schema contracts, not the production map.
ORIGINAL_TABLES = {"application_marker", "app_user", "external_identity", "spring_session", "spring_session_attributes",
                   "qb_discipline", "qb_role", "qb_topic", "qb_question", "qb_revision", "qb_alternative", "qb_revision_role",
                   "qb_reference", "qb_decision", "me_attempt", "me_item", "me_command", "pq_family", "pq_version",
                   "pq_decision", "pq_instance", "flyway_schema_history"}
IMPORT_EVIDENCE = {"qb_import_receipt", "qb_import_blob", "qb_import_file", "qb_import_revision"}
EXPECTED = {10: ORIGINAL_TABLES, 11: ORIGINAL_TABLES, 12: ORIGINAL_TABLES | IMPORT_EVIDENCE,
            13: ORIGINAL_TABLES | IMPORT_EVIDENCE | {"admin_registration"},
            14: ORIGINAL_TABLES | IMPORT_EVIDENCE | {"admin_registration"},
            15: ORIGINAL_TABLES | IMPORT_EVIDENCE | {"admin_registration"},
            16: ORIGINAL_TABLES | IMPORT_EVIDENCE | {"admin_registration"},
            17: ORIGINAL_TABLES | IMPORT_EVIDENCE | {"admin_registration", "qb_decision_batch", "qb_decision_batch_item"}}


class HistoricalBackupSchemas(unittest.TestCase):
    @contextmanager
    def manifest(self, version):
        parent = local_app.OUTPUT / "environments"
        parent.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="schema-backup-", dir=parent) as folder:
            path = Path(folder).resolve()
            self.assertTrue(path.is_relative_to(parent.resolve()))
            backups = path / "backups"; backups.mkdir()
            dump = backups / "synthetic.dump"; dump.write_bytes(b"synthetic-dump")
            metadata = {"version": 1, "kind": "local-app-backup", "schema_version": version,
                        "source_project": "concurso-simulator-app-local-source", "source_owner": "a" * 32,
                        "source_sha": "b" * 40, "api_image": "sha256:" + "c" * 64,
                        "postgres_version": "18.6", "sessions": 2,
                        "tables": {t: {"rows": 0, "sha256": hashlib.sha256(b"").hexdigest()}
                                   for t in EXPECTED[version] - local_app_data.SESSIONS},
                        "sha256": hashlib.sha256(dump.read_bytes()).hexdigest()}
            self.write_manifest(dump, metadata)
            yield path, dump, metadata

    def write_manifest(self, dump, metadata):
        Path(str(dump) + ".json").write_text(json.dumps(metadata), encoding="utf-8")

    def test_original_manifests_and_read_only_restore_preview_accept_each_schema(self):
        for version in EXPECTED:
            with self.subTest(schema=version), self.manifest(version) as (path, dump, metadata):
                self.assertEqual(metadata, local_app_data.verified_backup(dump))
                with patch.object(local_app_data, "owned"), patch.object(local_app_data, "compose") as compose, patch("builtins.print"):
                    self.assertFalse(local_app_data.restore_backup(path, {"project": "new"}, dump, None))
                    compose.assert_not_called()

    def test_each_schema_rejects_missing_extra_and_future_tables(self):
        for version in EXPECTED:
            with self.manifest(version) as (_, dump, metadata):
                for table in metadata["tables"]:
                    with self.subTest(schema=version, missing=table):
                        bad = dict(metadata); bad["tables"] = {k: v for k, v in metadata["tables"].items() if k != table}
                        self.write_manifest(dump, bad)
                        with self.assertRaises(ValueError): local_app_data.verified_backup(dump)
                for table in (EXPECTED[13] - EXPECTED[version]) | {"unexpected_business_table"}:
                    with self.subTest(schema=version, extra=table):
                        bad = dict(metadata); bad["tables"] = dict(metadata["tables"], **{table: {"rows": 0, "sha256": "a" * 64}})
                        self.write_manifest(dump, bad)
                        with self.assertRaises(ValueError): local_app_data.verified_backup(dump)

    def test_declared_schema_type_and_checksum_remain_strict(self):
        with self.manifest(11) as (_, dump, metadata):
            for version in (9, 15, "11", 11.0, True, None):
                with self.subTest(schema=version):
                    self.write_manifest(dump, dict(metadata, schema_version=version))
                    with self.assertRaises(ValueError): local_app_data.verified_backup(dump)
            self.write_manifest(dump, metadata); dump.write_bytes(b"tampered")
            with self.assertRaises(ValueError): local_app_data.verified_backup(dump)

    def test_fingerprints_read_only_the_complete_inventory_of_the_actual_schema(self):
        for version, expected in EXPECTED.items():
            reads = []
            def sql(path, config, statement):
                if "select version from" in statement: return ("\n".join(map(str, range(1, version + 1))) + "\n").encode()
                if "pg_tables" in statement: return "\n".join(sorted(expected)).encode()
                table = statement.split(" from ")[-1].split()[0]; reads.append(table)
                return b'{"synthetic":true}\n'
            with self.subTest(schema=version), patch.object(local_app_data, "sql", side_effect=sql):
                result = local_app_data.fingerprints(None, None)
                self.assertEqual(expected - local_app_data.SESSIONS, set(result))
                self.assertEqual(sorted(result), reads)
                self.assertTrue(all(v["rows"] == 1 for v in result.values()))
            for actual in (expected - {"qb_revision"}, expected | {"unexpected_business_table"}):
                with self.subTest(schema=version, inventory=actual), patch.object(local_app_data, "schema_version", return_value=version), patch.object(local_app_data, "tables", return_value=actual), patch.object(local_app_data, "sql") as rows:
                    with self.assertRaises(ValueError): local_app_data.fingerprints(None, None)
                    rows.assert_not_called()

    def test_wrong_image_is_rejected_before_starting_or_writing_database(self):
        with self.manifest(11) as (path, dump, metadata):
            config = {"project": "concurso-simulator-app-local-target", "owner": "f" * 32,
                      "images": {"images": {"api": {"id": "sha256:" + "e" * 64}}}}
            with patch.object(local_app_data, "owned"), patch.object(local_app_data, "compose") as compose:
                with self.assertRaisesRegex(ValueError, "mesma imagem"):
                    local_app_data.restore_backup(path, config, dump, local_app_data.confirmation(metadata, config))
                compose.assert_not_called()


if __name__ == "__main__":
    unittest.main()
