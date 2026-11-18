"""Regressões editoriais e de snapshot; não executa aprovação de acervo em CI."""
import copy
from fractions import Fraction
import hashlib
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from curate_questions import compile_curation, DIRECTORY, LEGACY_SOURCES, verify_manifest, write_outputs

ROOT = Path(__file__).resolve().parents[2]


class CurationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original, cls.items, cls.records, cls.batches, cls.manifest = compile_curation(ROOT)
        cls.by_key = {i["origin"]["key"]: i for i in cls.items}

    def test_selected_identities_batches_and_topic_coverage(self):
        self.assertEqual(len(self.by_key), 250)
        self.assertEqual(len(self.batches), 25)
        self.assertTrue(all(len(b["keys"]) == len(set(b["keys"])) == 10 for b in self.batches))
        self.assertEqual(len({i["content"]["classification"]["topicId"] for i in self.items}), 51)
        keys = [key for b in self.batches for key in b["keys"]]
        self.assertEqual(len(keys), len(set(keys)))
        for b in self.batches:
            self.assertEqual(len(b["package"]["inventory"]), 510)
            self.assertEqual(b["package"]["issues"], self.original["issues"])
        for group in self.original["possibleDuplicates"]:
            self.assertLessEqual(len(set(group) & set(keys)), 1)

    def test_review_snapshot_rejects_changed_content_or_rationale(self):
        verify_manifest(self.manifest, ROOT / DIRECTORY)
        for field in ("fingerprint", "reviewSha256"):
            changed = copy.deepcopy(self.manifest)
            changed["items"][0][field] = "0" * 64
            with self.assertRaisesRegex(ValueError, "Snapshot editorial mudou"):
                verify_manifest(changed, ROOT / DIRECTORY)

    def test_provenance_options_and_real_review_are_not_database_approval(self):
        original = {i["origin"]["key"]: i for i in self.original["items"]}
        for item, record in zip(sorted(self.items, key=lambda i: i["origin"]["key"]), sorted(self.records, key=lambda r: r["key"])):
            key = item["origin"]["key"]
            self.assertEqual(item["content"]["provenance"], original[key]["content"]["provenance"])
            self.assertEqual([o["id"] for o in item["content"]["alternatives"]],
                             [o["id"] for o in original[key]["content"]["alternatives"]])
            self.assertIsNone(item["expectedLatestRevision"])
            self.assertEqual(record["status"], "editorially-reviewed-not-imported")
            self.assertIsNone(record["databaseRevisionId"])
            self.assertEqual(record["fingerprint"], item["fingerprint"])
            self.assertIn("assistida", record["reviewedBy"])

    def test_independent_numeric_results_and_single_equivalent_answer(self):
        # Valores recalculados sem interpretar texto ou executar fórmulas do pacote.
        results = {
            "M01-Q01": 18-3*4, "M01-Q02": Fraction(2,3)+Fraction(1,6),
            "M01-Q03": (-4)*(-7), "M01-Q04": Fraction(25,10)+Fraction(75,100),
            "M01-Q05": Fraction(3,5)/Fraction(9,10), "M02-Q01": 12, "M02-Q02": 6,
            "M02-Q03": 30, "M03-Q01": 24/3*5, "M03-Q02": 6*10/12,
            "M04-Q01": Fraction(25,100)*240, "M04-Q02": 500*(1-Fraction(10,100)),
            "M04-Q03": 800*(1+Fraction(5,100)), "M05-Q01": (4+6+8)/3,
            "M05-Q02": (10+20)/2, "M05-Q03": 12*5, "M05-Q04": (5+8*2)/3,
            "M06-Q01": (14-4)/2, "M06-Q02": 35/5, "M06-Q03": 6*3,
            "M06-Q04": 15/3-2, "M07-Q01": 100-80, "M07-Q02": (100-80)/80*100,
            "M08-Q01": 2*100, "M08-Q02": 3*1000, "M08-Q03": 2*60+30,
            "M08-Q04": 100**2, "M09-Q01": 7*4, "M09-Q02": 2*(7+4),
            "M09-Q03": 5**2, "M09-Q04": 10*6/2, "M10-Q01": 12+3,
            "M10-Q02": 8*2, "M10-Q03": 28-2, "M12-Q01": 20+15-5,
            "M12-Q02": 40-30, "I06-Q01": 2+3+5, "I06-Q02": (4+6+8)/3,
            "I06-Q03": max(2,9,4), "I06-Q04": min(2,9,4), "I06-Q05": 2+3*4,
            "A08-Q01": 10*20*3, "A08-Q02": 5500-5000, "A08-Q03": (5500-5000)/5000*100,
        }
        def number(text):
            text = text.removeprefix("R$").strip().removesuffix("%").strip()
            for unit in ("cm²", "cm", "min", "g"):
                text = text.removesuffix(unit).strip()
            if "," in text:
                text = text.replace(".", "").replace(",", ".")
            elif text.count(".") == 1 and len(text.split(".")[-1]) == 3:
                text = text.replace(".", "")
            try:
                return Fraction(text.replace("−", "-"))
            except (ValueError, ZeroDivisionError):
                return None
        for key, result in results.items():
            with self.subTest(key=key):
                content = self.by_key[key]["content"]
                matches = [o["id"] for o in content["alternatives"] if number(o["content"]["text"]) == result]
                self.assertEqual(matches, [content["correctAlternativeId"]])
        self.assertEqual(Fraction(110,100)*Fraction(90,100), Fraction(99,100))
        for p in (False, True):
            for q in (False, True):
                self.assertEqual(not(p and q), (not p) or (not q))

    def test_no_previous_question_dependency_or_missing_legal_base(self):
        for item in self.items:
            content = item["content"]
            self.assertNotIn("caso anterior", content["statement"]["text"].lower())
            self.assertNotIn("mesmo retângulo", content["statement"]["text"].lower())
        for key, provision in {"S01-Q04":"art. 24, V, a", "S04-Q06":"art. 24, VI",
                               "S07-Q06":"art. 212, caput"}.items():
            self.assertIn(provision, [r["legalBasis"]["provision"] for r in self.by_key[key]["content"]["references"] if r["legalBasis"]])

    def test_generate_without_claiming_persisted_eligibility(self):
        with tempfile.TemporaryDirectory() as temp:
            coverage = write_outputs(ROOT, temp)
            self.assertIsNone(coverage["databaseEligible"])
            self.assertEqual(len(list((Path(temp)/"packages").glob("*.json"))), 25)
            self.assertNotIn("P05-Q01", self.by_key)
            self.assertIn("P05-Q01", coverage["notSelected"])


class FrozenSourcesTests(unittest.TestCase):
    """A curadoria 2026-10 lê as fontes congeladas, não as aulas vivas."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        shutil.copytree(ROOT / DIRECTORY, self.root / DIRECTORY)
        self.manifest = json.loads((ROOT / DIRECTORY / "manifest.json").read_text(encoding="utf-8"))
        # Aulas vivas divergentes do snapshot (como depois de lesson-depth-alignment).
        for source in self.manifest["sourceFiles"]:
            live = self.root / source["path"]
            live.parent.mkdir(parents=True, exist_ok=True)
            live.write_bytes((self.root / DIRECTORY / LEGACY_SOURCES / source["path"]).read_bytes() + b"\n<!-- aula viva editada -->\n")

    def tearDown(self):
        self.temp.cleanup()

    def test_frozen_copies_match_registered_source_hashes(self):
        for source in self.manifest["sourceFiles"]:
            data = (ROOT / DIRECTORY / LEGACY_SOURCES / source["path"]).read_bytes()
            self.assertEqual(hashlib.sha256(data).hexdigest(), source["sha256"], source["path"])

    def test_curation_reproduces_versioned_manifest_with_edited_live_lessons(self):
        original, items, _, batches, manifest = compile_curation(self.root)
        verify_manifest(manifest, self.root / DIRECTORY)
        self.assertEqual(manifest, self.manifest)
        self.assertEqual((len(items), len(batches)), (250, 25))
        for item in items:
            provenance = item["content"]["provenance"]
            self.assertFalse(provenance["contentPath"].startswith("editorial/"))
            self.assertFalse(provenance["answerPath"].startswith("editorial/"))

    def test_changed_frozen_copy_is_rejected(self):
        path = self.root / DIRECTORY / LEGACY_SOURCES / self.manifest["sourceFiles"][0]["path"]
        path.write_bytes(path.read_bytes() + b"\n")
        *_, manifest = compile_curation(self.root)
        with self.assertRaisesRegex(ValueError, "Snapshot editorial mudou: fontes congeladas"):
            verify_manifest(manifest, self.root / DIRECTORY)


if __name__ == "__main__":
    unittest.main()
