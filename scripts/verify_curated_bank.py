"""Confira a projeção HTTP das revisões reais aprovadas, sem executar escrita/editorial."""
import argparse
from collections import Counter
from datetime import datetime, timezone
import json
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import urlencode, urlsplit
from urllib.request import urlopen

from curate_questions import compile_curation, verify_manifest, DIRECTORY


def verify(root, report_file, api):
    endpoint = urlsplit(api)
    if (endpoint.scheme != "http" or endpoint.hostname not in {"127.0.0.1", "localhost", "::1"}
            or endpoint.username is not None or endpoint.query or endpoint.fragment
            or endpoint.path not in {"", "/"}):
        raise ValueError("Use origem HTTP local explícita, sem credenciais ou caminho")
    _, items, _, _, manifest = compile_curation(root)
    verify_manifest(manifest, Path(root) / DIRECTORY)
    content = {i["origin"]["key"]: i for i in items}
    report = json.loads(Path(report_file).read_text(encoding="utf-8"))
    entries = report["questions"]
    if len(entries) != 250 or {q["origin"]["key"] for q in entries} != set(content):
        raise ValueError("Relatório não corresponde às 250 origens selecionadas")
    by_id = {}
    for q in entries:
        item = content[q["origin"]["key"]]
        revision = q["revisions"][-1]
        if (len(q["revisions"]) != 1 or q["eligibleRevisionId"] != revision["id"]
                or revision["status"] != "APPROVED" or q["editorialSequence"] != 2
                or revision["fingerprint"] != item["fingerprint"] or q["questionId"] in by_id):
            raise ValueError("Relatório com revisão/base/estado divergentes")
        by_id[q["questionId"]] = (q, item)

    def read(path, expected_status=200):
        try:
            with urlopen(api.rstrip("/") + "/api/v1/question-bank/" + path, timeout=15) as response:
                status, value = response.status, json.load(response)
        except HTTPError as error:
            status, value = error.code, json.load(error)
        if status != expected_status:
            raise ValueError(f"Status HTTP divergente: {path}, {status}")
        return value

    def page(**filters):
        result = read("questions?" + urlencode({"size": 100, **filters}))
        if set(result) != {"items", "page", "size", "total"}:
            raise ValueError("Contrato de paginação divergente")
        for item in result["items"]:
            if set(item) != {"questionId", "revisionId", "classification", "statementStart"}:
                raise ValueError("Resumo com campo extra/privado")
        return result

    catalog = read("catalog")
    if (len(catalog["disciplines"]), len(catalog["topics"]), len(catalog["roles"])) != (5, 51, 2):
        raise ValueError("Catálogo inesperado")
    summaries = []
    for number in range(3):
        current = page(page=number)
        if current["total"] != 250:
            raise ValueError("Total público não é 250")
        summaries.extend(current["items"])
    ids = [s["questionId"] for s in summaries]
    if len(ids) != 250 or len(set(ids)) != 250 or set(ids) != set(by_id) or ids != sorted(ids):
        raise ValueError("Paginação perdeu, repetiu ou reordenou identidades")
    for summary in summaries:
        q, item = by_id[summary["questionId"]]
        expected = item["content"]
        detail = read("questions/" + q["questionId"])
        if set(detail) != {"questionId", "revisionId", "classification", "statement", "alternatives", "references"}:
            raise ValueError("Detalhe com campo extra/privado")
        classification = {**expected["classification"], "roleIds": sorted(expected["classification"]["roleIds"])}
        if (detail["revisionId"] != q["eligibleRevisionId"] or detail["questionId"] != q["questionId"]
                or summary["revisionId"] != q["eligibleRevisionId"]
                or summary["classification"] != classification or detail["classification"] != classification
                or detail["statement"] != expected["statement"] or detail["alternatives"] != expected["alternatives"]
                or detail["references"] != [{"name": r["name"], "url": r["url"]} for r in expected["references"]]):
            raise ValueError("Projeção pública divergente da revisão elegível")
    disciplines = Counter(i["content"]["classification"]["disciplineId"] for i in items)
    topics = Counter(i["content"]["classification"]["topicId"] for i in items)
    for discipline, total in disciplines.items():
        result = page(disciplineId=discipline)
        if result["total"] != total or len(result["items"]) != total:
            raise ValueError("Filtro de disciplina divergente")
    combinations = 0
    for topic in catalog["topics"]:
        for role in catalog["roles"]:
            filters = {"topicId": topic["id"], "disciplineId": topic["disciplineId"], "roleId": role["id"]}
            expected_ids = {qid for qid, (_, i) in by_id.items()
                            if i["content"]["classification"]["topicId"] == topic["id"]
                            and role["id"] in i["content"]["classification"]["roleIds"]}
            result = page(**filters)
            if result["total"] != len(expected_ids) or {i["questionId"] for i in result["items"]} != expected_ids:
                raise ValueError("Combinação AND não corresponde à elegibilidade")
            combinations += 1
    read("questions?topicId=unknown", 400)
    read("questions/00000000-0000-0000-0000-000000000000", 404)
    return {"verifiedAt": datetime.now(timezone.utc).isoformat(), "target": "isolated-local",
            "eligible": 250, "detailsChecked": 250, "pagesChecked": 3,
            "andFilterChecks": combinations, "disciplineTotals": dict(sorted(disciplines.items())),
            "topicTotals": dict(sorted(topics.items())), "privateFieldsAbsent": True,
            "correctiveContentAbsentFromProjection": True, "unknownFilterStatus": 400, "unknownIdStatus": 404}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".")
    parser.add_argument("--report", required=True)
    parser.add_argument("--api", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    result = verify(args.root, args.report, args.api)
    Path(args.output).write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in result.items() if k != "topicTotals"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
