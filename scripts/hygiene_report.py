#!/usr/bin/env python3
"""Relatório semanal de pendências do repositório.

Lista PRs, issues, branches e changes OpenSpec esquecidos e mantém uma única
issue ``[PENDÊNCIAS] Relatório semanal`` (label ``hygiene-report``). Nunca
fecha, edita, comenta ou apaga outros PRs, issues ou branches (spec
``repository-hygiene-guards``, design D5).

Uso: ``python3 scripts/hygiene_report.py [--dry-run]``. Requer ``gh``
autenticado e as branches remotas atualizadas (``git fetch --prune origin``).
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import Callable

REPORT_TITLE = "[PENDÊNCIAS] Relatório semanal"
REPORT_LABEL = "hygiene-report"
REPORT_LABEL_COLOR = "C5DEF5"
REPORT_LABEL_DESCRIPTION = "Relatório semanal de pendências (gerado pelo workflow hygiene-report)"
REVIEW_FINDING_LABEL = "review-finding"
DEFAULT_PERMANENT = ("main", "stable", "pages-store")
MAIN_BRANCH = "main"
CHANGES_DIR = "openspec/changes"
STALE_DRAFT_DAYS = 14
STALE_BRANCH_DAYS = 30
OLD_FINDING_DAYS = 60
DATED = re.compile(r"^\d{4}-\d{2}-\d{2}-(.+)$")
# Separador com espaço dos dois lados: o hífen interno de `nome-da-change` não conta.
SPEC_ISSUE = re.compile(r"^\[SPEC\].*\s[—–-]\s+([a-z0-9][a-z0-9-]*)\s*$", re.IGNORECASE)
PERMANENT_LINE = re.compile(r"^\s*[-*]?\s*Branches permanentes:\s*(.+)$", re.IGNORECASE | re.MULTILINE)

Runner = Callable[[list[str], "str | None"], str]


class CollectError(Exception):
    """Falha ao consultar ``gh`` ou ``git``."""


@dataclass
class Branch:
    name: str
    committed_at: datetime
    merged: bool


@dataclass
class Snapshot:
    prs: list[dict]
    issues: list[dict]
    report_issues: list[dict]
    branches: list[Branch]
    active_changes: list[str]
    archived_changes: set[str]
    permanent: set[str]


@dataclass
class Section:
    title: str
    lines: list[str] = field(default_factory=list)


# ---------- utilidades ----------


def parse_time(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def archived_name(directory: str) -> str:
    match = DATED.match(directory)
    return match.group(1) if match else directory


def parse_permanent(agents_text: str | None) -> set[str]:
    match = PERMANENT_LINE.search(agents_text or "")
    if not match:
        return set(DEFAULT_PERMANENT)
    names = re.split(r"[,\s]+", match.group(1).replace("`", ""))
    return {name.rstrip(".;") for name in names if name.rstrip(".;")}


def spec_issue_change(title: str) -> str | None:
    match = SPEC_ISSUE.match(title or "")
    return match.group(1) if match else None


def link(item: dict) -> str:
    return f"#{item['number']}"


# ---------- detecção (funções puras) ----------


def find_sections(snapshot: Snapshot, now: datetime) -> list[Section]:
    prs = snapshot.prs
    branch_names = {b.name for b in snapshot.branches}
    in_prs = {p["baseRefName"] for p in prs} | {p["headRefName"] for p in prs}
    candidates = [b for b in snapshot.branches if b.name not in snapshot.permanent and b.name not in in_prs]

    drafts = Section(f"PRs em rascunho sem atualização há mais de {STALE_DRAFT_DAYS} dias")
    for pr in sorted(prs, key=lambda p: p["number"]):
        updated = parse_time(pr["updatedAt"])
        if pr.get("isDraft") and now - updated > timedelta(days=STALE_DRAFT_DAYS):
            drafts.lines.append(f"{link(pr)} {pr['title']} (atualizado em {updated.date()})")

    orphan_base = Section("PRs abertos cuja base não existe mais")
    if branch_names:
        for pr in sorted(prs, key=lambda p: p["number"]):
            if pr["baseRefName"] not in branch_names:
                orphan_base.lines.append(f"{link(pr)} {pr['title']} (base `{pr['baseRefName']}`)")

    spec_issues = Section("Issues `[SPEC]` abertas com a change já arquivada")
    for issue in sorted(snapshot.issues, key=lambda i: i["number"]):
        change = spec_issue_change(issue["title"])
        if change and change in snapshot.archived_changes:
            spec_issues.lines.append(f"{link(issue)} {issue['title']}")

    merged = Section(f"Branches remotas já contidas na `{MAIN_BRANCH}` (sem PR aberto)")
    merged_names = set()
    for branch in sorted(candidates, key=lambda b: b.name):
        if branch.merged:
            merged_names.add(branch.name)
            merged.lines.append(f"`{branch.name}`")

    stale = Section(f"Branches sem PR aberto e sem commit há mais de {STALE_BRANCH_DAYS} dias")
    for branch in sorted(candidates, key=lambda b: b.name):
        if branch.name in merged_names:
            continue
        if now - branch.committed_at > timedelta(days=STALE_BRANCH_DAYS):
            stale.lines.append(f"`{branch.name}` (último commit em {branch.committed_at.date()})")

    changes = Section(f"Changes OpenSpec fora de `archive/` na `{MAIN_BRANCH}`")
    for change in sorted(snapshot.active_changes):
        changes.lines.append(f"`{change}`")

    findings = Section(f"Issues `{REVIEW_FINDING_LABEL}` abertas há mais de {OLD_FINDING_DAYS} dias")
    for issue in sorted(snapshot.issues, key=lambda i: i["number"]):
        labels = {label["name"] for label in issue.get("labels", [])}
        created = parse_time(issue["createdAt"])
        if REVIEW_FINDING_LABEL in labels and now - created > timedelta(days=OLD_FINDING_DAYS):
            findings.lines.append(f"{link(issue)} {issue['title']} (aberta em {created.date()})")

    duplicates = Section("Issues de relatório duplicadas (não são alteradas pelo relatório)")
    for issue in report_issues_sorted(snapshot.report_issues)[1:]:
        duplicates.lines.append(f"{link(issue)} ({issue['state'].lower()})")

    return [drafts, orphan_base, spec_issues, merged, stale, changes, findings, duplicates]


def report_issues_sorted(issues: list[dict]) -> list[dict]:
    return sorted((i for i in issues if i["title"] == REPORT_TITLE), key=lambda i: i["number"])


def has_items(sections: list[Section]) -> bool:
    return any(section.lines for section in sections)


def render(sections: list[Section], now: datetime) -> str:
    stamp = now.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    out = [
        f"Gerado em {stamp} pelo workflow `hygiene-report` (`scripts/hygiene_report.py`).",
        "",
        "Este relatório só lista pendências: não fecha, edita nem apaga os itens. Agir sobre "
        "eles segue as autorizações de sempre (`~/.claude/rules/pendencias-e-encerramento.md`).",
    ]
    total = sum(len(s.lines) for s in sections)
    out += ["", f"**Total:** {total} pendência(s)."]
    for section in sections:
        if not section.lines:
            continue
        out += ["", f"## {section.title} ({len(section.lines)})", ""]
        out += [f"- {line}" for line in section.lines]
    return "\n".join(out) + "\n"


def plan_actions(sections: list[Section], report_issues: list[dict]) -> list[tuple[str, int | None]]:
    """Decide o que fazer com a issue do relatório: create/update/reopen/comment/close."""
    target = (report_issues_sorted(report_issues) or [None])[0]
    if has_items(sections):
        if target is None:
            return [("ensure-label", None), ("create", None)]
        actions = [("update", target["number"])]
        if target["state"].upper() != "OPEN":
            actions.append(("reopen", target["number"]))
        return actions
    if target is not None and target["state"].upper() == "OPEN":
        return [("comment", target["number"]), ("close", target["number"])]
    return []


# ---------- coleta ----------


def default_runner(cmd: list[str], stdin: str | None = None) -> str:
    try:
        completed = subprocess.run(
            cmd, input=stdin, capture_output=True, text=True, encoding="utf-8", check=False
        )
    except OSError as error:
        raise CollectError(f"não foi possível executar {cmd[0]}: {error}") from error
    if completed.returncode != 0:
        detail = completed.stderr.strip() or completed.stdout.strip()
        raise CollectError(f"{' '.join(cmd[:3])} falhou: {detail}")
    return completed.stdout


class Collector:
    def __init__(self, run: Runner, repo: str | None, main_ref: str):
        self.run = run
        self.repo = repo
        self.main_ref = main_ref

    def gh(self, *args: str, stdin: str | None = None) -> str:
        repo = ["-R", self.repo] if self.repo and args[0] in {"pr", "issue", "label"} else []
        return self.run(["gh", args[0], args[1], *repo, *args[2:]], stdin)

    def gh_json(self, *args: str):
        return json.loads(self.gh(*args) or "[]")

    def git(self, *args: str) -> str:
        return self.run(["git", *args], None)

    def is_ancestor(self, ref: str) -> bool:
        try:
            self.git("merge-base", "--is-ancestor", ref, self.main_ref)
            return True
        except CollectError:
            return False

    def list_dirs(self, path: str) -> list[str]:
        out = self.git("ls-tree", "-d", "--name-only", self.main_ref, f"{path}/")
        return [line.rsplit("/", 1)[-1] for line in out.splitlines() if line.strip()]

    def snapshot(self) -> Snapshot:
        prs = self.gh_json(
            "pr", "list", "--state", "open", "--limit", "200",
            "--json", "number,title,isDraft,updatedAt,baseRefName,headRefName,url",
        )
        issues = self.gh_json(
            "issue", "list", "--state", "open", "--limit", "500",
            "--json", "number,title,createdAt,updatedAt,url,labels",
        )
        report_issues = self.gh_json(
            "issue", "list", "--state", "all", "--label", REPORT_LABEL, "--limit", "50",
            "--json", "number,title,state",
        )
        refs = self.git(
            "for-each-ref", "--format=%(refname)%09%(committerdate:iso-strict)", "refs/remotes/origin/"
        )
        branches = []
        for line in refs.splitlines():
            if not line.strip():
                continue
            ref, date = line.split("\t", 1)
            name = ref.removeprefix("refs/remotes/origin/")
            if name == "HEAD":
                continue
            branches.append(Branch(name, parse_time(date), self.is_ancestor(ref)))
        entries = self.list_dirs(CHANGES_DIR)
        active = [e for e in entries if e != "archive"]
        archived = {archived_name(d) for d in self.list_dirs(f"{CHANGES_DIR}/archive")} if "archive" in entries else set()
        try:
            agents = self.git("show", f"{self.main_ref}:AGENTS.md")
        except CollectError:
            agents = None
        return Snapshot(prs, issues, report_issues, branches, active, archived, parse_permanent(agents))

    def apply(self, actions: list[tuple[str, int | None]], body: str, now: datetime) -> list[str]:
        done = []
        for action, number in actions:
            if action == "ensure-label":
                labels = self.gh_json("label", "list", "--limit", "200", "--json", "name")
                if REPORT_LABEL not in {label["name"] for label in labels}:
                    self.gh(
                        "label", "create", REPORT_LABEL,
                        "--color", REPORT_LABEL_COLOR, "--description", REPORT_LABEL_DESCRIPTION,
                    )
                    done.append(f"label `{REPORT_LABEL}` criada")
            elif action == "create":
                url = self.gh(
                    "issue", "create", "--title", REPORT_TITLE, "--label", REPORT_LABEL,
                    "--body-file", "-", stdin=body,
                ).strip()
                done.append(f"issue criada: {url}")
            elif action == "update":
                self.gh("issue", "edit", str(number), "--body-file", "-", stdin=body)
                done.append(f"issue #{number} atualizada")
            elif action == "reopen":
                self.gh("issue", "reopen", str(number))
                done.append(f"issue #{number} reaberta")
            elif action == "comment":
                stamp = now.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
                self.gh("issue", "comment", str(number), "--body", f"Sem pendências em {stamp}.")
                done.append(f"issue #{number} comentada")
            elif action == "close":
                self.gh("issue", "close", str(number))
                done.append(f"issue #{number} fechada")
            else:  # pragma: no cover - proteção contra ação desconhecida
                raise ValueError(action)
        return done


# ---------- CLI ----------


def write_summary(text: str) -> None:
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as handle:
            handle.write(text)


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Relatório semanal de pendências do repositório.")
    parser.add_argument("--dry-run", action="store_true", help="só imprime; não escreve no GitHub")
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="owner/nome")
    parser.add_argument("--main-ref", default=f"origin/{MAIN_BRANCH}", help="ref da branch padrão")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None, run: Runner = default_runner, now: datetime | None = None) -> int:
    args = parse_args(argv)
    now = now or datetime.now(timezone.utc)
    collector = Collector(run, args.repo, args.main_ref)
    try:
        snapshot = collector.snapshot()
        sections = find_sections(snapshot, now)
        body = render(sections, now)
        print(f"# {REPORT_TITLE}\n\n{body}")
        write_summary(f"# {REPORT_TITLE}\n\n{body}")
        actions = plan_actions(sections, snapshot.report_issues)
        if args.dry_run:
            planned = ", ".join(f"{a}{'' if n is None else f' #{n}'}" for a, n in actions) or "nenhuma"
            print(f"Modo verificação (--dry-run): nenhuma escrita no GitHub. Ações previstas: {planned}.")
            return 0
        for line in collector.apply(actions, body, now) or ["nenhuma ação na issue do relatório"]:
            print(line)
    except CollectError as error:
        print(f"ERRO: {error}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
