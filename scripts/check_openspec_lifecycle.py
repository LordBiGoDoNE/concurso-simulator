#!/usr/bin/env python3
"""Checa o ciclo de vida das changes OpenSpec entre a base e o head de um PR.

Em PR para ``main``, falha quando uma change que já estava fora de
``openspec/changes/archive/`` na base continua fora no head e o PR não arquiva
nenhuma change. A decisão usa o conteúdo das árvores Git, nunca o título do PR
nem o nome da branch (spec ``repository-hygiene-guards``, design D1).

Códigos de saída: 0 sem falhas, 1 com falhas, 2 para erro de uso ou de ``git``.
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

CHANGES_DIR = "openspec/changes"
ARCHIVE = "archive"
MAIN_BRANCH = "main"
DATED = re.compile(r"^\d{4}-\d{2}-\d{2}-(.+)$")


class GitError(Exception):
    """Falha ao consultar o Git; nunca deve virar "checagem aprovada"."""


@dataclass(frozen=True)
class ChangeTree:
    active: frozenset[str]
    archive_dirs: frozenset[str]

    @property
    def archived(self) -> frozenset[str]:
        return frozenset(archived_name(d) for d in self.archive_dirs)


@dataclass
class Result:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)


def archived_name(directory: str) -> str:
    match = DATED.match(directory)
    return match.group(1) if match else directory


def git(args: list[str], cwd: Path) -> str:
    try:
        completed = subprocess.run(
            ["git", *args],
            cwd=cwd,
            capture_output=True,
            text=True,
            encoding="utf-8",
            check=False,
        )
    except OSError as error:
        raise GitError(f"não foi possível executar git: {error}") from error
    if completed.returncode != 0:
        detail = completed.stderr.strip() or completed.stdout.strip()
        raise GitError(f"git {' '.join(args)} falhou: {detail}")
    return completed.stdout


def list_dirs(ref: str, path: str, cwd: Path) -> frozenset[str]:
    output = git(["ls-tree", "-d", "--name-only", ref, f"{path}/"], cwd)
    return frozenset(Path(line).name for line in output.splitlines() if line.strip())


def read_tree(ref: str, cwd: Path) -> ChangeTree:
    try:
        git(["rev-parse", "--verify", "--quiet", f"{ref}^{{commit}}"], cwd)
    except GitError as error:
        raise GitError(f"ref `{ref}` não existe ou não é um commit ({error})") from error
    entries = list_dirs(ref, CHANGES_DIR, cwd)
    active = frozenset(e for e in entries if e != ARCHIVE)
    archive_dirs = list_dirs(ref, f"{CHANGES_DIR}/{ARCHIVE}", cwd) if ARCHIVE in entries else frozenset()
    return ChangeTree(active=active, archive_dirs=archive_dirs)


def archive_hint(change: str) -> str:
    return (
        f"arquive com `openspec archive {change}` numa branch `chore/archive-{change}` "
        "(revisao-e-integracao.md, seção 8)"
    )


def check_lifecycle(base: ChangeTree, head: ChangeTree, base_branch: str) -> Result:
    """Regra de ciclo de vida para um PR de ``base`` para ``head``."""
    result = Result()
    if base_branch != MAIN_BRANCH:
        result.notes.append(
            f"Base `{base_branch}`: a regra de ciclo de vida só se aplica a PR para `{MAIN_BRANCH}`."
        )
        return result

    new_archives = {archived_name(d) for d in head.archive_dirs - base.archive_dirs}
    left = base.active - head.active
    archived_in_pr = sorted(left & new_archives)
    vanished = sorted(left - new_archives)
    pending = sorted(base.active & head.active)

    for change in vanished:
        result.errors.append(
            f"Change `{change}` saiu de `{CHANGES_DIR}/` sem pasta correspondente em "
            f"`{CHANGES_DIR}/{ARCHIVE}/`: remoção sem arquivamento."
        )
    for change in archived_in_pr:
        result.notes.append(f"Change `{change}` arquivada por este PR.")
    for change in pending:
        message = (
            f"Change `{change}` já estava na `{MAIN_BRANCH}` fora de `{ARCHIVE}/` e continua "
            f"fora neste PR: {archive_hint(change)}."
        )
        if archived_in_pr:
            result.warnings.append(message)
        else:
            result.errors.append(message)
    for change in sorted(head.active - base.active):
        result.notes.append(f"Change `{change}` nova em relação à base (PR `[SPEC]`).")
    return result


def check_without_base(head: ChangeTree) -> Result:
    """Push ou execução local sem base de PR: só avisa sobre changes ativas."""
    result = Result()
    for change in sorted(head.active):
        result.warnings.append(
            f"Change `{change}` está fora de `{ARCHIVE}/`. Se já foi entregue na "
            f"`{MAIN_BRANCH}`, {archive_hint(change)}."
        )
    return result


def report(result: Result, stream=None) -> None:
    stream = stream or sys.stdout
    annotate = os.environ.get("GITHUB_ACTIONS") == "true"
    for note in result.notes:
        print(note, file=stream)
    for warning in result.warnings:
        print(("::warning::" if annotate else "AVISO: ") + warning, file=stream)
    for error in result.errors:
        print(("::error::" if annotate else "ERRO: ") + error, file=stream)
    if result.errors:
        print(f"Ciclo de vida OpenSpec: {len(result.errors)} falha(s).", file=stream)
    else:
        print("Ciclo de vida OpenSpec: sem falhas.", file=stream)


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--base-ref", help="ref da base do PR, ex.: origin/main")
    parser.add_argument("--base-branch", help="nome da branch base do PR, ex.: main")
    parser.add_argument("--head-ref", default="HEAD", help="ref do head (padrão: HEAD)")
    parser.add_argument("-C", "--repo", default=".", help="diretório do repositório")
    args = parser.parse_args(argv)
    if bool(args.base_ref) != bool(args.base_branch):
        parser.error("--base-ref e --base-branch devem ser informados juntos")
    return args


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    repo = Path(args.repo)
    try:
        head = read_tree(args.head_ref, repo)
        if args.base_ref:
            base = read_tree(args.base_ref, repo)
            result = check_lifecycle(base, head, args.base_branch)
        else:
            result = check_without_base(head)
    except GitError as error:
        print(f"ERRO: {error}", file=sys.stderr)
        return 2
    report(result)
    return 1 if result.errors else 0


if __name__ == "__main__":
    sys.exit(main())
