"""Controles negativos do verificador somente leitura; relatórios são fixtures, sem aprovações reais."""
import copy
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import uuid

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import verify_curated_bank as verifier
from curate_questions import compile_curation

ROOT = Path(__file__).resolve().parents[2]


class CuratedBankVerificationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.compiled = compile_curation(ROOT)
        cls.entries = []
        for item in cls.compiled[1]:
            key = item["origin"]["key"]
            revision = str(uuid.uuid5(uuid.NAMESPACE_URL, "synthetic-read-verifier-revision:" + key))
            cls.entries.append({"questionId": str(uuid.uuid5(uuid.NAMESPACE_URL, "synthetic-read-verifier-question:" + key)),
                                "origin": item["origin"], "eligibleRevisionId": revision, "editorialSequence": 2,
                                "revisions": [{"id": revision, "fingerprint": item["fingerprint"], "status": "APPROVED"}]})

    def report(self, directory, entries):
        file = Path(directory) / "synthetic-report-not-editorial-evidence.json"
        file.write_text(json.dumps({"fixture": True, "questions": entries}), encoding="utf-8")
        return file

    def test_rejects_external_or_credentialed_target_before_reading_report(self):
        for api in ("https://remote.test", "http://user:secret@127.0.0.1:8080", "http://127.0.0.1:8080/private"):
            with self.subTest(api=api), patch.object(verifier, "urlopen", side_effect=AssertionError("No HTTP permitted")):
                with self.assertRaisesRegex(ValueError, "origem HTTP local"):
                    verifier.verify(ROOT, "missing-report.json", api)

    def test_stale_unapproved_or_duplicate_revision_evidence_fails_before_http(self):
        for mutation in ("status", "fingerprint", "pointer", "duplicate"):
            entries = copy.deepcopy(self.entries)
            if mutation == "status":
                entries[0]["revisions"][0]["status"] = "DRAFT"
            elif mutation == "fingerprint":
                entries[0]["revisions"][0]["fingerprint"] = "0" * 64
            elif mutation == "pointer":
                entries[0]["eligibleRevisionId"] = None
            else:
                entries[1]["questionId"] = entries[0]["questionId"]
            with self.subTest(mutation=mutation), tempfile.TemporaryDirectory() as directory:
                with patch.object(verifier, "compile_curation", return_value=self.compiled), patch.object(verifier, "urlopen", side_effect=AssertionError("No HTTP permitted")):
                    with self.assertRaisesRegex(ValueError, "revisão/base/estado"):
                        verifier.verify(ROOT, self.report(directory, entries), "http://127.0.0.1:1")

    def test_added_correct_answer_in_http_detail_is_a_detected_negative_control(self):
        items = {i["origin"]["key"]: i for i in self.compiled[1]}
        summaries = []
        for q in sorted(self.entries, key=lambda q: q["questionId"]):
            content = items[q["origin"]["key"]]["content"]
            classification = {**content["classification"], "roleIds": sorted(content["classification"]["roleIds"])}
            summaries.append({"questionId": q["questionId"], "revisionId": q["eligibleRevisionId"],
                              "classification": classification, "statementStart": content["statement"]["text"][:200]})
        first = next(q for q in self.entries if q["questionId"] == summaries[0]["questionId"])
        content = items[first["origin"]["key"]]["content"]
        detail = {"questionId": first["questionId"], "revisionId": first["eligibleRevisionId"],
                  "classification": summaries[0]["classification"], "statement": content["statement"],
                  "alternatives": content["alternatives"], "references": [],
                  "correctAlternativeId": content["correctAlternativeId"]}
        responses = [{"disciplines": [None] * 5, "topics": [None] * 51, "roles": [None] * 2}]
        responses += [{"items": summaries[n:n + 100], "page": n // 100, "size": 100, "total": 250} for n in (0, 100, 200)]
        responses.append(detail)

        class Response(io.BytesIO):
            status = 200

        def fake_read(*args, **kwargs):
            return Response(json.dumps(responses.pop(0)).encode("utf-8"))

        with tempfile.TemporaryDirectory() as directory:
            with patch.object(verifier, "compile_curation", return_value=self.compiled), patch.object(verifier, "urlopen", side_effect=fake_read):
                with self.assertRaisesRegex(ValueError, "Detalhe com campo extra/privado"):
                    verifier.verify(ROOT, self.report(directory, self.entries), "http://127.0.0.1:1")


if __name__ == "__main__":
    unittest.main()
