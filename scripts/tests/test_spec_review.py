"""Repos Git isolados: protege metadados, relatório pendente e revisão desatualizada."""

import copy
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock

MODULE = Path(__file__).resolve().parents[1] / "check_spec_review.py"
loader = importlib.util.spec_from_file_location("check_spec_review", MODULE)
gate = importlib.util.module_from_spec(loader)
loader.loader.exec_module(gate)


class SpecReviewTests(unittest.TestCase):
    def setUp(self):
        self.cwd = Path.cwd()
        preferred_tmp = "/tmp/opencode" if Path("/tmp/opencode").is_dir() else None
        self.temp = tempfile.TemporaryDirectory(prefix="spec-review-test-", dir=preferred_tmp)
        os.chdir(self.temp.name)
        self.git("init", "--initial-branch=main")
        self.git("config", "user.name", "Test fixture")
        self.git("config", "user.email", "fixture@example.invalid")
        self.readme = [f"line {index}\n" for index in range(30)]
        self.readme[6] = "\n"
        self.write("README.md", "".join(self.readme))
        self.base = self.commit("base")
        self.readme[5] = "first change\n"
        self.readme[20] = "second change\n"
        self.write("README.md", "".join(self.readme))
        self.write("backend/feature.txt", "implementation\n")
        self.write("docs/revisão.txt", "UTF-8 path fixture\n")
        self.head = self.commit("feature")
        self.spec = "fixture-spec"
        self.paths = gate.report_paths(self.spec)
        self.explanation = "\n".join((
            "# Review fixture", "## Escopo e cobertura", "All diff inspected.",
            "## Achados e triagem", "None, with rationale.",
            "## Evidências e limitações", "Fixture does not attest model execution.",
            "## Revalidação", "Reviewed current snapshot."))
        self.report = {
            "version": 1, "status": "completed", "spec": self.spec, "pr": 25,
            **gate.snapshot(self.base, self.head, self.spec),
            "reviewer": {"model": "fixture/reviewer#high", "session": "fixture-session",
                         "independent_context": True},
            "reviewed_at": "2026-10-09T00:00:00Z",
            "coverage": {area: "Inspected or not applicable with explanation" for area in (
                "requirements", "architecture", "integration", "security", "persistence", "tests", "maintenance")},
            "limitations": ["Synthetic test evidence, not a real review."],
            "verification": [{"command": "fixture", "result": "passed", "executed_by": "fixture"}],
            "findings": [], "triage_complete": True,
        }

    def tearDown(self):
        os.chdir(self.cwd)
        self.temp.cleanup()

    def git(self, *args):
        return subprocess.check_output(["git", *args], stderr=subprocess.PIPE).decode().strip()

    def write(self, name, contents):
        path = Path(name)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents)

    def commit(self, message):
        self.git("add", ".")
        self.git("commit", "-m", message)
        return self.git("rev-parse", "HEAD")

    def record(self):
        self.write(self.paths[0], json.dumps(self.report))
        self.write(self.paths[1], self.explanation)
        return self.commit("report")

    def validate(self, report=None, head=None):
        gate.validate_report(self.report if report is None else report, self.explanation,
                             self.base, self.head if head is None else head, self.spec, 25)

    def check(self, **overrides):
        args = {"base_ref": "epic/application-reformulation", "head_ref": "epic/fixture-spec",
                "title": "[SPEC] Fixture", "base": self.base, "head": self.head, "pr": 25,
                "head_repository": "fixture/repo", "base_repository": "fixture/repo"}
        args.update(overrides)
        return gate.check(**args)

    def test_completed_review_without_findings_passes(self):
        self.validate()

    def test_report_commits_do_not_create_invalidation_loop(self):
        head = self.record()
        self.assertIn("OK:", self.check(head=head))
        self.write(self.paths[1], self.explanation + "\nClarified evidence.\n")
        self.validate(head=self.commit("clarify report"))

    def test_pending_review_and_wrong_spec_pr_are_blocked(self):
        for field, value in (("status", "pending"), ("spec", "other"), ("pr", 26), ("version", 2)):
            report = copy.deepcopy(self.report)
            report[field] = value
            with self.subTest(field=field), self.assertRaises(gate.ReviewError):
                self.validate(report)

    def test_code_docs_tests_workflow_and_other_report_changes_invalidate(self):
        for index, path in enumerate(("backend/feature.txt", "AGENTS.md", "tests/new.txt", ".github/workflows/check.yml",
                                     "openspec/changes/other-spec/spec-review.md")):
            with self.subTest(path=path):
                # Branch independente do head original: um caso não mascara o próximo.
                self.git("switch", "-c", f"invalidation-{index}", self.head)
                self.validate(head=self.git("rev-parse", "HEAD"))
                self.write(path, "new content\n")
                head = self.commit(f"change {path}")
                self.assertEqual(self.git("diff", "--name-only", self.head, head), path)
                with self.assertRaisesRegex(gate.ReviewError, "Diff mudou"):
                    self.validate(head=head)

    def test_snapshot_is_independent_of_local_diff_rendering_preferences(self):
        expected = gate.snapshot(self.base, self.head, self.spec)
        self.write("diff-order.txt", "backend/*\nREADME.md\n")
        preferences = (("diff.context", "8"), ("diff.interHunkContext", "30"),
                       ("diff.indentHeuristic", "true"), ("diff.algorithm", "histogram"),
                       ("diff.color", "always"), ("diff.mnemonicPrefix", "true"),
                       ("diff.noprefix", "true"), ("diff.relative", "true"),
                       ("diff.suppressBlankEmpty", "true"), ("core.quotePath", "false"),
                       ("diff.orderFile", "diff-order.txt"))
        for key, value in preferences:
            with self.subTest(preference=key):
                self.git("config", key, value)
                try:
                    self.assertEqual(gate.snapshot(self.base, self.head, self.spec), expected)
                    self.validate()
                finally:
                    self.git("config", "--unset", key)

    def test_snapshot_ignores_git_diff_opts_environment(self):
        expected = gate.snapshot(self.base, self.head, self.spec)
        for value in ("--unified=0", "--unified=20"):
            with self.subTest(value=value), mock.patch.dict(os.environ, {"GIT_DIFF_OPTS": value}):
                self.assertEqual(gate.snapshot(self.base, self.head, self.spec), expected)
                self.validate()
                self.assertEqual(os.environ["GIT_DIFF_OPTS"], value)

    def test_advancing_and_merging_base_requires_new_review_even_with_same_diff(self):
        self.git("switch", "-c", "advanced-base", self.base)
        self.write("base-only.txt", "independent base change\n")
        advanced_base = self.commit("advance base")
        self.git("switch", "main")
        self.git("merge", "--no-edit", "advanced-base")
        head = self.git("rev-parse", "HEAD")
        new_snapshot = gate.snapshot(advanced_base, head, self.spec)
        self.assertEqual(new_snapshot["diff_sha256"], self.report["diff_sha256"])
        with self.assertRaisesRegex(gate.ReviewError, "Base mudou"):
            gate.validate_report(self.report, self.explanation, advanced_base, head, self.spec, 25)
        revalidated = {**self.report, **new_snapshot}
        gate.validate_report(revalidated, self.explanation, advanced_base, head, self.spec, 25)

    def test_forged_digest_and_changed_base_are_blocked(self):
        report = copy.deepcopy(self.report)
        report["diff_sha256"] = "0" * 64
        with self.assertRaisesRegex(gate.ReviewError, "digest"):
            self.validate(report)
        report = copy.deepcopy(self.report)
        report["base_sha"] = self.head
        with self.assertRaisesRegex(gate.ReviewError, "Base mudou"):
            self.validate(report)

    def test_non_ancestor_review_is_blocked(self):
        self.git("checkout", "-b", "unrelated", self.base)
        self.write("other.txt", "other branch")
        report = copy.deepcopy(self.report)
        report["reviewed_head_sha"] = self.commit("other")
        with self.assertRaisesRegex(gate.ReviewError, "ancestral"):
            self.validate(report)

    def test_missing_model_session_or_independence_is_blocked(self):
        for field, value in (("model", None), ("model", "unknown"), ("session", ""), ("independent_context", False)):
            report = copy.deepcopy(self.report)
            report["reviewer"][field] = value
            with self.subTest(field=field), self.assertRaises(gate.ReviewError):
                self.validate(report)

    def test_missing_coverage_verification_limitations_or_triage_is_blocked(self):
        for field, value in (("coverage", {}), ("verification", []), ("limitations", []), ("triage_complete", False),
                             ("reviewed_at", "2026-10-09T00:00:00"), ("reviewed_at", "not-a-date")):
            report = copy.deepcopy(self.report)
            report[field] = value
            with self.subTest(field=field), self.assertRaises(gate.ReviewError):
                self.validate(report)

    def finding(self, **changes):
        item = {"id": "ARQ-01", "severity": "blocking", "location": "fixture:1",
                "evidence": "demonstration", "reference": "ADR", "impact": "inconsistent responsibility",
                "suggestion": "centralize", "disposition": "fixed", "resolution": "revalidated with evidence"}
        item.update(changes)
        return item

    def test_resolved_and_refuted_blockers_pass_but_deferred_or_open_do_not(self):
        for disposition in ("fixed", "rejected", "deferred", "open"):
            report = copy.deepcopy(self.report)
            report["findings"] = [self.finding(disposition=disposition)]
            with self.subTest(disposition=disposition):
                if disposition in ("fixed", "rejected"):
                    self.validate(report)
                else:
                    with self.assertRaises(gate.ReviewError):
                        self.validate(report)

    def test_deferred_optional_finding_needs_resolution_and_unique_id(self):
        report = copy.deepcopy(self.report)
        report["findings"] = [self.finding(severity="optional", disposition="deferred")]
        self.validate(report)
        report["findings"].append(copy.deepcopy(report["findings"][0]))
        with self.assertRaisesRegex(gate.ReviewError, "duplicados"):
            self.validate(report)
        report["findings"] = [self.finding(resolution="")]
        with self.assertRaises(gate.ReviewError):
            self.validate(report)

    def test_title_or_branch_rename_does_not_bypass_spec_gate(self):
        for changes in ({"title": "Without prefix"}, {"title": "[SPEC] "}, {"head_ref": "task/hide-spec"},
                        {"head_ref": "epic/../main"}):
            with self.subTest(changes=changes), self.assertRaises(gate.ReviewError):
                self.check(**changes)

    def test_main_sync_only_exempts_same_repository(self):
        self.assertIn("sincronização", self.check(head_ref="main", title="Sync main"))
        with self.assertRaises(gate.ReviewError):
            self.check(head_ref="main", title="Sync main", head_repository="fork/repo")
        with self.assertRaises(gate.ReviewError):
            self.check(head_ref="main", title="[SPEC] Sync main")

    def test_task_pr_is_not_applicable(self):
        self.assertIn("Não aplicável", self.check(base_ref="epic/fixture-spec", head_ref="task/fixture/task"))

    def test_missing_report_or_worktree_only_evidence_is_blocked(self):
        with self.assertRaises(subprocess.CalledProcessError):
            self.check()
        self.write(self.paths[0], json.dumps(self.report))
        self.write(self.paths[1], self.explanation)
        with self.assertRaises(subprocess.CalledProcessError):
            self.check()  # arquivo não commitado nunca é evidência do head.

    def test_invalid_sha_and_missing_markdown_sections_are_blocked(self):
        for sha in (None, "HEAD", "--help", "../main"):
            with self.subTest(sha=sha), self.assertRaises(gate.ReviewError):
                gate.validate_sha(sha)
        self.explanation = "Review without evidence sections"
        with self.assertRaisesRegex(gate.ReviewError, "humano incompleto"):
            self.validate()


if __name__ == "__main__":
    unittest.main()
