import hashlib
import json
import tempfile
import unittest
from pathlib import Path

import vps_question_load as loader


class FakeApi:
    def __init__(self, package_digest="pd", fail_on=None):
        self.calls, self.package_digest, self.fail_on, self.nonce = [], package_digest, fail_on, 0

    def ok(self, method, path, body=None, expected=(200,)):
        self.calls.append(path)
        if path == self.fail_on:
            raise RuntimeError("falha simulada")
        if path.endswith("question-imports/preview"):
            return {"packageDigest": self.package_digest, "selectionDigest": "sd", "confirmable": True, "confirmationToken": "t"}
        if path.endswith("question-imports/apply"):
            assert body["confirmationToken"] == "t"
            return {"receipt": {"confirmation": {"nonce": self.next()}}}
        if path.endswith("editorial-decisions/preview"):
            assert body["importReceiptId"] and body["items"] is None
            assert (body["reviewKind"], body["checklist"]) in ((None, []), ("AI_ONLY", loader.CHECKLIST))
            return {"confirmable": True, "reviewKind": body["reviewKind"], "checklist": body["checklist"],
                    "rationale": body["rationale"], "expected": [{"questionId": "q"}], "confirmationToken": "b"}
        if path.endswith("editorial-decisions/apply"):
            return {"receipt": {"confirmation": {"nonce": self.next()}}, "replayed": False}
        raise AssertionError(path)

    def next(self):
        self.nonce += 1
        return f"n{self.nonce}"


class LoaderTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.packages = self.root / "packages"
        entries = []
        for purpose, topic in (("SIMULADO", "P01"), ("PRATICA_AULA", "P01")):
            folder = self.packages / purpose / topic
            folder.mkdir(parents=True)
            raw = json.dumps({"package": {"items": []}, "keys": [f"admin.2026-11.{topic}"]}).encode()
            (folder / "preview-request.json").write_bytes(raw)
            entries.append({"purpose": purpose, "topicId": topic, "items": 25, "packageDigest": "pd",
                            "selectionDigest": "sd", "requestSha256": hashlib.sha256(raw).hexdigest()})
        (self.packages / "manifest.json").write_text(json.dumps({"packages": entries}), encoding="utf-8")
        self.journal = self.root / "state" / "question-load.json"

    def run_load(self, api, **kwargs):
        return loader.load(api, self.packages, self.journal, out=lambda _: None, **kwargs)

    def test_each_package_is_imported_submitted_and_approved_once(self):
        api = FakeApi()
        self.run_load(api)
        self.assertEqual(api.calls.count("/api/v1/admin/question-imports/apply"), 2)
        self.assertEqual(api.calls.count("/api/v1/admin/editorial-decisions/apply"), 4)
        journal = json.loads(self.journal.read_text())
        self.assertTrue(all(v["approve"] for v in journal["packages"].values()))
        again = FakeApi()
        self.run_load(again)
        self.assertEqual(again.calls, [])

    def test_resume_after_failure_reuses_the_receipt(self):
        api = FakeApi(fail_on="/api/v1/admin/editorial-decisions/apply")
        with self.assertRaises(RuntimeError):
            self.run_load(api)
        self.assertEqual(api.calls.count("/api/v1/admin/question-imports/apply"), 1)
        resumed = FakeApi()
        self.run_load(resumed)
        self.assertEqual(resumed.calls.count("/api/v1/admin/question-imports/apply"), 1)  # only the second package
        journal = json.loads(self.journal.read_text())
        self.assertEqual(journal["packages"]["SIMULADO/P01"]["receipt"], "n1")

    def test_digest_divergence_stops_before_apply(self):
        api = FakeApi(package_digest="outro")
        with self.assertRaises(RuntimeError):
            self.run_load(api)
        self.assertNotIn("/api/v1/admin/question-imports/apply", api.calls)

    def test_tampered_request_is_refused_before_any_call(self):
        (self.packages / "SIMULADO/P01/preview-request.json").write_bytes(b"{}")
        api = FakeApi()
        with self.assertRaises(RuntimeError):
            self.run_load(api)
        self.assertEqual(api.calls, [])

    def test_dry_run_only_previews(self):
        api = FakeApi()
        self.run_load(api, dry_run=True)
        self.assertEqual(set(api.calls), {"/api/v1/admin/question-imports/preview"})
        self.assertFalse(self.journal.exists())

    def test_checkout_must_be_at_the_release_commit(self):
        import subprocess
        head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=loader.ROOT, text=True).strip()
        loader.require_release_checkout({"rehearsal": True, "images": {"source_sha": head}})
        with self.assertRaises(RuntimeError):
            loader.require_release_checkout({"rehearsal": True, "images": {"source_sha": "0" * 40}})

    def test_versioned_manifest_matches_its_requests(self):
        manifest = json.loads((loader.PACKAGES / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(len(manifest["packages"]), 102)
        self.assertEqual(sum(e["items"] for e in manifest["packages"]), 2587)
        for entry in manifest["packages"]:
            raw = (loader.PACKAGES / entry["purpose"] / entry["topicId"] / "preview-request.json").read_bytes()
            self.assertEqual(hashlib.sha256(raw).hexdigest(), entry["requestSha256"], entry["topicId"])


if __name__ == "__main__":
    unittest.main()
