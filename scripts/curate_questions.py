"""Compile intervenções editoriais explícitas. Sem rede, banco ou aprovação implícita."""
import argparse
import copy
import hashlib
import json
import re
from collections import Counter
from pathlib import Path

from extract_questions import extract
from question_package import CATALOG, fingerprint, safe_url

DIRECTORY = Path("editorial/question-bank/2026-10")
DISCIPLINES = ("portugues", "matematica", "informatica", "administrativo", "escolar")
CHECKLIST = ("CORRECTNESS", "DISTRACTORS", "CLARITY", "STEP_BY_STEP",
             "ACCESSIBILITY", "SOURCES", "CLASSIFICATION")
RESPONSIBLE = "Codex — revisão editorial assistida por IA, sem revisão humana independente"


def load_json(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True,
                                    separators=(",", ":")).encode("utf-8")).hexdigest()


def compile_curation(root, directory=None):
    root = Path(root).resolve()
    directory = Path(directory) if directory else root / DIRECTORY
    original = extract(root)
    originals = {item["origin"]["key"]: item for item in original["items"]}
    sources = load_json(directory / "sources.json")
    items, records, batches, seen = [], [], [], set()
    for name in DISCIPLINES:
        path = directory / (name + ".jsonl")
        rows = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]
        if len(rows) != 50:
            raise ValueError(f"{name}: são necessárias 50 intervenções explícitas")
        section = []
        for row in rows:
            if set(row) - {"key", "resolution", "statement", "alternatives", "note"}:
                raise ValueError("Campo editorial desconhecido")
            key = row["key"]
            if key in seen or key not in originals:
                raise ValueError(f"Origem repetida ou sem candidato válido: {key}")
            seen.add(key)
            item = copy.deepcopy(originals[key])
            content = item["content"]
            topic = content["classification"]["topicId"]
            expected_prefix = {"portugues": "P", "matematica": "M", "informatica": "I",
                               "administrativo": "A", "escolar": "S"}[name]
            if not topic.startswith(expected_prefix):
                raise ValueError(f"Disciplina incorreta: {key}")
            if not row["resolution"].startswith("1. ") or "2. " not in row["resolution"]:
                raise ValueError(f"Resolução sem etapas explícitas: {key}")
            content["resolution"] = {"text": row["resolution"], "resources": []}
            if "statement" in row:
                if content["statement"]["resources"]:
                    raise ValueError("Revisar recursos antes de substituir enunciado")
                content["statement"]["text"] = row["statement"]
            for letter, text in row.get("alternatives", {}).items():
                if not re.fullmatch("[A-D]", letter):
                    raise ValueError("Alternativa fora do pacote revisado")
                option = content["alternatives"][ord(letter) - ord("A")]
                if option["content"]["resources"]:
                    raise ValueError("Revisar recursos antes de substituir alternativa")
                option["content"]["text"] = text
            values = [option["content"]["text"].strip().casefold() for option in content["alternatives"]]
            if len(values) != len(set(values)):
                raise ValueError(f"Alternativas repetidas: {key}")
            ids = sources["keySources"].get(key, sources["topicSources"][topic])
            content["references"] = []
            for source_id in ids:
                ref = copy.deepcopy(sources["references"][source_id])
                safe_url(ref["url"])
                provision = sources["provisions"].get(key, {}).get(source_id)
                ref["legalBasis"] = {"provision": provision, "checkedOn": sources["checkedOn"]} if provision else None
                content["references"].append(ref)
            item["fingerprint"] = fingerprint(content)
            section.append(item)
            answer = next(chr(65 + n) for n, o in enumerate(content["alternatives"])
                          if o["id"] == content["correctAlternativeId"])
            records.append({"key": key, "discipline": name, "topic": topic,
                            "fingerprint": item["fingerprint"], "originalFingerprint": originals[key]["fingerprint"],
                            "reviewedBy": RESPONSIBLE, "reviewedOn": sources["checkedOn"],
                            "answer": answer, "checklist": list(CHECKLIST), "sources": ids,
                            "rationale": row["resolution"], "note": row.get("note"),
                            "status": "editorially-reviewed-not-imported", "databaseRevisionId": None})
        # Intercalar assuntos: cada lote de dez tem variedade e a união tem todos os assuntos.
        section.sort(key=lambda item: (int(item["origin"]["key"][-2:]), item["origin"]["key"]))
        for offset in range(0, 50, 10):
            batch_id = f"{name}-{offset // 10 + 1:02}"
            selected = section[offset:offset + 10]
            package = {**copy.deepcopy(original), "items": selected}
            task = f"6.{DISCIPLINES.index(name) * 5 + offset // 10 + 1}"
            batches.append({"id": batch_id, "task": task,
                            "keys": [i["origin"]["key"] for i in selected], "package": package})
        items.extend(section)
    if {i["content"]["classification"]["topicId"] for i in items} != set(CATALOG):
        raise ValueError("Cobertura dos 51 assuntos incompleta")
    for group in original["possibleDuplicates"]:
        if len(set(group) & seen) > 1:
            raise ValueError("Mais de um item do mesmo alerta textual selecionado")
    signatures = [re.sub(r"\s+", " ", i["content"]["statement"]["text"]).strip().casefold() for i in items]
    if len(signatures) != len(set(signatures)):
        raise ValueError("Enunciados repetidos no acervo selecionado")
    manifest = {"format": "question-curation/v1", "sourcesSha256": digest(sources),
                "sourceFiles": original["sourceFiles"],
                "items": [{"key": r["key"], "fingerprint": r["fingerprint"], "reviewSha256": digest(r)}
                          for r in sorted(records, key=lambda r: r["key"])]}
    return original, items, records, batches, manifest


def verify_manifest(manifest, directory):
    if load_json(Path(directory) / "manifest.json") != manifest:
        raise ValueError("Snapshot editorial mudou; revisar conteúdo e registrar novo manifest explicitamente")


def write_outputs(root, output, verify=True):
    original, items, records, batches, manifest = compile_curation(root)
    if verify:
        verify_manifest(manifest, Path(root) / DIRECTORY)
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    (output / "packages").mkdir(exist_ok=True)
    def write(path, value):
        path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    for batch in batches:
        write(output / "packages" / (batch["id"] + ".json"), batch["package"])
    write(output / "review-records.json", records)
    write(output / "all-selected.json", {**original, "items": items})
    coverage = {"inventory": len(original["inventory"]), "extractionCandidates": len(original["items"]),
                "selected": len(items), "editoriallyReviewed": len(records), "databaseEligible": None,
                "topics": dict(sorted(Counter(r["topic"] for r in records).items())),
                "disciplines": dict(Counter(r["discipline"] for r in records)),
                "batches": [{"id": b["id"], "task": b["task"], "keys": b["keys"]} for b in batches],
                "notSelected": sorted(set(r["key"] for r in original["inventory"]) - {r["key"] for r in records}),
                "sourceIssues": original["issues"], "sourceDuplicateAlerts": original["possibleDuplicates"],
                "limitations": ["Checklist é da revisão editorial assistida, não aprovação humana dos PRs.",
                                "Elegibilidade persistida só pode ser comprovada pelo relatório do banco."]}
    write(output / "coverage.json", coverage)
    lines = ["# Fichas de revisão editorial assistida", "", RESPONSIBLE,
             "", "Não comprova importação, elegibilidade persistida ou aprovação de PR.", ""]
    for batch in batches:
        lines += [f"## {batch['id']}", ""]
        for item in batch["package"]["items"]:
            key, content = item["origin"]["key"], item["content"]
            record = next(r for r in records if r["key"] == key)
            lines += [f"### {key}", "", content["statement"]["text"], ""]
            lines += [f"- {chr(65 + n)}) {o['content']['text']}" for n, o in enumerate(content["alternatives"])]
            lines += ["", f"Resposta: {record['answer']}. {content['resolution']['text']}", "",
                      f"Fingerprint: `{item['fingerprint']}`.", ""]
            lines += [f"- [{r['name']}]({r['url']})" + (f" — {r['legalBasis']['provision']}; consulta {r['legalBasis']['checkedOn']}" if r["legalBasis"] else "") for r in content["references"]]
            lines += [""]
    (output / "review.md").write_text("\n".join(lines), encoding="utf-8", newline="\n")
    return coverage


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    try:
        coverage = write_outputs(args.root, args.output)
    except (ValueError, KeyError, OSError) as error:
        parser.error(str(error))
    print(json.dumps({"selected": coverage["selected"], "disciplines": coverage["disciplines"],
                      "topics": len(coverage["topics"]), "databaseEligible": None}, ensure_ascii=False))


if __name__ == "__main__":
    main()
