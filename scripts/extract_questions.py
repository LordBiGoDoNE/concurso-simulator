#!/usr/bin/env python3
"""Extrai candidatos locais; saída JSON em stdout, nunca modifica aulas ou aprova."""
import argparse
import csv
import hashlib
import io
import json
import posixpath
import re
import sys
import uuid
from dataclasses import dataclass, field
from html.parser import HTMLParser
from urllib.parse import unquote, urlsplit
import xml.etree.ElementTree as ET

from question_package import CATALOG, FORMAT, ROLES, fingerprint, read_local, safe_url


@dataclass
class Node:
    tag: str
    attrs: dict = field(default_factory=dict)
    children: list = field(default_factory=list)

    def nodes(self):
        yield self
        for child in self.children:
            if isinstance(child, Node): yield from child.nodes()

    def text(self):
        return "".join(child.text() if isinstance(child, Node) else child for child in self.children)


class Document(HTMLParser):
    VOID = {"img", "br", "hr", "meta", "link", "input", "source", "wbr"}
    def __init__(self, html):
        super().__init__(convert_charrefs=True)
        self.root = Node("document"); self.stack = [self.root]
        self.feed(html); self.close()
        if len(self.stack) != 1: raise ValueError("HTML incompleto")

    def handle_starttag(self, tag, attrs):
        if len(dict(attrs)) != len(attrs): raise ValueError("Atributo HTML duplicado")
        node = Node(tag, dict(attrs)); self.stack[-1].children.append(node)
        if tag not in self.VOID: self.stack.append(node)

    def handle_endtag(self, tag):
        if tag in self.VOID: return
        if len(self.stack) < 2 or self.stack[-1].tag != tag:
            raise ValueError("HTML malformado; conversão incompleta")
        self.stack.pop()

    def handle_startendtag(self, tag, attrs):
        self.handle_starttag(tag, attrs)
        if tag not in self.VOID: self.handle_endtag(tag)

    def handle_data(self, data): self.stack[-1].children.append(data)

    def questions(self):
        result = {}
        for node in self.root.nodes():
            if (set(node.attrs.get("class", "").split()) & {"q", "question"}
                    or re.fullmatch(r"q[1-9][0-9]*", node.attrs.get("id", ""))):
                ident = node.attrs.get("id", "")
                if node.tag not in {"div", "section"} or not re.fullmatch(r"q[1-9][0-9]*", ident) or ident in result:
                    raise ValueError("ID de questão inválido/repetido no HTML")
                result[ident] = node
        return result


SVG_NAMESPACE = "http://www.w3.org/2000/svg"
SVG_NUMBER = r"[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]+)?"
SVG_NUMBER_LIST = SVG_NUMBER + r"(?:[ ,\t\r\n]+" + SVG_NUMBER + r")*"
SVG_IDENTIFIER = r"[A-Za-z_][A-Za-z0-9_.-]*"
SVG_TRANSFORM = r"(?:matrix|translate|scale|rotate|skewX|skewY)\(" + SVG_NUMBER_LIST + r"\)"
# Subconjunto sem CSS arbitrário, referências, escapes ou atributos desconhecidos.
SVG_ATTRIBUTES = {name: SVG_NUMBER + r"(?:px|%)?" for name in
                  "x y dx dy x1 y1 x2 y2 cx cy r rx ry width height refX refY markerWidth markerHeight font-size stroke-width stroke-dashoffset".split()}
SVG_ATTRIBUTES.update({
    "viewBox": SVG_NUMBER + (r"[ ,\t\r\n]+" + SVG_NUMBER) * 3,
    "points": SVG_NUMBER_LIST, "d": r"[MmZzLlHhVvCcSsQqTtAa0-9eE+., \t\r\n-]+",
    "transform": SVG_TRANSFORM + r"(?:[ ,\t\r\n]*" + SVG_TRANSFORM + r")*",
    "id": SVG_IDENTIFIER, "aria-labelledby": SVG_IDENTIFIER + r"(?:[ \t]+" + SVG_IDENTIFIER + r")*",
    "role": r"img", "fill": r"(?:#[0-9A-Fa-f]{3}|#[0-9A-Fa-f]{4}|#[0-9A-Fa-f]{6}|#[0-9A-Fa-f]{8}|(?i:none|currentColor|transparent|black|white|gray|grey|red|green|blue|yellow|orange|purple|pink|brown|cyan|magenta))",
    "opacity": SVG_NUMBER, "fill-opacity": SVG_NUMBER, "stroke-opacity": SVG_NUMBER,
    "stroke-dasharray": r"(?:none|" + SVG_NUMBER_LIST + r")",
    "stroke-linecap": r"(?:butt|round|square)", "stroke-linejoin": r"(?:miter|round|bevel)",
    "fill-rule": r"(?:nonzero|evenodd)", "font-family": r"[A-Za-z][A-Za-z0-9 ,_-]*",
    "font-weight": r"(?:normal|bold|bolder|lighter|[1-9]00)", "font-style": r"(?:normal|italic|oblique)",
    "text-anchor": r"(?:start|middle|end)", "dominant-baseline": r"(?:auto|middle|central|hanging|alphabetic)",
    "orient": r"(?:auto|auto-start-reverse|" + SVG_NUMBER + r"(?:deg)?)"
})
SVG_ATTRIBUTES["stroke"] = SVG_ATTRIBUTES["fill"]


class InertSvgTreeBuilder(ET.TreeBuilder):
    # Callbacks estruturais também veem PI fora da raiz e XML codificado em UTF-16.
    def pi(self, target, text): raise ValueError("SVG com instrução de processamento")
    def doctype(self, name, public_id, system_id): raise ValueError("SVG com entidades/DOCTYPE")
    def start_ns(self, prefix, uri):
        if uri != SVG_NAMESPACE: raise ValueError("Namespace SVG não permitido")


def image_resource(node, root, source):
    src = node.attrs.get("src", ""); parsed = urlsplit(src)
    if parsed.scheme or parsed.netloc or parsed.query or parsed.fragment:
        raise ValueError("Imagem deve ser recurso local simples")
    path = posixpath.normpath(posixpath.join(posixpath.dirname(source), unquote(src)))
    if not path.startswith("assets/"): raise ValueError("Recurso fora de assets")
    data = read_local(root, path); extension = path.rsplit(".", 1)[-1].lower()
    kinds = {"png": "image/png", "jpg": "image/jpeg", "jpeg": "image/jpeg", "svg": "image/svg+xml"}
    if extension not in kinds: raise ValueError("Tipo de imagem não permitido")
    if extension == "png" and not data.startswith(b"\x89PNG\r\n\x1a\n"): raise ValueError("PNG inválido")
    if extension in {"jpg", "jpeg"} and not data.startswith(b"\xff\xd8\xff"): raise ValueError("JPEG inválido")
    if extension == "svg":
        svg = ET.fromstring(data, parser=ET.XMLParser(target=InertSvgTreeBuilder()))
        if svg.tag != "{" + SVG_NAMESPACE + "}svg": raise ValueError("Raiz SVG inválida")
        allowed = {"{" + SVG_NAMESPACE + "}" + tag for tag in
                   {"svg", "g", "path", "rect", "circle", "ellipse", "line", "polyline", "polygon", "text", "tspan", "title", "desc", "defs", "marker"}}
        for element in svg.iter():
            if element.tag not in allowed: raise ValueError("SVG ativo/não permitido")
            for attr, value in element.attrib.items():
                pattern = SVG_ATTRIBUTES.get(attr)
                if pattern is None or not re.fullmatch(pattern, value.strip(" \t\r\n"), flags=re.ASCII):
                    raise ValueError("Atributo/valor SVG não permitido")
    alt = node.attrs.get("alt", "").strip()
    if not alt: raise ValueError("Imagem sem descrição acessível")
    return {"path": path, "mediaType": kinds[extension], "sha256": hashlib.sha256(data).hexdigest(), "description": alt}


def accessible(nodes, root, source, public_content=True):
    pieces, resources = [], []
    allowed = {"strong", "b", "em", "i", "p", "br", "ol", "ul", "li", "span", "sub", "sup", "code", "details", "summary", "div",
               "math", "mrow", "mi", "mn", "mo", "mfrac", "msup", "msub", "msqrt"}
    def visit(node):
        if isinstance(node, str): pieces.append(node); return
        if node.tag not in allowed | {"img"}: raise ValueError("Markup não permitido no bloco selecionado")
        for attr, value in node.attrs.items():
            if attr not in {"class", "aria-label", "role", "type", "src", "alt", "loading", "xmlns", "display"}:
                raise ValueError("Atributo não permitido no bloco selecionado")
            if attr == "xmlns" and (node.tag != "math" or value != "http://www.w3.org/1998/Math/MathML"):
                raise ValueError("Namespace de fórmula não permitido")
            if public_content and re.search(r"correct|answer|gabarito|corret[ao]|resolu[cç][aã]o", value or "", re.I):
                raise ValueError("Indicador corretivo no conteúdo projetável")
        if node.tag == "img":
            resource = image_resource(node, root, source); resources.append(resource)
            pieces.append(" [Imagem: " + resource["description"] + "] "); return
        if node.tag in {"mfrac", "msup", "msub"}:
            children = [c for c in node.children if isinstance(c, Node)]
            if len(children) != 2: raise ValueError("Fórmula incompleta")
            pieces.append("("); visit(children[0]); pieces.append(")" + {"mfrac": "/", "msup": "^", "msub": "_"}[node.tag] + "(")
            visit(children[1]); pieces.append(")"); return
        if node.tag in {"sup", "sub"}:
            if not node.children or not node.text().strip(): raise ValueError("Expoente/índice incompleto")
            pieces.append("^(" if node.tag == "sup" else "_(")
            for child in node.children: visit(child)
            pieces.append(")"); return
        if node.tag == "msqrt": pieces.append("raiz quadrada de (")
        if node.tag in {"p", "div", "details", "summary", "br", "li"}: pieces.append("\n")
        if node.tag == "li": pieces.append("• ")
        if node.tag in {"strong", "b"}: pieces.append("[destaque: ")
        if node.tag in {"em", "i"}: pieces.append("[ênfase: ")
        for child in node.children: visit(child)
        if node.tag in {"strong", "b", "em", "i"}: pieces.append("]")
        if node.tag in {"p", "div", "li", "summary"}: pieces.append("\n")
        if node.tag == "msqrt": pieces.append(")")
    for node in nodes: visit(node)
    text = "\n".join(re.sub(r"[ \t]+", " ", line).strip() for line in "".join(pieces).splitlines()).strip()
    if not text or len(text) > 100_000: raise ValueError("Texto acessível ausente/acima do limite")
    return {"text": text, "resources": resources}


def known_source_block(node, module, row):
    if not row.get("Tipo"): return False
    reference = Node("p", {}, ["Base para estudo e conferência: ", Node("a", {"href": "#referencias"},
                     ["Referências temáticas deste módulo"]), "."])
    if row.get("Fundamento_legal"):
        ref = module["referencias"][0]
        reference = Node("p", {}, ["Base para estudo e conferência: ", Node("a",
                         {"href": ref["url"], "target": "_blank", "rel": "noopener"}, [ref["titulo"]]),
                         " · " + row["Fundamento_legal"] + "."])
    return node == Node("aside", {"class": "callout callout--source"}, [
        Node("span", {"class": "callout-label"}, ["Fonte da questão"]),
        Node("p", {}, [Node("strong", {}, [row["Tipo"]]), " · " + row["ID"] + "."]),
        Node("p", {}, ["Origem: material aprofundado do projeto [Gaby], mantido na V3."]),
        reference
    ])


def known_return_link(node, module, number):
    if node.tag == "p" and node.attrs == {"class": "small"} and len(node.children) == 1:
        node = node.children[0]
    if not isinstance(node, Node) or node.tag != "a" or set(node.attrs) != {"href"}: return False
    target = posixpath.basename(module["conteudo"]) + f"#q{number}"
    return (unquote(node.attrs["href"]) == target and node.children in
            [[f"Rever o enunciado da questão {number}"], [f"Rever o enunciado {number}"]])


def candidate(root, module, row, content, answer, content_hash, answer_hash):
    key = row["ID"]; number = int(row["Questao"])
    if key != f"{module['id']}-Q{number:02}" or row["Arquivo"] != module["conteudo"]:
        raise ValueError("Join CSV/manifesto/ID inconsistente")
    q = content[f"q{number}"]; a = answer[f"q{number}"]
    if any(set(n.attrs) - {"id", "class"} for n in (q, a)):
        raise ValueError("Atributo inseguro no contêiner da questão")
    options = [n for n in q.children if isinstance(n, Node) and n.tag == "ol" and n.attrs.get("type") == "A"]
    if len(options) != 1: raise ValueError("Bloco de alternativas ausente/ambíguo")
    if set(options[0].attrs) != {"type"}: raise ValueError("Atributo não permitido nas alternativas")
    choices = [n for n in options[0].children if isinstance(n, Node)]
    if not 2 <= len(choices) <= 26 or any(n.tag != "li" for n in choices): raise ValueError("Alternativas incompletas")
    headers = [n for n in a.children if isinstance(n, Node) and n.tag in {"h2", "h3"}]
    match = re.fullmatch(rf"Questão {number} — alternativa ([A-Z])", headers[0].text().strip()) if len(headers) == 1 else None
    if not match or ord(match[1]) - ord("A") >= len(choices): raise ValueError("Gabarito sem join inequívoco")
    # Excluir só estruturas auxiliares conhecidas, nunca qualquer aside/classe small.
    labels = [Node("strong", {}, [f"Questão {number}."]), Node("h3", {}, [f"Questão {number}"])]
    statement = [n for n in q.children if n is not options[0] and not
                 (isinstance(n, Node) and (n in labels or known_source_block(n, module, row)))]
    resolution = [n for n in a.children if n is not headers[0] and not
                  (isinstance(n, Node) and (known_source_block(n, module, row) or known_return_link(n, module, number)))]
    alternatives = [{"id": str(uuid.uuid5(uuid.NAMESPACE_URL, f"concurso-simulator:legacy-v3:{key}:option:{i}")),
                     "content": accessible([Node("span", n.attrs, n.children)], root, module["conteudo"])} for i, n in enumerate(choices)]
    role_ids = [ROLES[0]] if module["id"].startswith("A") else [ROLES[1]] if module["id"].startswith("S") else list(ROLES)
    references = [{"name": r["titulo"], "url": safe_url(r["url"]), "legalBasis": None} for r in module["referencias"]]
    if not references: raise ValueError("Referências ausentes")
    body = {"statement": accessible(statement, root, module["conteudo"]), "alternatives": alternatives,
            "correctAlternativeId": alternatives[ord(match[1]) - ord("A")]["id"],
            "resolution": accessible(resolution, root, module["gabarito"], public_content=False),
            "classification": {"disciplineId": CATALOG[module["id"]], "topicId": module["id"], "roleIds": role_ids},
            "references": references,
            "provenance": {"author": row["Origem"], "contentPath": module["conteudo"], "answerPath": module["gabarito"],
                           "contentSha256": content_hash, "answerSha256": answer_hash}}
    if len({o["content"]["text"] for o in alternatives}) != len(alternatives): raise ValueError("Alternativas repetidas")
    return {"origin": {"namespace": "legacy-v3", "key": key}, "content": body,
            "fingerprint": fingerprint(body), "expectedLatestRevision": None}


def extract(root):
    manifest_data = read_local(root, "manifesto_v3.json"); csv_data = read_local(root, "catalogo_fontes_questoes.csv")
    modules = json.loads(manifest_data)["modulos"]; rows = list(csv.DictReader(io.StringIO(csv_data.decode("utf-8-sig")), delimiter=";"))
    if len({m["id"] for m in modules}) != len(modules) or set(m["id"] for m in modules) != set(CATALOG):
        raise ValueError("Manifesto não corresponde aos 51 módulos aprovados")
    if len(rows) != 510 or len({r["ID"] for r in rows}) != 510: raise ValueError("Inventário deve conter 510 IDs únicos")
    expected_keys = {f"{m['id']}-Q{i:02}" for m in modules for i in range(1, m["questoes"] + 1)}
    if {r["ID"] for r in rows} != expected_keys: raise ValueError("IDs CSV fora do inventário manifestado")
    sources = {"manifesto_v3.json": hashlib.sha256(manifest_data).hexdigest(), "catalogo_fontes_questoes.csv": hashlib.sha256(csv_data).hexdigest()}
    items, inventory, issues = [], [], []
    for module in modules:
        selected = [r for r in rows if r["ID"].startswith(module["id"] + "-Q")]
        failure = None
        try:
            if len(selected) != module["questoes"]: raise ValueError("Contagem do módulo divergente")
            documents = []
            for kind in ("conteudo", "gabarito"):
                data = read_local(root, module[kind]); sources[module[kind]] = hashlib.sha256(data).hexdigest()
                doc = Document(data.decode("utf-8")); documents.append(doc.questions())
            expected = {f"q{int(r['Questao'])}" for r in selected}
            if any(set(doc) != expected for doc in documents): raise ValueError("IDs HTML/CSV ausentes ou extras")
        except (ValueError, KeyError, OSError, UnicodeError) as error: failure = str(error)
        for row in selected:
            key = row["ID"]; record = {"key": key, "status": "candidate", "legalBasisPending": row["Fundamento_legal"],
                                        "editorialReviewRequired": True}
            try:
                if failure: raise ValueError(failure)
                item = candidate(root, module, row, *documents, sources[module["conteudo"]], sources[module["gabarito"]])
                items.append(item)
            except (ValueError, KeyError, OSError, UnicodeError, ET.ParseError) as error:
                record["status"] = "invalid"; issues.append({"key": key, "message": str(error)})
            inventory.append(record)
    # Similaridade de texto somente sinaliza; nenhum ID é fundido/excluído.
    groups = {}
    for item in items:
        signature = re.sub(r"\s+", " ", item["content"]["statement"]["text"]).casefold()
        groups.setdefault(signature, []).append(item["origin"]["key"])
    duplicates = [keys for keys in groups.values() if len(keys) > 1]
    return {"format": FORMAT, "catalog": [{"topicId": k, "disciplineId": v} for k, v in CATALOG.items()],
            "sourceFiles": [{"path": path, "sha256": sha} for path, sha in sorted(sources.items())],
            "inventory": inventory, "items": items, "issues": issues, "possibleDuplicates": duplicates}


def main():
    # O pacote e consumido como UTF-8 pelo launcher Java, inclusive via pipe no Windows.
    sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument("--root", default=".")
    args = parser.parse_args()
    try: result = extract(args.root)
    except (ValueError, KeyError, OSError, UnicodeError) as error:
        print("Extração recusada: " + str(error), file=sys.stderr); return 1
    print(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False))
    return 0


if __name__ == "__main__": raise SystemExit(main())
