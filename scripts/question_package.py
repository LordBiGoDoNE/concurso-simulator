"""Contrato local question-package/v1; não aprova conteúdo nem acessa a rede."""
import hashlib
import re
import struct
from pathlib import Path, PurePosixPath
from urllib.parse import urlsplit, unquote

FORMAT = "question-package/v1"
CATALOG = {**{f"P{i:02}": "portugues" for i in range(1, 11)},
           **{f"M{i:02}": "matematica-raciocinio" for i in range(1, 13)},
           **{f"I{i:02}": "informatica" for i in range(1, 11)},
           **{f"A{i:02}": "conhecimentos-administrativos" for i in range(1, 9)},
           **{f"S{i:02}": "conhecimentos-escolares" for i in range(1, 12)}}
ROLES = ("assistente-administrativo", "secretario-de-escola")
SOURCE_ROOTS = {"01_Portugues", "02_Matematica_Logica", "03_Informatica",
                "04_Assistente_Administrativo", "05_Secretario_Escola", "assets"}


def read_local(root, relative, limit=16_000_000):
    """Caminho canônico relativo, componentes sem symlink; raiz local confiável."""
    path = PurePosixPath(relative)
    if not relative or "\\" in relative or path.is_absolute() or any(p in {".", ".."} for p in relative.split("/")):
        raise ValueError("Caminho local não permitido")
    if path.parts[0] not in SOURCE_ROOTS and relative not in {"manifesto_v3.json", "catalogo_fontes_questoes.csv"}:
        raise ValueError("Arquivo fora das raízes aprovadas")
    current = Path(root).resolve()
    for part in path.parts:
        current /= part
        if current.is_symlink():
            raise ValueError("Symlink não permitido")
    if not current.is_file() or current.stat().st_size > limit:
        raise ValueError("Arquivo ausente ou acima do limite")
    data = current.read_bytes()
    if len(data) > limit:
        raise ValueError("Arquivo acima do limite")
    return data


def safe_url(value):
    parsed = urlsplit(value)
    if (parsed.scheme not in {"http", "https"} or not parsed.hostname or parsed.username is not None
            or any(ord(c) < 32 for c in value) or "\\" in value or len(value) > 4000):
        raise ValueError("URL de estudo não permitida")
    if re.search(r"gabarito|resolu[cç][aã]o|answer(?:key)?", unquote(value), re.I):
        raise ValueError("URL de gabarito não pertence à projeção de questão")
    return value


def fingerprint(content):
    """Mesmo enquadramento binário UTF-8 v1 de QuestionContent, sem normalização."""
    chunks = []
    def integer(value): chunks.append(struct.pack(">i", value))
    def text(value):
        encoded = value.encode("utf-8", errors="strict")
        integer(len(encoded)); chunks.append(encoded)
    def rich(value):
        text(value["text"]); integer(len(value["resources"]))
        for r in value["resources"]:
            for field in ("path", "mediaType", "sha256", "description"): text(r[field])
    text("question-content/v1"); rich(content["statement"])
    integer(len(content["alternatives"]))
    for option in content["alternatives"]: text(option["id"]); rich(option["content"])
    text(content["correctAlternativeId"]); rich(content["resolution"])
    cls = content["classification"]; text(cls["disciplineId"]); text(cls["topicId"])
    roles = sorted(cls["roleIds"]); integer(len(roles))
    for role in roles: text(role)
    integer(len(content["references"]))
    for reference in content["references"]:
        text(reference["name"]); text(reference["url"])
        basis = reference["legalBasis"]; chunks.append(bytes([basis is not None]))
        if basis: text(basis["provision"]); text(basis["checkedOn"])
    for field in ("author", "contentPath", "answerPath", "contentSha256", "answerSha256"):
        text(content["provenance"][field])
    return hashlib.sha256(b"".join(chunks)).hexdigest()
