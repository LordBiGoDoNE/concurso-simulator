"""Fixtures de cota sintéticas; não representam consumo real da assinatura."""

import copy
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from review_quota import compare, validate


def snapshot(moment="before", remaining=80, isolated=True):
    return {"version": 1, "batch": "fixture-round", "phase": "initial", "moment": moment,
            "model": "openai/gpt-6-astra#medium", "session": "ses_fixture",
            "observed_at": "2026-10-11T10:00:00Z" if moment == "before" else "2026-10-11T10:20:00Z",
            "isolation_declared": isolated,
            "windows": {"5h": {"remaining_percent": remaining, "reset_at": "2026-10-11T15:00:00Z"},
                        "weekly": {"remaining_percent": 90, "reset_at": "2026-10-18T00:00:00Z"}}}


class ReviewQuotaTests(unittest.TestCase):
    def test_controlled_observation_is_percentage_points_not_money(self):
        result = compare(snapshot(), snapshot("after", 76))
        self.assertEqual(result["windows"]["5h"], {"status": "controlled-account-observation", "observed_decrease_pp": 4})
        self.assertEqual(result["windows"]["weekly"]["status"], "not-resolved")
        self.assertNotIn("cost", result)

    def test_overlap_is_not_attributed_and_zero_is_not_free(self):
        result = compare(snapshot(isolated=False), snapshot("after", 76))
        self.assertEqual(result["windows"]["5h"]["status"], "unattributable")
        self.assertEqual(compare(snapshot(), snapshot("after"))["windows"]["5h"]["status"], "not-resolved")

    def test_reset_change_and_replenishment_do_not_produce_consumption(self):
        after = snapshot("after", 99)
        after["windows"]["5h"]["reset_at"] = "2026-10-11T16:00:00Z"
        result = compare(snapshot(), after)["windows"]["5h"]
        self.assertEqual(result, {"status": "reset-changed", "observed_decrease_pp": None})
        self.assertEqual(compare(snapshot(), snapshot("after", 99))["windows"]["5h"]["status"], "replenished-or-inconsistent")

    def test_unavailable_window_is_not_invented(self):
        after = snapshot("after", 76)
        after["windows"]["weekly"] = {"remaining_percent": None, "reset_at": None}
        self.assertEqual(compare(snapshot(), after)["windows"]["weekly"], {"status": "unavailable", "observed_decrease_pp": None})

    def test_timezones_are_compared_as_instants_and_bad_order_is_rejected(self):
        after = snapshot("after", 76)
        after["windows"]["5h"]["reset_at"] = "2026-10-11T12:00:00-03:00"
        self.assertEqual(compare(snapshot(), after)["windows"]["5h"]["observed_decrease_pp"], 4)
        after["observed_at"] = "2026-10-11T10:00:00Z"
        with self.assertRaises(ValueError):
            compare(snapshot(), after)

    def test_model_session_batch_phase_and_moment_cannot_be_mixed(self):
        for key, value in (("model", "openai/gpt-6.1-sol#xhigh"), ("session", "ses_other"),
                           ("batch", "other"), ("phase", "revalidation"), ("moment", "before")):
            after = snapshot("after", 76)
            after[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError):
                compare(snapshot(), after)

    def test_rejects_bad_percentages_expired_resets_and_extra_diagnostics(self):
        for value in (-1, 101, True, "62", float("nan"), float("inf")):
            invalid = snapshot()
            invalid["windows"]["5h"]["remaining_percent"] = value
            with self.subTest(value=value), self.assertRaises(ValueError):
                validate(invalid)
        for key, value in (("credential", "PRIVATE_TEST_MARKER"), ("isolation_declared", "yes"),
                           ("observed_at", "2026-10-11T16:00:00Z"), ("observed_at", "2026-10-11T10:00:00")):
            invalid = snapshot()
            invalid[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError):
                validate(invalid)

    def test_cli_emits_valid_snapshot_and_comparison_without_mutating_inputs(self):
        script = Path(__file__).resolve().parents[1] / "review_quota.py"
        value = snapshot()
        result = subprocess.run([sys.executable, str(script), "snapshot"], input=json.dumps(value), text=True, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout), value)
        with tempfile.TemporaryDirectory() as directory:
            first, second = Path(directory) / "before.json", Path(directory) / "after.json"
            first.write_text(json.dumps(value))
            second.write_text(json.dumps(snapshot("after", 76)))
            original = first.read_bytes(), second.read_bytes()
            result = subprocess.run([sys.executable, str(script), "compare", str(first), str(second)], text=True, capture_output=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(json.loads(result.stdout)["windows"]["5h"]["observed_decrease_pp"], 4)
            self.assertEqual(original, (first.read_bytes(), second.read_bytes()))
        invalid = copy.deepcopy(value)
        invalid["credential"] = "PRIVATE_TEST_MARKER"
        result = subprocess.run([sys.executable, str(script), "snapshot"], input=json.dumps(invalid), text=True, capture_output=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn("PRIVATE_TEST_MARKER", result.stdout + result.stderr)
        invalid = copy.deepcopy(value)
        invalid["observed_at"] = "PRIVATE_TEST_MARKER"
        result = subprocess.run([sys.executable, str(script), "snapshot"], input=json.dumps(invalid), text=True, capture_output=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn("PRIVATE_TEST_MARKER", result.stdout + result.stderr)
