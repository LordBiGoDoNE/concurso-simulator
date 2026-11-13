"""Preparação incremental #173: offline, sem DB, importação ou aprovação."""
import argparse
import base64
import hashlib
import json
import re
import struct
import uuid
from datetime import date
from pathlib import Path, PurePosixPath

from question_package import CATALOG, ROLES, fingerprint, safe_url, study_reference

EDITION = Path('editorial/question-bank-expansion/2026-11')
PURPOSES = {'SIMULADO': 'admin-question-v1', 'PRATICA_AULA': 'lesson-practice-v1'}
DISCIPLINES = {'portugues': 'P', 'matematica': 'M', 'informatica': 'I',
               'administrativo': 'A', 'escolar': 'S'}
CHECKS = {'CORRECTNESS', 'DISTRACTORS', 'CLARITY', 'STEP_BY_STEP',
          'ACCESSIBILITY', 'SOURCES', 'CLASSIFICATION'}


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True,
                      separators=(',', ':'), allow_nan=False).encode('utf-8')


def sha(data):
    return hashlib.sha256(data).hexdigest()


def read_json(data):
    def pairs(values):
        result = {}
        for key, value in values:
            require(key not in result, 'Campo JSON repetido')
            result[key] = value
        return result
    def invalid_constant(value):
        raise ValueError('Constante JSON inválida: ' + value)
    return json.loads(data, object_pairs_hook=pairs, parse_constant=invalid_constant)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def text(value, limit=100000):
    require(isinstance(value, str) and bool(value.strip()) and len(value) <= limit,
            'Texto ausente/extenso')
    require(not any(ord(c) < 32 and c not in '\n\r\t' for c in value), 'Controle em texto')
    value.encode('utf-8', errors='strict')
    return value


def local_bytes(directory, relative, expected, limit=16000000):
    require(isinstance(relative, str) and bool(relative), 'Caminho ausente')
    parts = PurePosixPath(relative).parts
    require(not PurePosixPath(relative).is_absolute() and '\\' not in relative
            and ':' not in relative and '%' not in relative
            and all(p not in {'', '.', '..'} for p in relative.split('/')), 'Caminho inválido')
    current = Path(directory).resolve()
    for part in parts:
        current /= part
        require(not current.is_symlink() and not current.is_junction(), 'Link não permitido')
    require(current.is_file() and current.stat().st_size <= limit, 'Fonte ausente/extensa')
    data = current.read_bytes()
    require(len(data) <= limit and sha(data) == expected, 'Fonte/hash divergente: ' + relative)
    return data


def load_edition(root, edition_root=None):
    directory = Path(edition_root).resolve() if edition_root is not None else Path(root).resolve() / EDITION
    manifest = read_json((directory / 'edition.json').read_text(encoding='utf-8'))
    require(set(manifest) == {'format', 'edition', 'sources', 'inputs'}, 'Manifesto inválido')
    require(manifest['format'] == 'question-expansion/v1' and manifest['edition'] == '2026-11', 'Edição inválida')
    spec = manifest['sources']
    refs = read_json(local_bytes(directory, spec['path'], spec['sha256']).decode('utf-8'))
    rows, seen = [], set()
    for entry in manifest['inputs']:
        purpose, discipline = entry['purpose'], entry['discipline']
        require(purpose in PURPOSES and discipline in DISCIPLINES, 'Finalidade/disciplina inválida')
        require(entry['path'] == f'{purpose.lower()}/{discipline}.jsonl', 'Arquivo fora da modalidade')
        require(entry['path'] not in seen, 'Arquivo repetido')
        seen.add(entry['path'])
        data = local_bytes(directory, entry['path'], entry['sha256'])
        for line in data.decode('utf-8').splitlines():
            if line.strip():
                row = read_json(line)
                require(row['topicId'].startswith(DISCIPLINES[discipline]), 'Assunto fora da disciplina')
                rows.append((purpose, row))
    expected = {f'{p.lower()}/{d}.jsonl' for p in PURPOSES for d in DISCIPLINES}
    require(seen == expected, 'Manifesto deve incluir os dez arquivos, inclusive vazios')
    return directory, refs, rows


def official_identity(exam, directory):
    fields = {'board', 'competition', 'year', 'role', 'booklet', 'number', 'identity',
              'status', 'definitive', 'proof', 'answer', 'reuse'}
    require(isinstance(exam, dict) and set(exam) == fields, 'Dossiê original incompleto')
    require(type(exam['year']) is int and 1900 <= exam['year'] <= date.today().year,
            'Ano de prova inválido')
    for field in ('board', 'competition', 'role', 'booklet', 'number', 'identity'):
        text(exam[field], 1000)
    require(exam['status'] == 'VALID' and exam['definitive'] is True,
            'Questão anulada/sem resposta definitiva')
    for field in ('proof', 'answer', 'reuse'):
        evidence = exam[field]
        require(set(evidence) == {'path', 'sha256', 'url', 'note'}, 'Evidência incompleta')
        text(evidence['note'], 4000)
        # Gabarito pertence ao dossiê privado, nunca às referências projetadas.
        from urllib.parse import urlsplit
        parsed = urlsplit(evidence['url'])
        require(parsed.scheme == 'https' and parsed.hostname and parsed.username is None
                and parsed.password is None and len(evidence['url']) <= 4000
                and '\\' not in evidence['url'] and not any(ord(c) < 32 for c in evidence['url']),
                'URL de evidência inválida')
        require(evidence['path'].startswith('dossier/'), 'Evidência fora do dossiê')
        local_bytes(directory, evidence['path'], evidence['sha256'])
    # Identidade editorial canônica deve reunir republicações/cadernos equivalentes.
    return exam['identity']


def compile_row(row, refs, purpose, directory):
    required = {'id', 'topicId', 'originKind', 'author', 'statement', 'alternatives',
                'correctOption', 'resolution', 'references'}
    require(required <= set(row) and not set(row) - required - {'exam', 'checkExpr'}, 'Campos editoriais inválidos')
    require(purpose in PURPOSES and row['topicId'] in CATALOG, 'Catálogo/finalidade inválidos')
    require(isinstance(row['id'], str) and re.fullmatch(r'[a-z0-9][a-z0-9_-]{0,39}', row['id']), 'ID estável inválido')
    require(row['originKind'] in {'AUTORAL', 'ADAPTADA', 'ORIGINAL_DE_PROVA'}, 'Origem desconhecida')
    text(row['author'], 500)
    text(row['statement']); text(row['resolution'])
    require(row['resolution'].startswith('1. ') and '2. ' in row['resolution'], 'Resolução sem etapas')
    require(isinstance(row['alternatives'], list) and 2 <= len(row['alternatives']) <= 10, 'Alternativas inválidas')
    options = sorted(row['alternatives'], key=lambda a: a['id'])
    ids, values = [], []
    for option in options:
        require(set(option) == {'id', 'text'} and isinstance(option['id'], str)
                and re.fullmatch(r'[a-z][a-z0-9_-]{0,39}', option['id']), 'ID de alternativa inválido')
        ids.append(option['id']); values.append(text(option['text']).strip().casefold())
    require(len(set(ids)) == len(ids) and len(set(values)) == len(values)
            and row['correctOption'] in ids, 'Resposta/alternativa repetida ou ausente')
    topic = row['topicId']
    marker = 's' if purpose == 'SIMULADO' else 'p'
    key = f'admin.2026-11.{topic}.{marker}.{row["id"]}'
    require(isinstance(row['references'], list) and 1 <= len(row['references']) <= 20
            and len(set(row['references'])) == len(row['references']), 'Referências inválidas')
    references, evidence_refs = [], {}
    for refid in sorted(row['references']):
        require(refid in refs, 'Fonte ausente: ' + refid)
        ref = refs[refid]
        require(set(ref) == {'name', 'url', 'checkedOn', 'legalBasis'}, 'Registro de fonte inválido')
        text(ref['name'], 1000); study_reference(ref['name'], ref['url']); date.fromisoformat(ref['checkedOn'])
        basis = ref['legalBasis']
        if basis is not None:
            require(set(basis) == {'provision', 'checkedOn'}, 'Dispositivo inválido')
            text(basis['provision'], 4000); date.fromisoformat(basis['checkedOn'])
        references.append({k: ref[k] for k in ('name', 'url', 'legalBasis')})
        evidence_refs[refid] = ref
    identity = None
    if row['originKind'] == 'ORIGINAL_DE_PROVA':
        identity = official_identity(row.get('exam'), directory)
    elif 'exam' in row:
        require(row['originKind'] == 'ADAPTADA', 'Autoral não pode reivindicar prova')
        official_identity(row['exam'], directory)
    numeric = None
    if 'checkExpr' in row:
        from compile_lesson_practice import calculate, answer_number
        numeric = str(calculate(row['checkExpr']))
        correct_text = next(a['text'] for a in options if a['id'] == row['correctOption'])
        require(calculate(row['checkExpr']) == answer_number(correct_text), 'Cálculo divergente')
    # Bytes normalizados por item: acrescentar outra linha não invalida estes hashes.
    normalized = {**row, 'alternatives': options, 'references': sorted(row['references'])}
    source = canonical({'editorial': normalized, 'studySources': evidence_refs})
    require(len(source) <= 1048576, 'Fonte normalizada excede 1 MiB')
    path = f'sources/{key}.json'
    rich = lambda value: {'text': value, 'resources': []}
    namespace = PURPOSES[purpose]
    alternatives = [{'id': str(uuid.uuid5(uuid.NAMESPACE_URL, f'{namespace}/{key}/option/{a["id"]}')),
                     'content': rich(a['text'])} for a in options]
    roleids = list(ROLES) if topic[0] in 'PMI' else [ROLES[0 if topic[0] == 'A' else 1]]
    content = {'statement': rich(row['statement']), 'alternatives': alternatives,
               'correctAlternativeId': alternatives[ids.index(row['correctOption'])]['id'],
               'resolution': rich(row['resolution']),
               'classification': {'topicId': topic, 'disciplineId': CATALOG[topic], 'roleIds': roleids},
               'references': references,
               'provenance': {'author': row['author'], 'contentPath': path, 'answerPath': path,
                              'contentSha256': sha(source), 'answerSha256': sha(source)}}
    item = {'origin': {'namespace': namespace, 'key': key}, 'content': content,
            'fingerprint': fingerprint(content), 'expectedLatestRevision': None}
    file = {'path': path, 'mediaType': 'application/json', 'sha256': sha(source),
            'base64': base64.b64encode(source).decode('ascii')}
    dossier = {'key': key, 'fingerprint': item['fingerprint'], 'originKind': row['originKind'],
               'officialIdentity': identity, 'numericCheck': numeric, 'review': None,
               'reuseAssessment': row.get('exam', {}).get('reuse', {}).get('note')}
    return item, file, dossier


def compile_edition(root, edition_root=None):
    directory, refs, rows = load_edition(root, edition_root)
    cells, seen, originals = {}, set(), set()
    for purpose, row in rows:
        item, file, dossier = compile_row(row, refs, purpose, directory)
        key = item['origin']['key']
        require(key not in seen, 'Identidade repetida na edição')
        seen.add(key)
        identity = dossier['officialIdentity']
        if identity:
            require(identity not in originals, 'Original reutilizada entre células/cadernos')
            originals.add(identity)
        cell = cells.setdefault((row['topicId'], purpose), {'items': [], 'files': [], 'dossier': []})
        for field, value in (('items', item), ('files', file), ('dossier', dossier)):
            cell[field].append(value)
    for cell in cells.values():
        for field in cell:
            cell[field].sort(key=lambda v: v.get('key', v.get('path', v.get('origin', {}).get('key'))))
    return cells


def digests(package, keys):
    """Digest binário da API #160, distinto de hashes JSON de artefatos."""
    chunks = []
    def integer(v): chunks.append(struct.pack('>i', v))
    def string(v):
        data = v.encode('utf-8'); integer(len(data)); chunks.append(data)
    string(package['format']); string(package['purpose']); integer(len(package['items']))
    for item in sorted(package['items'], key=lambda i: i['origin']['key']):
        string(item['origin']['namespace']); string(item['origin']['key']); string(item['fingerprint'])
        revision = item['expectedLatestRevision']; chunks.append(bytes([revision is not None]))
        if revision is not None:
            string(str(uuid.UUID(revision)))
    integer(len(package['files']))
    for file in sorted(package['files'], key=lambda f: f['path']):
        string(file['path']); string(file['mediaType']); string(file['sha256'])
        integer(len(base64.b64decode(file['base64'], validate=True)))
    package_digest = sha(b''.join(chunks)); chunks.clear()
    string('question-import-selection/v1'); integer(len(keys))
    for key in sorted(keys): string(key)
    return {'packageDigest': package_digest, 'selectionDigest': sha(b''.join(chunks))}


def request_for(cell, purpose, selection):
    require(purpose in PURPOSES and all(i['origin']['namespace'] == PURPOSES[purpose]
            for i in cell['items']), 'Namespace/finalidade divergentes')
    require(len({i['content']['classification']['topicId'] for i in cell['items']}) <= 1,
            'Pacote deve conter um único assunto')
    require(1 <= len(selection) <= 100, 'Seleção deve conter 1–100 itens')
    require(all(set(s) == {'key', 'fingerprint', 'expectedLatestRevision'} for s in selection), 'Seleção inválida')
    by_key = {i['origin']['key']: i for i in cell['items']}
    require(len({s['key'] for s in selection}) == len(selection), 'Seleção repetida')
    items = []
    for selected in sorted(selection, key=lambda s: s['key']):
        require(selected['key'] in by_key, 'Seleção fora do assunto/modalidade')
        item = by_key[selected['key']]
        require(selected['fingerprint'] == item['fingerprint'], 'Fingerprint da seleção desatualizado')
        revision = selected['expectedLatestRevision']
        require(revision is None or str(uuid.UUID(revision)) == revision, 'Base esperada inválida')
        items.append({**item, 'expectedLatestRevision': revision})
    paths = {i['content']['provenance']['contentPath'] for i in items}
    files = [f for f in cell['files'] if f['path'] in paths]
    require(len(files) <= 100 and sum(len(base64.b64decode(f['base64'])) for f in files) <= 8388608,
            'Arquivos excedem limites administrativos')
    package = {'format': 'administrative-question-package/v1', 'purpose': purpose, 'items': items, 'files': files}
    request = {'package': package, 'keys': [i['origin']['key'] for i in items]}
    require(len(canonical(request)) <= 16777216, 'Corpo excede 16 MiB')
    return request, digests(package, request['keys'])


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(json.dumps(value, ensure_ascii=False, indent=2, allow_nan=False).encode('utf-8') + b'\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', default='.')
    parser.add_argument('--edition-root', help='Diretório explícito do acervo externo; não é incluído em código/build')
    parser.add_argument('--output', required=True)
    parser.add_argument('--topic', choices=sorted(CATALOG))
    parser.add_argument('--purpose', choices=sorted(PURPOSES))
    parser.add_argument('--selection', help='Arquivo explícito; ausente gera somente plano de seleção')
    args = parser.parse_args()
    try:
        cells = compile_edition(args.root, args.edition_root)
        output = Path(args.output)
        if args.selection:
            require(args.topic is not None and args.purpose is not None, 'Informe assunto/finalidade')
            cell = cells.get((args.topic, args.purpose), {'items': [], 'files': [], 'dossier': []})
            selection = read_json(Path(args.selection).read_text(encoding='utf-8'))
            request, report = request_for(cell, args.purpose, selection)
            write_json(output / 'preview-request.json', request)
            write_json(output / 'digests.json', report)
            print(json.dumps(report))
        else:
            for (topic, purpose), cell in sorted(cells.items()):
                plan = [{'key': i['origin']['key'], 'fingerprint': i['fingerprint'],
                         'expectedLatestRevision': None} for i in cell['items']]
                write_json(output / purpose / f'{topic}-selection.json', plan)
                write_json(output / purpose / f'{topic}-dossier.json', cell['dossier'])
            print(json.dumps({'cells': len(cells), 'candidates': sum(len(c['items']) for c in cells.values()),
                              'imported': False, 'approved': False}))
    except (ValueError, KeyError, TypeError, OSError) as error:
        parser.error(str(error))


if __name__ == '__main__':
    main()
