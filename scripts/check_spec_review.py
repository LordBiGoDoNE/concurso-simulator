#!/usr/bin/env python3
"""Gate de evidência de revisão de spec; não atesta sua qualidade semântica."""

import argparse
import hashlib
import json
import re
import subprocess
from datetime import datetime


class ReviewError(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise ReviewError(message)


def git(*args):
    return subprocess.check_output(["git", *args], stderr=subprocess.PIPE)


def validate_sha(sha):
    require(isinstance(sha, str) and re.fullmatch(r"[0-9a-f]{40}", sha), "SHA inválido")
    git("cat-file", "-e", f"{sha}^{{commit}}")


def report_paths(spec):
    require(isinstance(spec, str) and re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", spec), "Spec inválida")
    root = f"openspec/changes/{spec}"
    return f"{root}/spec-review.json", f"{root}/spec-review.md"


def snapshot(base, head, spec):
    validate_sha(base)
    validate_sha(head)
    paths = report_paths(spec)
    diff = git("diff", "--binary", "--full-index", "--no-ext-diff", "--no-textconv",
               "--no-renames", "--diff-algorithm=myers", "--no-color", "--src-prefix=a/", "--dst-prefix=b/",
               f"{base}...{head}", "--", ".", *(f":(exclude){path}" for path in paths))
    return {"base_sha": base, "reviewed_head_sha": head,
            "diff_sha256": hashlib.sha256(diff).hexdigest()}


def text(value, field):
    require(isinstance(value, str) and bool(value.strip()), f"Campo vazio/inválido: {field}")


def validate_report(report, explanation, base, head, spec, pr):
    require(isinstance(report, dict) and type(report.get("version")) is int and report["version"] == 1,
            "Versão de relatório inválida")
    require(report.get("status") == "completed", "Revisão independente pendente/não executada")
    require(report.get("spec") == spec and type(report.get("pr")) is int and report["pr"] == pr,
            "Relatório não corresponde à spec/PR")
    require(report.get("base_sha") == base, "Base mudou: revalidar revisão")
    reviewed = report.get("reviewed_head_sha")
    validate_sha(reviewed)
    require(subprocess.run(["git", "merge-base", "--is-ancestor", reviewed, head],
                           capture_output=True).returncode == 0, "Head revisado não é ancestral do head atual")
    original = snapshot(base, reviewed, spec)
    current = snapshot(base, head, spec)
    require(report.get("diff_sha256") == original["diff_sha256"] == current["diff_sha256"],
            "Diff mudou ou digest incorreto: revisão/revalidação necessária")
    reviewer = report.get("reviewer")
    require(isinstance(reviewer, dict), "Revisor ausente")
    text(reviewer.get("model"), "reviewer.model")
    require(re.fullmatch(r"[^\s/]+/[^\s/#]+(?:#[^\s#]+)?", reviewer["model"]), "ID de modelo inválido")
    text(reviewer.get("session"), "reviewer.session")
    require(reviewer.get("independent_context") is True, "Contexto independente não confirmado")
    text(report.get("reviewed_at"), "reviewed_at")
    try:
        date = datetime.fromisoformat(report["reviewed_at"].replace("Z", "+00:00"))
    except ValueError as error:
        raise ReviewError("Data de revisão inválida") from error
    require(date.tzinfo is not None, "Data deve ter timezone")
    coverage = report.get("coverage")
    require(isinstance(coverage, dict), "Cobertura ausente")
    for area in ("requirements", "architecture", "integration", "security", "persistence", "tests", "maintenance"):
        text(coverage.get(area), f"coverage.{area}")
    limitations = report.get("limitations")
    require(isinstance(limitations, list) and bool(limitations), "Limitações devem ser explícitas")
    for item in limitations:
        text(item, "limitations")
    verification = report.get("verification")
    require(isinstance(verification, list) and bool(verification), "Verificações ausentes")
    for item in verification:
        require(isinstance(item, dict), "Verificação inválida")
        for field in ("command", "result", "executed_by"):
            text(item.get(field), f"verification.{field}")
    require(report.get("triage_complete") is True, "Triagem pendente")
    findings = report.get("findings")
    require(isinstance(findings, list), "Lista de achados ausente")
    ids = set()
    for item in findings:
        require(isinstance(item, dict), "Achado inválido")
        for field in ("id", "location", "evidence", "reference", "impact", "suggestion", "resolution"):
            text(item.get(field), f"finding.{field}")
        require(item["id"] not in ids, "IDs de achados duplicados")
        ids.add(item["id"])
        require(item.get("severity") in ("blocking", "recommended", "optional"), "Severidade inválida")
        require(item.get("disposition") in ("fixed", "rejected", "deferred"), "Achado não triado")
        require(not (item["severity"] == "blocking" and item["disposition"] == "deferred"),
                "Bloqueador não pode ser adiado")
    text(explanation, "spec-review.md")
    for heading in ("## Escopo e cobertura", "## Achados e triagem", "## Evidências e limitações", "## Revalidação"):
        require(heading in explanation, f"Relatório humano incompleto: {heading}")


def check(base_ref, head_ref, title, base, head, pr, head_repository, base_repository):
    if base_ref != "epic/application-reformulation":
        return "Não aplicável: não integra spec na épica geral"
    require(head_repository == base_repository and bool(head_repository),
            "Integração na épica geral deve usar uma branch interna do projeto")
    if head_ref == "main":
        require(not title.startswith("[SPEC]"), "Sincronização de main não é spec")
        return "Não aplicável: sincronização de main, sem autorização implícita de merge"
    require(head_ref.startswith("epic/"), "Integração na épica geral deve partir de epic/<spec>")
    spec = head_ref.removeprefix("epic/")
    paths = report_paths(spec)
    require(title.startswith("[SPEC] ") and bool(title[len("[SPEC] "):].strip()), "Título deve ser [SPEC] <Título>")
    validate_sha(base)
    validate_sha(head)
    # Ler evidência do commit exato, não do working tree ou merge sintético do CI.
    report = json.loads(git("show", f"{head}:{paths[0]}").decode())
    explanation = git("show", f"{head}:{paths[1]}").decode()
    validate_report(report, explanation, base, head, spec, pr)
    return f"OK: evidência de revisão atual para {spec}; aprovação de merge continua necessária"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    parser.add_argument("--spec")
    parser.add_argument("--snapshot", action="store_true")
    parser.add_argument("--base-ref")
    parser.add_argument("--head-ref")
    parser.add_argument("--title")
    parser.add_argument("--pr", type=int)
    parser.add_argument("--head-repository")
    parser.add_argument("--base-repository")
    args = parser.parse_args()
    try:
        if args.snapshot:
            print(json.dumps(snapshot(args.base, args.head, args.spec), indent=2))
        else:
            require(all(value is not None for value in (args.base_ref, args.head_ref, args.title, args.pr,
                                                       args.head_repository, args.base_repository)),
                    "Metadados de PR obrigatórios")
            print(check(args.base_ref, args.head_ref, args.title, args.base, args.head, args.pr,
                        args.head_repository, args.base_repository))
    except (ReviewError, subprocess.CalledProcessError, json.JSONDecodeError, UnicodeDecodeError) as error:
        print(f"BLOQUEADO: {error}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
