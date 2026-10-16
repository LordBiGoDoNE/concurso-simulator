"""Controles negativos com JAR/SPA sintéticos; nenhum dado real."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location("attempt_artifacts", Path(__file__).resolve().parents[1] / "check_attempt_artifacts.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ArtifactTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.jar = self.root / "app.jar"
        with zipfile.ZipFile(self.jar, "w") as archive:
            archive.writestr("BOOT-INF/classes/br/com/concursosimulator/App.class", b"production-code")
        self.dist = self.root / "dist"
        (self.dist / "assets").mkdir(parents=True)
        (self.dist / "index.html").write_text('<script type="module" src="/assets/entry.js"></script>', encoding="utf-8")
        (self.dist / "assets/entry.js").write_text("public-entry", encoding="utf-8")
        (self.dist / "assets/AttemptResult-lazy.js").write_text("correctAlternativeId", encoding="utf-8")

    def test_generic_late_result_code_is_allowed_without_editorial_payload(self):
        self.assertEqual(module.check(self.jar, self.dist)["status"], "passed")

    def test_fixture_class_and_env_cannot_be_packaged_in_boot_classes(self):
        for name in ("br/com/concursosimulator/mockexam/MockExamOperationalFixture.class", ".env", "curation/database-private.json"):
            with self.subTest(name=name):
                with zipfile.ZipFile(self.jar, "w") as archive:
                    archive.writestr("BOOT-INF/classes/br/com/concursosimulator/App.class", b"production-code")
                    archive.writestr("BOOT-INF/classes/" + name, b"fixture")
                with self.assertRaises(ValueError):
                    module.check(self.jar, self.dist)

    def test_private_payload_in_any_chunk_and_initial_corrective_contract_are_rejected(self):
        late = self.dist / "assets/AttemptResult-lazy.js"
        late.write_text("PRIVATE_BROWSER_RESOLUTION_SENTINEL", encoding="utf-8")
        with self.assertRaises(ValueError):
            module.check(self.jar, self.dist)
        late.write_text("generic-result", encoding="utf-8")
        (self.dist / "assets/entry.js").write_text("correctAlternativeId", encoding="utf-8")
        with self.assertRaises(ValueError):
            module.check(self.jar, self.dist)

    def test_entry_path_cannot_escape_artifact(self):
        (self.dist / "index.html").write_text('<script type="module" src="../outside.js"></script>', encoding="utf-8")
        with self.assertRaises(ValueError):
            module.check(self.jar, self.dist)

    def test_numeric_generator_metadata_and_fixture_cannot_enter_any_frontend_chunk(self):
        late = self.dist / "assets/AttemptResult-lazy.js"
        for marker in (*module.NUMERIC_PRIVATE, b"PRIVATE_PARAMETERIZED_FIXTURE"):
            with self.subTest(marker=marker):
                late.write_bytes(marker)
                with self.assertRaises(ValueError):
                    module.check(self.jar, self.dist)
