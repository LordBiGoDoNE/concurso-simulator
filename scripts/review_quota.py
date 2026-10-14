#!/usr/bin/env python3
"""Valida observações mínimas da cota; não acessa credenciais ou precifica assinatura."""

import argparse
import json
import math
import sys
from datetime import datetime, timezone

MODELS = {"openai/gpt-6-astra#medium", "openai/gpt-6.1-sol#xhigh"}
WINDOWS = ("5h", "weekly")


def instant(value):
    if not isinstance(value, str):
        raise ValueError("Horário deve ser ISO-8601 com timezone")
    try:
        result = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        raise ValueError("Horário ISO-8601 inválido") from None
    if result.tzinfo is None:
        raise ValueError("Timezone obrigatório")
    return result.astimezone(timezone.utc)


def validate(data):
    required = {"version", "batch", "phase", "moment", "model", "session", "observed_at",
                "isolation_declared", "windows"}
    if not isinstance(data, dict) or set(data) != required:
        raise ValueError("Use somente os campos mínimos do template, nunca diagnósticos brutos")
    if type(data["version"]) is not int or data["version"] != 1 or data["model"] not in MODELS:
        raise ValueError("Versão/modelo não suportado; não substituir revisor silenciosamente")
    if data["phase"] not in {"initial", "revalidation", "coordinator"} or data["moment"] not in {"before", "after"}:
        raise ValueError("Fase/momento inválido")
    if type(data["isolation_declared"]) is not bool:
        raise ValueError("Isolamento deve ser declarado explicitamente")
    for key in ("batch", "session"):
        if not isinstance(data[key], str) or not data[key].strip() or len(data[key]) > 200:
            raise ValueError("Lote/sessão obrigatórios")
    observed = instant(data["observed_at"])
    if not isinstance(data["windows"], dict) or set(data["windows"]) != set(WINDOWS):
        raise ValueError("Declare as duas janelas, inclusive indisponibilidade com null")
    for window in data["windows"].values():
        if not isinstance(window, dict) or set(window) != {"remaining_percent", "reset_at"}:
            raise ValueError("Janela inválida")
        percent, reset = window["remaining_percent"], window["reset_at"]
        if percent is None and reset is None:
            continue
        if type(percent) not in (int, float) or not math.isfinite(percent) or not 0 <= percent <= 100:
            raise ValueError("Percentual restante deve estar entre 0 e 100")
        if instant(reset) <= observed:
            raise ValueError("Observação com reset vencido: recolher dado fresco ou marcar indisponível")
    return data


def compare(before, after):
    before, after = validate(before), validate(after)
    if before["moment"] != "before" or after["moment"] != "after":
        raise ValueError("Esperados snapshots antes/depois")
    for key in ("batch", "phase", "model", "session"):
        if before[key] != after[key]:
            raise ValueError("Snapshots de lote/fase/modelo/sessão diferentes")
    if instant(after["observed_at"]) <= instant(before["observed_at"]):
        raise ValueError("Fim deve ser posterior ao início")
    controlled = before["isolation_declared"] and after["isolation_declared"]
    rows = {}
    for name in WINDOWS:
        a, b = before["windows"][name], after["windows"][name]
        delta = None
        if a["remaining_percent"] is None or b["remaining_percent"] is None:
            status = "unavailable"
        elif instant(a["reset_at"]) != instant(b["reset_at"]) or instant(after["observed_at"]) >= instant(a["reset_at"]):
            status = "reset-changed"
        elif b["remaining_percent"] > a["remaining_percent"]:
            status = "replenished-or-inconsistent"
        else:
            delta = round(a["remaining_percent"] - b["remaining_percent"], 6)
            status = "not-resolved" if delta == 0 else ("controlled-account-observation" if controlled else "unattributable")
        rows[name] = {"status": status, "observed_decrease_pp": delta}
    return {"version": 1, "batch": before["batch"], "phase": before["phase"], "model": before["model"],
            "session": before["session"], "before_at": before["observed_at"], "after_at": after["observed_at"],
            "isolation_declared": controlled, "windows": rows,
            "scope": "Variação observada da conta, não custo financeiro ou atribuição exata ao modelo.",
            "limitations": ["Isolamento é declaração operacional, não verificação automática.",
                            "Arredondamento/atraso do provedor e atividade externa podem afetar a observação.",
                            "Delta zero não comprova consumo zero; janelas não devem ser somadas."]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("snapshot", help="Valida JSON mínimo no stdin; emite somente campos permitidos")
    comparison = commands.add_parser("compare", help="Compara dois snapshots locais")
    comparison.add_argument("before")
    comparison.add_argument("after")
    args = parser.parse_args()
    try:
        if args.command == "snapshot":
            result = validate(json.load(sys.stdin))
        else:
            with open(args.before, encoding="utf-8") as first, open(args.after, encoding="utf-8") as second:
                result = compare(json.load(first), json.load(second))
        print(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False))
    except (ValueError, TypeError, OSError) as error:
        print("Erro: " + str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
