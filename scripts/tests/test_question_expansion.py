"""Regressões #173 e emissão de fixtures sintéticas para o leitor Java real."""
import copy
import json
from pathlib import Path
import sys
import subprocess
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from question_expansion import (EDITION, PURPOSES, DISCIPLINES, CHECKS, canonical, sha,
    compile_row, compile_edition, request_for, write_json, read_json)
from audit_question_expansion import (audit_pairs, coverage, identity, observed_counts,
    baseline, similarity_at_least, numeric_problem_template)

ROOT = Path(__file__).resolve().parents[2]
REFS = {'synthetic': {'name': 'Fonte sintética de teste', 'url': 'https://example.test/study',
        'checkedOn': '2026-11-10', 'legalBasis': None}}


def row(number=1):
    return {'id': f'fixture-{number:03}', 'topicId': 'M01', 'originKind': 'AUTORAL',
        'author': 'Fixture sintética #173 — não é questão editorial',
        'statement': f'Fixture exclusiva {number}: calcule 2 + 2.',
        'alternatives': [{'id': 'four', 'text': '4'}, {'id': 'five', 'text': '5'}],
        'correctOption': 'four', 'resolution': '1. Somar 2 e 2. 2. A soma é 4.',
        'references': ['synthetic'], 'checkExpr': '2+2'}


def cell(rows, purpose='PRATICA_AULA', directory=ROOT):
    result = {'items': [], 'files': [], 'dossier': []}
    for r in rows:
        item, file, dossier = compile_row(r, REFS, purpose, directory)
        for field, value in (('items', item), ('files', file), ('dossier', dossier)):
            result[field].append(value)
    return result


def selection(c):
    return [{'key': i['origin']['key'], 'fingerprint': i['fingerprint'],
             'expectedLatestRevision': None} for i in c['items']]


def edition(directory, rows=()):
    directory = Path(directory) / EDITION
    write_json(directory / 'sources.json', REFS)
    inputs = []
    for purpose in PURPOSES:
        for discipline in DISCIPLINES:
            path = directory / purpose.lower() / f'{discipline}.jsonl'
            path.parent.mkdir(parents=True, exist_ok=True)
            data = b''.join(canonical(r) + b'\n' for r in rows) if (purpose, discipline) == ('PRATICA_AULA', 'matematica') else b''
            path.write_bytes(data)
            inputs.append({'path': path.relative_to(directory).as_posix(), 'sha256': sha(data),
                           'purpose': purpose, 'discipline': discipline})
    write_json(directory / 'edition.json', {'format': 'question-expansion/v1', 'edition': '2026-11',
        'sources': {'path': 'sources.json', 'sha256': sha((directory / 'sources.json').read_bytes())}, 'inputs': inputs})
    return directory


class ExpansionTests(unittest.TestCase):
    def test_documented_cli_requires_current_explicit_selection(self):
        with tempfile.TemporaryDirectory() as tmp:
            edition(tmp, [row()])
            planned = Path(tmp) / 'planned'; compiled = Path(tmp) / 'compiled'
            command = [sys.executable, '-X', 'utf8', str(ROOT / 'scripts/question_expansion.py'), '--root', tmp]
            subprocess.run(command + ['--output', str(planned)], check=True, capture_output=True)
            selection_file = planned / 'PRATICA_AULA/M01-selection.json'
            self.assertTrue(selection_file.is_file())
            self.assertFalse((planned / 'preview-request.json').exists())
            subprocess.run(command + ['--output', str(compiled), '--topic', 'M01', '--purpose',
                           'PRATICA_AULA', '--selection', str(selection_file)], check=True, capture_output=True)
            request = json.loads((compiled / 'preview-request.json').read_text(encoding='utf-8'))
            self.assertEqual(len(request['keys']), 1)
            self.assertEqual(request['package']['purpose'], 'PRATICA_AULA')
            stale = json.loads(selection_file.read_text(encoding='utf-8')); stale[0]['fingerprint'] = '0'*64
            write_json(selection_file, stale)
            result = subprocess.run(command + ['--output', str(Path(tmp)/'stale'), '--topic', 'M01',
                '--purpose', 'PRATICA_AULA', '--selection', str(selection_file)], capture_output=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertFalse((Path(tmp)/'stale/preview-request.json').exists())

    def test_external_edition_uses_its_own_sources_without_rewriting_repository(self):
        with tempfile.TemporaryDirectory() as external, tempfile.TemporaryDirectory() as empty_root:
            directory = edition(external, [row()])
            compiled = compile_edition(empty_root, directory)
            self.assertEqual(list(compiled), [('M01', 'PRATICA_AULA')])
            self.assertFalse((Path(empty_root) / EDITION).exists())
            output = Path(external) / 'plans'
            subprocess.run([sys.executable, '-X', 'utf8', str(ROOT/'scripts/question_expansion.py'),
                '--root', empty_root, '--edition-root', str(directory), '--output', str(output)],
                check=True, capture_output=True)
            self.assertTrue((output/'PRATICA_AULA/M01-selection.json').exists())

    def test_incremental_identity_and_permuted_alternatives_are_stable(self):
        original = row(); permuted = copy.deepcopy(original); permuted['alternatives'].reverse()
        a = cell([original]); b = cell([permuted])
        self.assertEqual(a, b)
        request, digests = request_for(a, 'PRATICA_AULA', selection(a))
        self.assertEqual((request, digests), request_for(b, 'PRATICA_AULA', selection(b)))
        more = cell([original, row(2)])
        self.assertEqual(a['items'][0], more['items'][0])
        changed = copy.deepcopy(original); changed['correctOption'] = 'five'; del changed['checkExpr']
        c = cell([changed])
        self.assertEqual(a['items'][0]['origin'], c['items'][0]['origin'])
        self.assertEqual(a['items'][0]['content']['alternatives'], c['items'][0]['content']['alternatives'])
        self.assertNotEqual(a['items'][0]['fingerprint'], c['items'][0]['fingerprint'])

    def test_purpose_namespace_and_selection_are_isolated(self):
        a = cell([row()]); b = cell([row()], 'SIMULADO')
        self.assertEqual(a['items'][0]['origin']['namespace'], 'lesson-practice-v1')
        self.assertEqual(b['items'][0]['origin']['namespace'], 'admin-question-v1')
        self.assertNotEqual(a['items'][0]['origin']['key'], b['items'][0]['origin']['key'])
        with self.assertRaisesRegex(ValueError, 'Namespace'): request_for(a, 'SIMULADO', selection(a))
        with self.assertRaises(ValueError): request_for(a, 'PRATICA_AULA', selection(b))
        bad = selection(a); bad[0]['fingerprint'] = '0' * 64
        with self.assertRaises(ValueError): request_for(a, 'PRATICA_AULA', bad)
        with self.assertRaises(ValueError): request_for(a, 'PRATICA_AULA', selection(a) * 2)
        with self.assertRaises(ValueError): request_for(a, 'PRATICA_AULA', [])

    def test_invalid_source_hash_catalog_identity_and_manifest_are_rejected(self):
        with self.assertRaisesRegex(ValueError, 'repetido'): read_json('{"answer":1,"answer":2}')
        with self.assertRaisesRegex(ValueError, 'Constante'): read_json('{"answer":NaN}')
        for patch in ({'topicId': 'P99'}, {'originKind': 'UNKNOWN'}, {'id': '../bad'},
                      {'references': ['missing']}, {'correctOption': 'absent'}, {'checkExpr': '4+4'}):
            with self.subTest(patch=patch), self.assertRaises((ValueError, KeyError)):
                cell([{**row(), **patch}])
        refs = copy.deepcopy(REFS); refs['synthetic']['url'] = 'https://example.test/gabarito.pdf'
        with self.assertRaises(ValueError): compile_row(row(), refs, 'SIMULADO', ROOT)
        # Nome de prova/gabarito ou página de banca também é recusado na projeção (achado A1).
        for name, url in (('Concurso X, prova e gabarito definitivo oficiais', 'https://example.test/study'),
                          ('Fonte de estudo', 'https://www.cebraspe.org.br/concursos/pf_21')):
            refs = copy.deepcopy(REFS); refs['synthetic'].update(name=name, url=url)
            with self.subTest(name=name), self.assertRaisesRegex(ValueError, 'prova/gabarito'):
                compile_row(row(), refs, 'SIMULADO', ROOT)
        with tempfile.TemporaryDirectory() as tmp:
            directory = edition(tmp, [row()])
            self.assertEqual(len(compile_edition(tmp)), 1)
            (directory / 'pratica_aula/matematica.jsonl').write_text('{}\n', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'hash'): compile_edition(tmp)

    def test_synthetic_complete_requests_obey_100_limit(self):
        c = cell([row(i) for i in range(100)])
        request, report = request_for(c, 'PRATICA_AULA', selection(c))
        self.assertEqual(len(request['package']['items']), 100)
        self.assertEqual(len(request['package']['files']), 100)
        self.assertEqual(len(report['packageDigest']), 64)
        more = cell([row(i) for i in range(101)])
        with self.assertRaisesRegex(ValueError, '1–100'): request_for(more, 'PRATICA_AULA', selection(more))

    def test_originals_need_actual_dossier_and_cannot_be_reused(self):
        r = {**row(), 'originKind': 'ORIGINAL_DE_PROVA'}
        with self.assertRaisesRegex(ValueError, 'Dossiê'): cell([r])
        with tempfile.TemporaryDirectory() as tmp:
            directory = edition(tmp)
            evidence = directory / 'dossier/synthetic.txt'; evidence.parent.mkdir()
            evidence.write_bytes(b'SYNTHETIC EVIDENCE ONLY')
            doc = {'path': 'dossier/synthetic.txt', 'sha256': sha(evidence.read_bytes()),
                   'url': 'https://example.test/evidence', 'note': 'Apenas teste; nenhuma permissão real alegada'}
            r['exam'] = {'board': 'Fixture', 'competition': 'Teste', 'year': 2025, 'role': 'Fixture',
                'booklet': 'Único', 'number': '1', 'identity': 'synthetic-official-1',
                'status': 'VALID', 'definitive': True, 'proof': doc, 'answer': doc, 'reuse': doc}
            c = cell([r], directory=directory)
            self.assertEqual(c['dossier'][0]['officialIdentity'], 'synthetic-official-1')
            for patch in ({'status': 'ANNULLED'}, {'definitive': False}, {'reuse': {**doc, 'sha256': '0'*64}},
                          {'reuse': {**doc, 'url': 'https://user:synthetic@example.test/evidence'}}):
                with self.assertRaises(ValueError): cell([{**r, 'exam': {**r['exam'], **patch}}], directory=directory)
            edition(tmp, [r, {**r, 'id': 'fixture-other'}])
            with self.assertRaisesRegex(ValueError, 'reutilizada'): compile_edition(tmp)

    def test_exact_numeric_and_near_duplicate_decisions_bind_current_pair(self):
        a = cell([row()])['items'][0]
        b = cell([{**row(2), 'statement': row()['statement']}], 'SIMULADO')['items'][0]
        report = audit_pairs([a], [b]); self.assertIn('EXACT_PROBLEM', report['pairs'][0]['signals'])
        c = cell([{**row(3), 'statement': 'Fixture exclusiva 8: calcule 3 + 3.'}], 'SIMULADO')['items'][0]
        report = audit_pairs([a], [c]); pair = report['pairs'][0]
        self.assertIn('NUMERIC_STRUCTURE', pair['signals'])
        decision = {'pair': pair['pair'], 'fingerprints': pair['fingerprints'], 'decision': 'EQUIVALENT',
                    'rationale': 'Mesma operação; apenas troca de valores.', 'responsible': 'Fixture IA', 'at': '2026-11-10T12:00:00Z'}
        self.assertEqual(audit_pairs([a], [c], [decision])['pairs'][0]['decisionState'], 'CURRENT')
        changed = copy.deepcopy(a); changed['content']['resolution']['text'] += ' Conferido.'
        from question_package import fingerprint
        changed['fingerprint'] = fingerprint(changed['content'])
        self.assertEqual(audit_pairs([changed], [c], [decision])['pairs'][0]['decisionState'], 'STALE')
        near = cell([{**row(4), 'statement': 'Fixture exclusiva 1: calcule e justifique 2 + 2.'}], 'SIMULADO')['items'][0]
        self.assertIn('TEXT_SIMILARITY', audit_pairs([a], [near])['pairs'][0]['signals'])

    def test_similarity_bounds_preserve_full_reference_results(self):
        from difflib import SequenceMatcher
        from itertools import product
        texts = ['', 'a', 'ab', 'ba', 'a' * 240 + 'b', 'a' * 240 + 'c',
                 'protocolo ' * 30 + 'recebimento', 'protocolo ' * 30 + 'encaminhamento',
                 'prazo de guarda 20 anos', 'prazo de guarda 25 anos',
                 'a b c d e f g h', 'h g f e d c b a']
        for left, right in product(texts, repeat=2):
            reference = SequenceMatcher(None, left, right, autojunk=False).ratio()
            for threshold in (0, .72, .75, 1, reference, min(1, reference + .00001)):
                with self.subTest(left=left[:25], right=right[:25], threshold=threshold):
                    self.assertEqual(similarity_at_least(left, right, threshold), reference >= threshold)

    def test_generic_prompt_with_different_alternatives_is_not_exact_problem(self):
        first = row(); first['statement'] = 'Assinale a alternativa correta.'
        other = row(2); other['statement'] = first['statement']
        other['alternatives'] = [{'id':'one','text':'Uma pasta pode conter arquivos e subpastas.'},
                                 {'id':'two','text':'Pastas servem exclusivamente para navegar na internet.'}]
        other['correctOption'] = 'one'; del other['checkExpr']
        a = cell([first])['items'][0]; b = cell([other], 'SIMULADO')['items'][0]
        self.assertTrue(all('EXACT_PROBLEM' not in p['signals'] for p in audit_pairs([a],[b])['pairs']))

    def test_numeric_template_preserves_operations_and_exponents(self):
        for left, right in [('2 + 2', '2 × 2'), ('3²', '32'),
                            ('101²−99²', '18−3×4'),
                            ('|−7| − |−3−5|', '(3²)³/3⁴')]:
            with self.subTest(left=left, right=right):
                self.assertNotEqual(numeric_problem_template(left), numeric_problem_template(right))
        self.assertEqual(numeric_problem_template('2 × 3'), numeric_problem_template('7 * 8'))
        self.assertEqual(numeric_problem_template('3²'), numeric_problem_template('7³'))

    def test_different_operations_can_be_adjudicated_but_numeric_variants_cannot(self):
        first = row(); first['statement'] = 'Quanto vale 2 + 2?'
        other = row(2); other['statement'] = 'Quanto vale 2 × 2?'; del other['checkExpr']
        a = cell([first])['items'][0]; b = cell([other], 'SIMULADO')['items'][0]
        pair = audit_pairs([a], [b])['pairs'][0]
        self.assertNotIn('EXACT_PROBLEM', pair['signals'])
        decision = {'pair': pair['pair'], 'fingerprints': pair['fingerprints'], 'decision': 'DISTINCT',
                    'rationale': 'Adição e multiplicação são operações distintas.',
                    'responsible': 'Fixture IA', 'at': '2026-11-12T12:00:00Z'}
        self.assertFalse(audit_pairs([a], [b], [decision])['pairs'][0]['blocksCandidate'])
        variant = row(3); variant['statement'] = 'Quanto vale 3 + 3?'; del variant['checkExpr']
        c = cell([variant], 'SIMULADO')['items'][0]
        pair = audit_pairs([a], [c])['pairs'][0]
        decision.update(pair=pair['pair'], fingerprints=pair['fingerprints'])
        self.assertTrue(audit_pairs([a], [c], [decision])['pairs'][0]['blocksCandidate'])

    def test_punctuation_with_meaning_is_not_an_exact_duplicate(self):
        first = row(); first.update(topicId='P04', statement='Compare a ordem: Não espere.')
        other = row(2); other.update(topicId='P04', statement='Compare a ordem: Não, espere.')
        del first['checkExpr']; del other['checkExpr']
        a = cell([first])['items'][0]; b = cell([other], 'SIMULADO')['items'][0]
        pair = audit_pairs([a], [b])['pairs'][0]
        self.assertNotIn('EXACT_PROBLEM', pair['signals'])
        decision = {'pair': pair['pair'], 'fingerprints': pair['fingerprints'], 'decision': 'DISTINCT',
                    'rationale': 'A vírgula muda a ordem de não esperar para esperar.',
                    'responsible': 'Fixture IA', 'at': '2026-11-12T12:00:00Z'}
        self.assertFalse(audit_pairs([a], [b], [decision])['pairs'][0]['blocksCandidate'])

    def test_observation_counts_base_once_not_roles_revisions_or_variants(self):
        c = cell([row()]); i = c['items'][0]
        q = {'questionId': 'synthetic-1', 'origin': i['origin'], 'eligibleRevisionId': 'old',
             'revisions': [{'id': 'old', 'fingerprint': i['fingerprint'], 'status': 'APPROVED'},
                           {'id': 'new', 'fingerprint': '0'*64, 'status': 'DRAFT'}]}
        observation = {'destination': 'synthetic-173-only', 'observedAt': '2026-11-10T12:00:00-03:00',
                       'complete': True, 'report': {'questions': [q]}, 'contents': {i['fingerprint']: i['content']}}
        counts, _ = observed_counts(observation)
        self.assertEqual(counts[('M01', 'PRATICA_AULA')], 1)
        self.assertEqual(sum(counts.values()), 1)
        empty = {**observation, 'report': {'questions': []}, 'contents': {}}
        self.assertEqual(sum(observed_counts(empty)[0].values()), 0)
        self.assertIsNone(observed_counts(None))
        observation['report']['questions'].append(copy.deepcopy(q))
        with self.assertRaisesRegex(ValueError, 'duplicada'): observed_counts(observation)

    def test_historical_inventory_is_not_database_and_all_102_cells_remain_explicit(self):
        existing, historical, metadata = baseline(ROOT)
        self.assertEqual(metadata['legacyValidCandidates'], 509)
        self.assertEqual(sum(v for (t, p), v in historical.items() if p == 'SIMULADO'), 251)
        self.assertEqual(sum(v for (t, p), v in historical.items() if p == 'PRATICA_AULA'), 255)
        self.assertEqual(len(existing), 764)
        report = coverage(ROOT, {})
        self.assertEqual(len(report['cells']), 102)
        self.assertTrue(all(c['databaseEligible'] is None and c['databaseDeficit'] is None for c in report['cells']))
        self.assertEqual(sum(c['historicalReferenceDeficit'] for c in report['cells']), 2044)

    def test_changed_review_no_longer_counts_candidate(self):
        c = cell([row()]); item = c['items'][0]
        review = {'key': item['origin']['key'], 'fingerprint': item['fingerprint'], 'checks': sorted(CHECKS),
                  'responsible': 'Fixture IA', 'at': '2026-11-10T12:00:00Z', 'limits': 'Teste sintético somente'}
        cells = {('M01', 'PRATICA_AULA'): c}
        def count(r): return next(v for v in r['cells'] if (v['topicId'], v['purpose']) == ('M01', 'PRATICA_AULA'))['reviewedCandidates']
        self.assertEqual(count(coverage(ROOT, cells, reviews=[review])), 1)
        review['fingerprint'] = '0'*64
        self.assertEqual(count(coverage(ROOT, cells, reviews=[review])), 0)


def emit_fixture(output):
    for purpose in PURPOSES:
        c = cell([row(i) for i in range(100)], purpose)
        request, report = request_for(c, purpose, selection(c))
        write_json(Path(output) / f'{purpose}.json', {'request': request, 'digests': report})


if __name__ == '__main__':
    if len(sys.argv) == 3 and sys.argv[1] == '--fixture-output':
        emit_fixture(sys.argv[2])
    else:
        unittest.main()
