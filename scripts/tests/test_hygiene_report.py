import contextlib
import copy
import importlib.util
import io
import json
import os
import sys
import tempfile
import unittest
from datetime import datetime
from pathlib import Path
from unittest.mock import patch


MODULE = Path(__file__).resolve().parents[1] / "hygiene_report.py"
SPEC = importlib.util.spec_from_file_location("hygiene_report", MODULE)
report = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = report  # dataclasses resolve anotações pelo módulo
SPEC.loader.exec_module(report)

FIXTURE = json.loads((Path(__file__).parent / "fixtures" / "hygiene_report.json").read_text(encoding="utf-8"))
NOW = datetime.fromisoformat(FIXTURE["now"].replace("Z", "+00:00"))
READ_ONLY = {("pr", "list"), ("issue", "list"), ("label", "list")}


class FakeRunner:
    """Dublê de ``gh``/``git`` que responde com a fixture e registra as chamadas."""

    def __init__(self, data, labels=("enhancement",), fail_on=None):
        self.data = data
        self.labels = list(labels)
        self.fail_on = fail_on
        self.calls = []

    def __call__(self, cmd, stdin=None):
        self.calls.append((cmd, stdin))
        if self.fail_on and cmd[:2] == self.fail_on:
            raise report.CollectError("falha simulada")
        tool, args = cmd[0], cmd[1:]
        if tool == "gh":
            return self.gh(args)
        return self.git(args)

    def gh(self, args):
        if args[:2] == ["pr", "list"]:
            return json.dumps(self.data["prs"])
        if args[:2] == ["issue", "list"]:
            if "--label" in args:
                return json.dumps(self.data["report_issues"])
            return json.dumps(self.data["issues"])
        if args[:2] == ["label", "list"]:
            return json.dumps([{"name": name} for name in self.labels])
        if args[:2] == ["issue", "create"]:
            return "https://example.invalid/issues/99\n"
        return ""

    def git(self, args):
        if args[0] == "for-each-ref":
            return "".join(f"refs/remotes/origin/{r['name']}\t{r['date']}\n" for r in self.data["refs"])
        if args[0] == "merge-base":
            name = args[2].removeprefix("refs/remotes/origin/")
            if name in self.data["merged"]:
                return ""
            raise report.CollectError("não é ancestral")
        if args[0] == "ls-tree":
            path = args[-1]
            return "\n".join(self.data["archive"] if "archive" in path else self.data["changes"]) + "\n"
        if args[0] == "show":
            if self.data.get("agents") is None:
                raise report.CollectError("sem AGENTS.md")
            return self.data["agents"]
        raise AssertionError(f"git inesperado: {args}")

    def writes(self):
        return [cmd for cmd, _ in self.calls if cmd[0] == "gh" and tuple(cmd[1:3]) not in READ_ONLY]


def sections_for(data):
    collector = report.Collector(FakeRunner(data), None, "origin/main")
    return {s.title: s.lines for s in report.find_sections(collector.snapshot(), NOW)}


def lines_with(sections, prefix):
    return next(lines for title, lines in sections.items() if title.startswith(prefix))


class DetectionTests(unittest.TestCase):
    def setUp(self):
        self.sections = sections_for(FIXTURE)

    def test_stale_drafts(self):
        lines = lines_with(self.sections, "PRs em rascunho")
        self.assertEqual(len(lines), 1)
        self.assertIn("#10", lines[0])

    def test_pr_with_missing_base(self):
        lines = lines_with(self.sections, "PRs abertos cuja base")
        self.assertEqual(lines, ["#12 Base apagada (base `epic/gone`)"])

    def test_spec_issue_with_archived_change(self):
        lines = lines_with(self.sections, "Issues `[SPEC]`")
        self.assertEqual(lines, ["#20 [SPEC] Algo entregue — entregue-change"])

    def test_spec_issue_change_keeps_hyphenated_name(self):
        self.assertEqual(
            report.spec_issue_change("[SPEC] Guardas de higiene do repositório — repository-hygiene-guards"),
            "repository-hygiene-guards",
        )
        self.assertEqual(report.spec_issue_change("[SPEC] Título - build-and-test-quality"), "build-and-test-quality")
        self.assertIsNone(report.spec_issue_change("[SPEC] sem separador"))
        self.assertIsNone(report.spec_issue_change("Título — change"))

    def test_merged_branches_exclude_permanent_and_pr_branches(self):
        lines = lines_with(self.sections, "Branches remotas já contidas")
        self.assertEqual(lines, ["`epic/merged-old`", "`task/old/merged`"])

    def test_stale_branches_exclude_merged_permanent_and_pr_branches(self):
        lines = lines_with(self.sections, "Branches sem PR")
        self.assertEqual(lines, ["`task/old/stale` (último commit em 2026-10-01)"])

    def test_active_changes_on_main(self):
        self.assertEqual(lines_with(self.sections, "Changes OpenSpec"), ["`ativa-change`"])

    def test_old_review_findings(self):
        lines = lines_with(self.sections, "Issues `review-finding`")
        self.assertEqual(lines, ["#22 Achado antigo (aberta em 2026-09-01)"])

    def test_permanent_branches_come_from_agents(self):
        data = copy.deepcopy(FIXTURE)
        data["agents"] = "- Branches permanentes: `main`, `stable`, `pages-store`, `epic/merged-old`.\n"
        lines = lines_with(sections_for(data), "Branches remotas já contidas")
        self.assertEqual(lines, ["`task/old/merged`"])

    def test_default_permanent_branches_without_agents(self):
        data = copy.deepcopy(FIXTURE)
        data["agents"] = None
        sections = sections_for(data)
        self.assertNotIn("`stable`", " ".join(lines_with(sections, "Branches sem PR")))
        self.assertEqual(report.parse_permanent("sem declaração"), {"main", "stable", "pages-store"})

    def test_no_missing_base_when_branches_unknown(self):
        data = copy.deepcopy(FIXTURE)
        data["refs"] = []
        self.assertEqual(lines_with(sections_for(data), "PRs abertos cuja base"), [])

    def test_duplicate_report_issues_are_listed(self):
        data = copy.deepcopy(FIXTURE)
        data["report_issues"] = [
            {"number": 40, "title": report.REPORT_TITLE, "state": "CLOSED"},
            {"number": 30, "title": report.REPORT_TITLE, "state": "OPEN"},
            {"number": 35, "title": "Outro título", "state": "OPEN"},
        ]
        self.assertEqual(lines_with(sections_for(data), "Issues de relatório duplicadas"), ["#40 (closed)"])


class PlanTests(unittest.TestCase):
    def setUp(self):
        self.with_items = [report.Section("x", ["item"])]
        self.empty = [report.Section("x", [])]

    def issue(self, number, state):
        return {"number": number, "title": report.REPORT_TITLE, "state": state}

    def test_items_without_issue_create_it(self):
        self.assertEqual(report.plan_actions(self.with_items, []), [("ensure-label", None), ("create", None)])

    def test_items_with_open_issue_update(self):
        self.assertEqual(report.plan_actions(self.with_items, [self.issue(5, "OPEN")]), [("update", 5)])

    def test_items_with_closed_issue_update_and_reopen(self):
        self.assertEqual(
            report.plan_actions(self.with_items, [self.issue(5, "CLOSED")]), [("update", 5), ("reopen", 5)]
        )

    def test_empty_with_open_issue_comment_and_close(self):
        self.assertEqual(report.plan_actions(self.empty, [self.issue(5, "OPEN")]), [("comment", 5), ("close", 5)])

    def test_empty_without_issue_or_closed_does_nothing(self):
        self.assertEqual(report.plan_actions(self.empty, []), [])
        self.assertEqual(report.plan_actions(self.empty, [self.issue(5, "CLOSED")]), [])

    def test_lowest_number_is_the_report_issue(self):
        issues = [self.issue(9, "OPEN"), self.issue(4, "CLOSED"), {"number": 1, "title": "outro", "state": "OPEN"}]
        self.assertEqual(report.plan_actions(self.with_items, issues), [("update", 4), ("reopen", 4)])


class CliTests(unittest.TestCase):
    def setUp(self):
        env = patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": "", "GITHUB_REPOSITORY": ""})
        env.start()
        self.addCleanup(env.stop)

    def run_main(self, args, runner):
        stdout, stderr = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            code = report.main(args, run=runner, now=NOW)
        return code, stdout.getvalue() + stderr.getvalue()

    def test_dry_run_prints_and_never_writes(self):
        runner = FakeRunner(FIXTURE)
        code, output = self.run_main(["--dry-run"], runner)
        self.assertEqual(code, 0, output)
        self.assertIn("Gerado em 2026-11-23 12:00 UTC", output)
        self.assertIn("**Total:** 8 pendência(s).", output)
        self.assertIn("Ações previstas: ensure-label, create.", output)
        self.assertEqual(runner.writes(), [])

    def test_dry_run_writes_step_summary(self):
        with tempfile.TemporaryDirectory() as directory:
            summary = Path(directory) / "summary.md"
            with patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": str(summary)}):
                self.run_main(["--dry-run"], FakeRunner(FIXTURE))
            self.assertIn(report.REPORT_TITLE, summary.read_text(encoding="utf-8"))

    def test_creates_label_and_issue(self):
        runner = FakeRunner(FIXTURE)
        code, output = self.run_main([], runner)
        self.assertEqual(code, 0, output)
        writes = runner.writes()
        self.assertEqual([cmd[1:3] for cmd in writes], [["label", "create"], ["issue", "create"]])
        create = next((cmd, stdin) for cmd, stdin in runner.calls if cmd[1:3] == ["issue", "create"])
        self.assertIn(report.REPORT_TITLE, create[0])
        self.assertIn(report.REPORT_LABEL, create[0])
        self.assertIn("Gerado em 2026-11-23 12:00 UTC", create[1])

    def test_existing_label_is_not_recreated(self):
        runner = FakeRunner(FIXTURE, labels=("enhancement", report.REPORT_LABEL))
        self.run_main([], runner)
        self.assertEqual([cmd[1:3] for cmd in runner.writes()], [["issue", "create"]])

    def test_updates_and_reopens_closed_report(self):
        data = copy.deepcopy(FIXTURE)
        data["report_issues"] = [{"number": 50, "title": report.REPORT_TITLE, "state": "CLOSED"}]
        runner = FakeRunner(data)
        self.run_main([], runner)
        self.assertEqual(
            [cmd[1:4] for cmd in runner.writes()], [["issue", "edit", "50"], ["issue", "reopen", "50"]]
        )

    def test_closes_open_report_without_items(self):
        data = {
            **copy.deepcopy(FIXTURE),
            "prs": [], "issues": [], "changes": ["openspec/changes/archive"],
            "refs": [{"name": "main", "date": "2026-11-23T10:00:00Z"}],
            "report_issues": [{"number": 50, "title": report.REPORT_TITLE, "state": "OPEN"}],
        }
        runner = FakeRunner(data)
        code, output = self.run_main([], runner)
        self.assertEqual(code, 0, output)
        self.assertEqual(
            [cmd[1:4] for cmd in runner.writes()], [["issue", "comment", "50"], ["issue", "close", "50"]]
        )

    def test_never_touches_other_items(self):
        data = copy.deepcopy(FIXTURE)
        data["report_issues"] = [{"number": 50, "title": report.REPORT_TITLE, "state": "OPEN"}]
        runner = FakeRunner(data)
        self.run_main([], runner)
        for cmd in runner.writes():
            self.assertIn(cmd[1:3], (["issue", "edit"], ["issue", "reopen"], ["issue", "comment"], ["issue", "close"]))
            self.assertEqual(cmd[3], "50")
        self.assertFalse(any(cmd[0] == "git" and cmd[1] in {"push", "branch"} for cmd, _ in runner.calls))

    def test_repo_option_is_passed_to_gh(self):
        runner = FakeRunner(FIXTURE)
        self.run_main(["--dry-run", "--repo", "dono/repo"], runner)
        gh_calls = [cmd for cmd, _ in runner.calls if cmd[0] == "gh"]
        self.assertTrue(gh_calls)
        self.assertTrue(all(cmd[3:5] == ["-R", "dono/repo"] for cmd in gh_calls))

    def test_collect_error_exits_2_without_writing(self):
        runner = FakeRunner(FIXTURE, fail_on=["gh", "pr"])
        code, output = self.run_main([], runner)
        self.assertEqual(code, 2)
        self.assertIn("ERRO", output)
        self.assertEqual(runner.writes(), [])


if __name__ == "__main__":
    unittest.main()
