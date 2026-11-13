"""Conferência offline dos pacotes finais #173: cotas, identidades, fontes e inventário.

Lê somente os pacotes administrativos já exportados (``manifest.json`` + uma pasta por
finalidade/assunto). Não acessa banco, rede, importação ou aprovação. Com
``--edition-root`` também confere os SHA-256 das provas/gabaritos locais citados pelas
originais; com ``--reviews`` confere revisão individual com fingerprint atual.
Falhas encerram com código 1 e listam todos os problemas encontrados.
"""
import argparse
import base64
import hashlib
import json
from collections import Counter, defaultdict
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlsplit

from question_package import CATALOG, fingerprint, study_reference

PURPOSES = ('SIMULADO', 'PRATICA_AULA')
NAMESPACES = {'SIMULADO': 'admin-question-v1', 'PRATICA_AULA': 'lesson-practice-v1'}
MIN_ITEMS = 25
MIN_ORIGINALS = 5
DISCIPLINE_NAMES = {'portugues': 'Português', 'matematica-raciocinio': 'Matemática/Raciocínio',
                    'informatica': 'Informática', 'conhecimentos-administrativos': 'Conhecimentos Administrativos',
                    'conhecimentos-escolares': 'Conhecimentos Escolares'}


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def normalized_url(url):
    """Compara URLs sem esquema, maiúsculas no host, codificação percentual e barra final."""
    parts = urlsplit(url.strip())
    return ((parts.hostname or '').lower().removeprefix('www.'), unquote(parts.path).rstrip('/'), parts.query)


def load(path):
    return json.loads(Path(path).read_bytes())


def local_file(root, relative):
    """Resolve caminho relativo POSIX sob ``root`` recusando escapes."""
    pure = PurePosixPath(relative)
    if pure.is_absolute() or '..' in pure.parts or '\\' in relative or ':' in relative:
        return None
    path = Path(root, *pure.parts).resolve()
    return path if path.is_relative_to(Path(root).resolve()) and path.is_file() else None


def check(packages_root, edition_root=None, reviews_path=None):
    root = Path(packages_root)
    problems = []
    manifest = load(root / 'manifest.json')
    cells, originals, keys, prints, identities = [], [], Counter(), Counter(), defaultdict(list)
    listed = {(p['topicId'], p['purpose']) for p in manifest['packages']}
    expected = {(t, p) for t in CATALOG for p in PURPOSES}
    for missing in sorted(expected - listed):
        problems.append(f'Célula sem pacote: {missing[0]}/{missing[1]}')
    for extra in sorted(listed - expected):
        problems.append(f'Pacote fora do catálogo: {extra[0]}/{extra[1]}')
    reviews = None
    if reviews_path:
        reviews = {r['key']: r for r in load(reviews_path)}
    total_items = 0
    public_refs, exam_urls = [], set()
    for entry in manifest['packages']:
        topic, purpose = entry['topicId'], entry['purpose']
        directory = root / purpose / topic
        request_bytes = (directory / 'preview-request.json').read_bytes()
        where = f'{topic}/{purpose}'
        if sha256(request_bytes) != entry['requestSha256']:
            problems.append(f'{where}: SHA-256 da requisição diverge do manifesto')
        digests = load(directory / 'digests.json')
        if (digests['packageDigest'], digests['selectionDigest']) != (entry['packageDigest'], entry['selectionDigest']):
            problems.append(f'{where}: digests.json diverge do manifesto')
        request = json.loads(request_bytes)
        package, selected = request['package'], request['keys']
        dossier = {d['key']: d for d in load(directory / 'dossier.json')}
        selection = load(directory / 'selection.json')
        if package['purpose'] != purpose:
            problems.append(f'{where}: finalidade do pacote divergente')
        files = {f['path']: f for f in package['files']}
        items = {i['origin']['key']: i for i in package['items']}
        if sorted(selected) != sorted(items) or sorted(s['key'] for s in selection) != sorted(items):
            problems.append(f'{where}: seleção não corresponde aos itens do pacote')
        if len(items) != entry['items']:
            problems.append(f'{where}: quantidade diverge do manifesto')
        origin_count = 0
        for key, item in items.items():
            total_items += 1
            keys[key] += 1
            prints[item['fingerprint']] += 1
            if item['origin']['namespace'] != NAMESPACES[purpose]:
                problems.append(f'{key}: namespace incompatível com a finalidade')
            if not key.startswith(f'admin.2026-11.{topic}.'):
                problems.append(f'{key}: chave fora do assunto')
            if fingerprint(item['content']) != item['fingerprint']:
                problems.append(f'{key}: fingerprint não corresponde ao conteúdo')
            for reference in item['content']['references']:
                public_refs.append((key, reference['url']))
                try:
                    study_reference(reference['name'], reference['url'])
                except ValueError:
                    problems.append(f'{key}: referência pública aponta para prova/gabarito ({reference["url"]})')
            info = dossier.get(key)
            if info is None or info['fingerprint'] != item['fingerprint']:
                problems.append(f'{key}: dossiê ausente ou com fingerprint divergente')
                continue
            if reviews is not None:
                review = reviews.get(key)
                if review is None or review['fingerprint'] != item['fingerprint']:
                    problems.append(f'{key}: sem revisão individual com fingerprint atual')
            source_name = f'sources/{key}.json'
            source_file = files.get(source_name)
            if source_file is None:
                problems.append(f'{key}: arquivo de proveniência ausente')
                continue
            raw = base64.b64decode(source_file['base64'])
            if sha256(raw) != source_file['sha256']:
                problems.append(f'{key}: SHA-256 da proveniência divergente')
            editorial = json.loads(raw)['editorial']
            for part in ('proof', 'answer'):
                url = ((editorial.get('exam') or {}).get(part) or {}).get('url')
                if url:
                    exam_urls.add(normalized_url(url))
            if editorial['topicId'] != topic or editorial['originKind'] != info['originKind']:
                problems.append(f'{key}: assunto/origem da proveniência divergente')
            if info['originKind'] != 'ORIGINAL_DE_PROVA':
                continue
            origin_count += 1
            exam = editorial.get('exam') or {}
            identity = exam.get('identity')
            if not identity or identity != info['officialIdentity']:
                problems.append(f'{key}: identidade oficial ausente ou divergente')
            if exam.get('definitive') is not True or exam.get('status') != 'VALID':
                problems.append(f'{key}: gabarito não definitivo ou questão não válida')
            identities[identity].append(key)
            record = {'key': key, 'topicId': topic, 'purpose': purpose, 'identity': identity,
                      'board': exam.get('board'), 'competition': exam.get('competition'),
                      'role': exam.get('role'), 'year': exam.get('year'), 'number': exam.get('number'),
                      'proof': {k: exam.get('proof', {}).get(k) for k in ('url', 'sha256', 'path')},
                      'answer': {k: exam.get('answer', {}).get(k) for k in ('url', 'sha256', 'path')},
                      'localHashes': None}
            if edition_root:
                verified = True
                for part in ('proof', 'answer'):
                    meta = record[part]
                    path = local_file(edition_root, meta['path'] or '')
                    if path is None or sha256(path.read_bytes()) != meta['sha256']:
                        verified = False
                        problems.append(f'{key}: arquivo local de {part} ausente ou com SHA-256 divergente')
                record['localHashes'] = 'VERIFIED' if verified else 'FAILED'
            originals.append(record)
        if len(items) < MIN_ITEMS:
            problems.append(f'{where}: {len(items)} itens, mínimo {MIN_ITEMS}')
        if origin_count < MIN_ORIGINALS:
            problems.append(f'{where}: {origin_count} originais, mínimo {MIN_ORIGINALS}')
        cells.append({'topicId': topic, 'disciplineId': CATALOG.get(topic), 'purpose': purpose,
                      'items': len(items), 'originals': origin_count,
                      'authored': len(items) - origin_count,
                      'packageDigest': entry['packageDigest'], 'selectionDigest': entry['selectionDigest']})
    for key, url in public_refs:
        if normalized_url(url) in exam_urls:
            problems.append(f'{key}: referência pública coincide com prova/gabarito da proveniência ({url})')
    for key, count in keys.items():
        if count > 1:
            problems.append(f'Chave repetida entre pacotes: {key}')
    for value, count in prints.items():
        if count > 1:
            problems.append(f'Fingerprint repetido entre pacotes: {value}')
    for identity, used in identities.items():
        if len(used) > 1:
            problems.append(f'Original de prova usada mais de uma vez ({identity}): {", ".join(sorted(used))}')
    disciplines = {}
    for discipline in DISCIPLINE_NAMES:
        topics = sorted(t for t, d in CATALOG.items() if d == discipline)
        mine = [c for c in cells if c['disciplineId'] == discipline]
        summary = {'topics': len(topics)}
        for purpose in PURPOSES:
            chosen = [c for c in mine if c['purpose'] == purpose]
            summary[purpose] = {'items': sum(c['items'] for c in chosen),
                                'originals': sum(c['originals'] for c in chosen),
                                'minimumItems': MIN_ITEMS * len(topics)}
            if summary[purpose]['items'] < MIN_ITEMS * len(topics):
                problems.append(f'{discipline}/{purpose}: abaixo de {MIN_ITEMS * len(topics)} itens')
        summary['originals'] = sum(c['originals'] for c in mine)
        summary['minimumOriginals'] = MIN_ORIGINALS * 2 * len(topics)
        if summary['originals'] < summary['minimumOriginals']:
            problems.append(f'{discipline}: abaixo de {summary["minimumOriginals"]} originais')
        disciplines[discipline] = summary
    boards = Counter(o['board'] for o in originals)
    report = {
        'format': 'question-expansion-final-check/v1',
        'packagesManifestSha256': sha256((root / 'manifest.json').read_bytes()),
        'cells': len(cells), 'items': total_items,
        'byPurpose': {p: sum(c['items'] for c in cells if c['purpose'] == p) for p in PURPOSES},
        'originals': len(originals), 'distinctOfficialIdentities': len(identities),
        'boards': dict(sorted(boards.items())),
        'localHashesChecked': bool(edition_root), 'reviewsChecked': reviews is not None,
        'disciplines': disciplines, 'cellsDetail': sorted(cells, key=lambda c: (c['topicId'], c['purpose'])),
        'problems': problems,
        'limits': ('Conferência estrutural/quantitativa offline: não decide correção editorial, '
                   'equivalência semântica, licença de reutilização nem elegibilidade no banco.'),
    }
    inventory = {'format': 'question-expansion-originals-inventory/v1',
                 'originals': sorted(originals, key=lambda o: (o['topicId'], o['purpose'], o['key']))}
    return report, inventory


def markdown(report):
    lines = ['# Conferência dos pacotes finais — #173', '',
             f"{report['cells']} células, {report['items']} itens "
             f"({report['byPurpose']['SIMULADO']} simulado / {report['byPurpose']['PRATICA_AULA']} prática), "
             f"{report['originals']} originais de prova com {report['distinctOfficialIdentities']} identidades oficiais distintas. "
             f"Problemas: {len(report['problems'])}.", '',
             '| Disciplina | Assuntos | Simulado | Prática | Originais (mín.) |',
             '| --- | ---: | ---: | ---: | ---: |']
    for key, d in report['disciplines'].items():
        lines.append(f"| {DISCIPLINE_NAMES[key]} | {d['topics']} | {d['SIMULADO']['items']} | "
                     f"{d['PRATICA_AULA']['items']} | {d['originals']} ({d['minimumOriginals']}) |")
    lines += ['', '| Assunto | Finalidade | Itens | Originais | Autorais |', '| --- | --- | ---: | ---: | ---: |']
    for c in report['cellsDetail']:
        lines.append(f"| {c['topicId']} | {c['purpose']} | {c['items']} | {c['originals']} | {c['authored']} |")
    lines += ['', 'Bancas das originais: ' + ', '.join(f'{b} {n}' for b, n in report['boards'].items()) + '.',
              '', report['limits'], '']
    return '\n'.join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--packages', required=True)
    parser.add_argument('--edition-root', help='aggregate-wip local, para conferir PDFs das originais')
    parser.add_argument('--reviews', help='reviews.json com revisões individuais')
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    report, inventory = check(args.packages, args.edition_root, args.reviews)
    out = Path(args.output)
    out.mkdir(parents=True, exist_ok=True)
    (out / 'final-check.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (out / 'originals-inventory.json').write_text(json.dumps(inventory, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (out / 'final-check.md').write_text(markdown(report), encoding='utf-8')
    print(f"{report['cells']} células, {report['items']} itens, {report['originals']} originais; "
          f"problemas: {len(report['problems'])}")
    for problem in report['problems'][:50]:
        print(' -', problem)
    raise SystemExit(1 if report['problems'] else 0)


if __name__ == '__main__':
    main()
