"""Inventário e triagem incremental #173; ausência de observação permanece desconhecida."""
import argparse
import json
import re
import unicodedata
from collections import Counter
from datetime import datetime
from difflib import SequenceMatcher
from itertools import combinations
from pathlib import Path

from compile_lesson_practice import comparison_snapshot, compile_practice, normalized, direct_intent
from curate_questions import compile_curation, verify_manifest
from question_package import CATALOG, fingerprint
from question_expansion import EDITION, PURPOSES, CHECKS, require, compile_edition, write_json, read_json


def identity(item):
    return item['origin']['namespace'] + ':' + item['origin']['key']


def similarity_at_least(left, right, threshold):
    """Os limites superiores de difflib evitam custo sem perder qualquer sinal."""
    matcher = SequenceMatcher(None, left, right, autojunk=False)
    return (matcher.real_quick_ratio() >= threshold
            and matcher.quick_ratio() >= threshold
            and matcher.ratio() >= threshold)


def problem_text(value):
    """Espaçamento não é conteúdo; pontuação, caixa e operadores podem ser."""
    return ' '.join(unicodedata.normalize('NFC', value).split())


def numeric_problem_template(value):
    """Generaliza valores sem apagar a operação ou fundir expoentes ao número."""
    superscripts = str.maketrans('⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻', '0123456789+-')
    value = re.sub(r'[⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻]+',
                   lambda m: '^(' + m[0].translate(superscripts) + ')', value)
    value = unicodedata.normalize('NFKC', value)
    value = value.translate(str.maketrans({'−': '-', '×': '*', '÷': '/', '⁄': '/'}))
    value = re.sub(r'Microsoft\s*365|Windows\s*11', '', value, flags=re.I)
    value = re.sub(r'\b[A-Z]{1,3}[0-9]+\b', 'CELULA', value, flags=re.I)
    return re.sub(r'\d+(?:[.,]\d+)*', 'N', problem_text(value))


def baseline(root):
    """Sobrepõe revisões curadas por origem, sem contá-las novamente."""
    legacy = comparison_snapshot(root)
    original, curated, _, _, manifest = compile_curation(root)
    verify_manifest(manifest, Path(root) / 'editorial/question-bank/2026-10')
    practice, _, _ = compile_practice(root)
    extra = json.loads((Path(root) / 'editorial/parameterized-questions/2026-10/base-m04-q05-package.json').read_text(encoding='utf-8'))['items']
    require(len(extra) == 1 and extra[0]['origin'] == {'namespace': 'legacy-v3', 'key': 'M04-Q05'}
            and extra[0]['fingerprint'] == fingerprint(extra[0]['content']), 'Base M04-Q05 divergente')
    current = {identity(i): i for i in legacy['items']}
    current.update({identity(i): i for i in extra})
    current.update({identity(i): i for i in practice['items']})
    counts = Counter((i['content']['classification']['topicId'], 'SIMULADO') for i in curated + extra)
    counts.update((i['content']['classification']['topicId'], 'PRATICA_AULA') for i in practice['items'])
    return list(current.values()), counts, {'legacyInventory': len(original['inventory']),
        'legacyValidCandidates': len(original['items']), 'curatedRevisions': len(curated),
        'm04Base': len(extra), 'practiceCandidates': len(practice['items']),
        'legacyIssues': original['issues'], 'historicalCountsAreCurrentDatabase': False}


def audit_pairs(candidates, existing, decisions=()):
    by_id = {identity(i): i for i in existing}
    candidate_ids = set()
    for item in candidates:
        key = identity(item)
        require(key not in candidate_ids, 'Candidato repetido')
        candidate_ids.add(key)
        by_id[key] = item
    # Só pares tocados pela edição, não refazer adjudicação histórica de todo o legado.
    prepared = {}
    for key, item in by_id.items():
        statement = item['content']['statement']['text']
        # Enunciados genéricos/avisos acessíveis podem coincidir em problemas distintos.
        # Compare também alternativas (sem IDs/ordem/gabarito) e recursos do problema.
        options = sorted(problem_text(a['content']['text']) for a in item['content']['alternatives'])
        resources = sorted(r['sha256'] for block in [item['content']['statement']]
                           + [a['content'] for a in item['content']['alternatives']]
                           for r in block.get('resources', []))
        problem = statement + '\n' + '\n'.join(options + resources)
        normal = problem_text(problem)
        prepared[key] = (normal, numeric_problem_template(problem),
                         direct_intent(statement, item['content']['classification']['topicId']),
                         frozenset(normalized(problem).split()), tuple(resources),
                         bool(re.search(r'\d', problem)))
    decision_map = {}
    for d in decisions:
        require(set(d) == {'pair', 'fingerprints', 'decision', 'rationale', 'responsible', 'at'}, 'Decisão de par inválida')
        require(len(d['pair']) == 2 and d['pair'] == sorted(set(d['pair']))
                and len(d['fingerprints']) == 2 and d['decision'] in {'DISTINCT', 'EQUIVALENT'}, 'Par inválido')
        require(all(isinstance(d[k], str) and d[k].strip() for k in ('rationale', 'responsible', 'at')), 'Decisão sem justificativa')
        datetime.fromisoformat(d['at'])
        require(tuple(d['pair']) not in decision_map, 'Decisão repetida para par')
        decision_map[tuple(d['pair'])] = d
    pairs, used = [], set()
    for left, right in combinations(sorted(by_id), 2):
        if left not in candidate_ids and right not in candidate_ids:
            continue
        a, b = by_id[left], by_id[right]
        sa, na, ia, tokens_a, resources_a, numeric_a = prepared[left]
        sb, nb, ib, tokens_b, resources_b, numeric_b = prepared[right]
        same_topic = a['content']['classification']['topicId'] == b['content']['classification']['topicId']
        reasons = []
        if sa == sb:
            reasons.append('EXACT_PROBLEM')
        else:
            # Cruza disciplinas: rotina administrativa/escolar pode ser equivalente.
            overlap = len(tokens_a & tokens_b) / max(1, len(tokens_a | tokens_b))
            if (same_topic or overlap >= .35) and similarity_at_least(sa, sb, .75):
                reasons.append('TEXT_SIMILARITY')
        if same_topic and (na == nb or similarity_at_least(na, nb, .72)):
            reasons.append('NUMERIC_STRUCTURE')
        if same_topic and ia is not None and ia == ib:
            reasons.append('DIRECT_INTENT')
        if not reasons:
            continue
        pair = (left, right); used.add(pair)
        hashes = [a['fingerprint'], b['fingerprint']]
        d = decision_map.get(pair)
        state = 'PENDING' if d is None else ('CURRENT' if d['fingerprints'] == hashes else 'STALE')
        # Igualdade exata/estrutura numérica exata não aceita justificativa DISTINCT para cumprir cota.
        hard_duplicate = sa == sb or (same_topic and numeric_a and numeric_b
                                     and na == nb and resources_a == resources_b)
        accepted = state == 'CURRENT' and d['decision'] == 'DISTINCT' and not hard_duplicate
        pairs.append({'pair': list(pair), 'fingerprints': hashes, 'signals': reasons,
                      'decisionState': state, 'decision': d, 'blocksCandidate': not accepted})
    unused = sorted(set(decision_map) - used)
    return {'format': 'question-expansion-pairs/v1', 'pairs': pairs,
            'unusedDecisions': [list(p) for p in unused],
            'limits': 'Triagem textual/estrutural não decide equivalência semântica. IDs/cargos/variantes não comprovam diversidade.'}


def observed_counts(observation):
    """Envelope privado do relatório local existente + conteúdo por fingerprint; sem conectar DB."""
    if observation is None:
        return None
    require(set(observation) == {'destination', 'observedAt', 'complete', 'report', 'contents'}, 'Observação inválida')
    require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]{0,99}', observation['destination']), 'Destino lógico inválido')
    timestamp = datetime.fromisoformat(observation['observedAt'])
    require(timestamp.tzinfo is not None and observation['complete'] is True, 'Observação incompleta/sem fuso')
    questions = observation['report']['questions']
    require(isinstance(questions, list), 'Relatório inválido')
    counts = Counter(); imported = Counter(); known, origins = set(), set()
    for q in questions:
        qid, origin = q['questionId'], q['origin']
        oid = (origin['namespace'], origin['key'])
        require(qid not in known and oid not in origins, 'Base duplicada no relatório')
        known.add(qid); origins.add(oid)
        revisions = {r['id']: r for r in q['revisions']}
        require(len(revisions) == len(q['revisions']), 'Revisão repetida')
        purpose = 'PRATICA_AULA' if origin['namespace'] == 'lesson-practice-v1' else 'SIMULADO'
        eligible = q['eligibleRevisionId']
        # Não assumir que a última revisão é a elegível.
        if eligible is not None:
            require(eligible in revisions and revisions[eligible]['status'] == 'APPROVED', 'Ponteiro elegível inválido')
            revision = revisions[eligible]
            require(revision['fingerprint'] in observation['contents'], 'Conteúdo elegível ausente; contagem desconhecida')
            content = observation['contents'][revision['fingerprint']]
            require(fingerprint(content) == revision['fingerprint'], 'Conteúdo observado divergente')
            topic = content['classification']['topicId']
            require(topic in CATALOG and CATALOG[topic] == content['classification']['disciplineId'], 'Catálogo observado inválido')
            counts[(topic, purpose)] += 1
        # Importados só por assunto conhecido, não inferir classificação de origem/chave.
        topics = set()
        for revision in revisions.values():
            content = observation['contents'].get(revision['fingerprint'])
            if content is not None:
                require(fingerprint(content) == revision['fingerprint'], 'Conteúdo observado divergente')
                topics.add(content['classification']['topicId'])
        if len(topics) == 1:
            imported[(next(iter(topics)), purpose)] += 1
    return counts, imported


def coverage(root, cells, observation=None, reviews=(), pairs=None):
    _, historical, metadata = baseline(root)
    observed = observed_counts(observation)
    review_map = {}
    for r in reviews:
        require(set(r) == {'key', 'fingerprint', 'checks', 'responsible', 'at', 'limits'}, 'Revisão inválida')
        require(r['key'] not in review_map and set(r['checks']) == CHECKS
                and all(isinstance(r[k], str) and r[k].strip() for k in ('responsible', 'at', 'limits')), 'Checklist/revisão incompletos')
        datetime.fromisoformat(r['at']); review_map[r['key']] = r
    blocked = set()
    for pair in (pairs or {}).get('pairs', []):
        if pair['blocksCandidate']:
            blocked.update(pair['pair'])
    matrix = []
    for topic in sorted(CATALOG):
        for purpose in PURPOSES:
            cell = cells.get((topic, purpose), {'items': [], 'dossier': []})
            valid, originals = [], set()
            for item in cell['items']:
                key = item['origin']['key']; review = review_map.get(key)
                if review is not None and review['fingerprint'] == item['fingerprint'] and identity(item) not in blocked:
                    valid.append(item)
                    d = next(d for d in cell['dossier'] if d['key'] == key)
                    if d['originKind'] == 'ORIGINAL_DE_PROVA':
                        originals.add(d['officialIdentity'])
            eligible = None if observed is None else observed[0][(topic, purpose)]
            matrix.append({'topicId': topic, 'disciplineId': CATALOG[topic], 'purpose': purpose,
                'historicalReference': historical[(topic, purpose)], 'candidates': len(cell['items']),
                'reviewedCandidates': len(valid), 'verifiedCandidateOriginals': len(originals),
                'candidateOriginalDeficit': max(0, 5 - len(originals)),
                'importedKnownTopic': None if observed is None else observed[1][(topic, purpose)],
                'databaseEligible': eligible, 'databaseDeficit': None if eligible is None else max(0, 25 - eligible),
                'databaseVerifiedOriginals': None,
                'historicalReferenceDeficit': max(0, 25 - historical[(topic, purpose)]),
                'complete': False})
    return {'format': 'question-expansion-coverage/v1', 'destination': None if observation is None else observation['destination'],
            'observedAt': None if observation is None else observation['observedAt'],
            'metadata': metadata, 'cells': matrix,
            'limits': 'Histórico/candidatos não são carga atual. Originais elegíveis exigem vínculo ao dossiê conferido; total não comprova diversidade. Nenhuma célula é certificada por esta ferramenta.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', default='.')
    parser.add_argument('--edition-root', help='Diretório explícito do acervo externo')
    parser.add_argument('--output', required=True)
    parser.add_argument('--observation', help='Envelope privado, não acessa destino automaticamente')
    args = parser.parse_args()
    try:
        cells = compile_edition(args.root, args.edition_root)
        existing, _, _ = baseline(args.root)
        candidates = [i for cell in cells.values() for i in cell['items']]
        directory = Path(args.edition_root) if args.edition_root else Path(args.root) / EDITION
        decisions = read_json((directory / 'pair-decisions.json').read_text(encoding='utf-8'))
        reviews = read_json((directory / 'reviews.json').read_text(encoding='utf-8'))
        pairs = audit_pairs(candidates, existing, decisions)
        observation = None if not args.observation else read_json(Path(args.observation).read_text(encoding='utf-8'))
        report = coverage(args.root, cells, observation, reviews, pairs)
        output = Path(args.output)
        write_json(output / 'pairs.json', pairs); write_json(output / 'coverage.json', report)
        print(json.dumps({'cells': len(report['cells']), 'candidates': len(candidates),
                          'pairs': len(pairs['pairs']), 'databaseEligible': None if observation is None else sum(c['databaseEligible'] for c in report['cells'])}))
    except (ValueError, KeyError, TypeError, OSError) as error:
        parser.error(str(error))


if __name__ == '__main__':
    main()
