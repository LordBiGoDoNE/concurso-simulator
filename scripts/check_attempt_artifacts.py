#!/usr/bin/env python3
"""Inspeção local de artefatos: sem fixtures/payloads privados e entrada sem módulo corretivo."""
import argparse
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import zipfile

SENTINELS = tuple(text.encode("utf-8") for text in (
    "PRIVATE_BROWSER_RESOLUTION_SENTINEL", "NEW_BROWSER_RESOLUTION_SENTINEL",
    "PRIVATE_ATTEMPT_RESOLUTION_SENTINEL", "PRIVATE_CONTEXT_STATEMENT",
    "Questão sintética", "Fixture de navegador"))


class Entry(HTMLParser):
    def __init__(self):
        super().__init__()
        self.paths = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "script" and attrs.get("type") == "module" and "src" in attrs:
            self.paths.append(attrs["src"])


def check(jar, dist):
    jar, dist = Path(jar), Path(dist).resolve()
    classes = 0
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            if not name.startswith("BOOT-INF/classes/"):
                continue
            if re.search(r"(^|/)(\.env[^/]*|database-private\.json|curation)(/|$)", name):
                raise ValueError("Arquivo de ambiente/curadoria no JAR")
            if name.startswith("BOOT-INF/classes/br/com/concursosimulator/") and name.endswith(".class"):
                classes += 1
                if re.search(r"(Fixture|Fixtures|Tests)(\$[^/]*)?\.class$", name):
                    raise ValueError("Fixture/classe de teste no JAR")
            if any(marker in archive.read(name) for marker in SENTINELS):
                raise ValueError("Payload de fixture no JAR")
    if classes == 0:
        raise ValueError("JAR sem classes de aplicação")
    files = [p for p in dist.rglob("*") if p.is_file()]
    for path in files:
        if path.name.startswith(".env") or any(marker in path.read_bytes() for marker in SENTINELS):
            raise ValueError("Ambiente/payload de fixture no frontend")
    entry = Entry()
    entry.feed((dist / "index.html").read_text(encoding="utf-8"))
    if len(entry.paths) != 1:
        raise ValueError("Entrada JavaScript única não encontrada")
    initial = (dist / entry.paths[0].lstrip("/")).resolve()
    if not initial.is_relative_to(dist) or not initial.is_file():
        raise ValueError("Entrada fora do artefato")
    if b"correctAlternativeId" in initial.read_bytes():
        raise ValueError("Contrato corretivo carregado na entrada inicial")
    return {"status": "passed", "application_classes": classes, "frontend_files": len(files),
            "jar_sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
            "entry": initial.relative_to(dist).as_posix(),
            "limitations": ["Sentinelas/paths conhecidos; não prova ausência de todo segredo ou dica semântica.",
                            "Módulos tardios contêm código genérico do resultado, sem payload editorial."]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", required=True)
    parser.add_argument("--dist", required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(check(args.jar, args.dist), ensure_ascii=True))
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        parser.exit(1, f"Artefato recusado: {type(error).__name__}; confira paths/fixtures, sem publicar payload.\n")


if __name__ == "__main__":
    main()
