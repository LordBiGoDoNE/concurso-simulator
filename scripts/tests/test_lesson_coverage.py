"""Regressions for the read-only lesson coverage matrix."""
import contextlib
import hashlib
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'scripts'))
import lesson_coverage as lc


def question(key, topic, statement='Enunciado', provision=None):
    reference = {'name': 'Fonte', 'url': 'https://example.test'}
    if provision:
        reference['legalBasis'] = {'provision': provision, 'checkedOn': '2026-11-01'}
    return {'origin': {'namespace': 'n', 'key': key}, 'content': {
        'statement': {'text': statement},
        'alternatives': [{'id': 'a', 'content': {'text': 'Certa'}},
                         {'id': 'b', 'content': {'text': 'Errada'}}],
        'correctAlternativeId': 'a', 'resolution': {'text': 'Porque sim.'},
        'classification': {'topicId': topic}, 'references': [reference]}}


def concept(identifier, topic, patterns, keys, **extra):
    return dict({'id': identifier, 'topicId': topic, 'concept': 'Conceito ' + identifier,
                 'kind': 'termo', 'patterns': patterns, 'questionKeys': keys}, **extra)


class Workspace:
    """A minimal repository: two topics, one question each, one lesson each."""

    def __init__(self, directory):
        self.root = Path(directory)
        self.write(lc.CATALOG, json.dumps({'topics': [
            {'id': 'I08', 'title': 'Excel - Macros'}, {'id': 'A08', 'title': 'Orçamento'}]}))
        self.package('SIMULADO', 'I08', [question('k.i08', 'I08')])
        self.package('PRATICA_AULA', 'A08', [question('k.a08', 'A08', provision='Lei 4.320, art. 41')])
        self.lesson('I08', 'O método PasteSpecial cola somente valores.')
        self.lesson('A08', 'Orçamento público e prestação de contas.')

    def write(self, relative, text):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8', newline='\n')

    def package(self, purpose, topic, items):
        self.write('%s/%s/%s/preview-request.json' % (lc.PACKAGES, purpose, topic),
                   json.dumps({'package': {'items': items}}))

    def lesson(self, topic, text):
        self.write('%s/%s.json' % (lc.LESSONS, topic), json.dumps({
            'format': 'native-lesson/v1', 'topicId': topic, 'title': topic,
            'children': ['\n', {'tag': 'section', 'children': [
                {'tag': 'h2', 'children': ['Título']},
                {'tag': 'p', 'children': [text[:10], {'tag': 'strong', 'children': [text[10:]]}]}]}]}))

    def concepts(self, rows):
        self.write(lc.CONCEPTS, ''.join(json.dumps(row, ensure_ascii=False) + '\n' for row in rows))

    def run(self, *args):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = lc.main(['--root', str(self.root), *args])
        return code, out.getvalue(), err.getvalue()

    def digest(self):
        files = sorted(p for p in self.root.rglob('*') if p.is_file())
        return hashlib.sha256(b''.join(p.read_bytes() for p in files)).hexdigest(), len(files)


VALID = [concept('I08-pastespecial', 'I08', ['PasteSpecial', 'valores'], ['k.i08']),
         concept('A08-credito', 'A08', ['cr[ée]dito extraordin[áa]rio'], ['k.a08'], kind='dispositivo')]


class LessonCoverageTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.workspace = Workspace(temporary.name)

    def test_reads_questions_with_correct_alternative_and_legal_basis(self):
        questions = lc.load_questions(self.workspace.root)
        self.assertEqual({'k.i08', 'k.a08'}, set(questions))
        self.assertEqual('Certa', questions['k.i08']['correct'])
        self.assertEqual('SIMULADO', questions['k.i08']['purpose'])
        self.assertEqual(['Lei 4.320, art. 41'], questions['k.a08']['provisions'])

    def test_lesson_text_keeps_words_across_inline_markup_and_separates_blocks(self):
        self.assertEqual('Título O método PasteSpecial cola somente valores.',
                         lc.lesson_text(self.workspace.root, 'I08'))

    def test_every_pattern_must_match_and_case_is_ignored(self):
        text = 'Use PasteSpecial para colar valores.'
        self.assertTrue(lc.is_covered({'patterns': ['pastespecial', 'VALORES|fórmulas']}, text))
        self.assertFalse(lc.is_covered({'patterns': ['PasteSpecial', 'xlPasteValues']}, text))

    def test_report_lists_absent_and_out_of_scope_without_changing_sources(self):
        self.workspace.concepts(VALID + [concept(
            'A08-antigo', 'A08', ['x'], ['k.a08'], outOfScope='Norma revogada.')])
        before = self.workspace.digest()
        code, out, _ = self.workspace.run()
        self.assertEqual(0, code)
        self.assertIn('3 conceitos: 1 cobertos, 1 ausentes, 1 fora de escopo.', out)
        self.assertEqual(before, self.workspace.digest())
        self.assertEqual(0, self.workspace.run('--write-report')[0])
        report = (self.workspace.root / lc.REPORT).read_text(encoding='utf-8')
        self.assertIn('| I08 | Excel - Macros | 1 | 1 | 1 | 0 | 0 |', report)
        self.assertIn('| A08 | Orçamento | 1 | 2 | 0 | 1 | 1 |', report)
        self.assertIn('- Conceito A08-credito (dispositivo; 1 questões)', report)
        self.assertIn('- Conceito A08-antigo (termo; 1 questões; Norma revogada.)', report)

    def test_check_fails_on_absent_concept_and_structure_check_does_not(self):
        self.workspace.lesson('I08', 'Use o método PasteSpecial para colar valores.')
        self.workspace.concepts(VALID)
        self.workspace.run('--write-report')
        self.assertEqual(0, self.workspace.run('--check-structure')[0])
        code, _, err = self.workspace.run('--check')
        self.assertEqual(1, code)
        self.assertIn('A08: conceito ausente na aula: Conceito A08-credito', err)
        self.workspace.lesson('A08', 'O crédito extraordinário atende despesas urgentes.')
        self.workspace.run('--write-report')
        self.assertEqual(0, self.workspace.run('--check')[0])

    def test_stale_report_fails_both_checks(self):
        self.workspace.lesson('I08', 'Use o método PasteSpecial para colar valores.')
        self.workspace.lesson('A08', 'O crédito extraordinário atende despesas urgentes.')
        self.workspace.concepts(VALID)
        for mode in ('--check', '--check-structure'):
            code, _, err = self.workspace.run(mode)
            self.assertEqual(1, code)
            self.assertIn('desatualizado', err)

    def test_structural_problems_are_named(self):
        rows = [
            concept('I08-a', 'I08', ['('], ['k.i08']),
            concept('I08-a', 'I08', ['x'], ['k.inexistente']),
            concept('X99-b', 'X99', ['x'], ['k.i08']),
            concept('A08-c', 'I08', ['x'], ['k.a08']),
            concept('I08-d', 'I08', [], [], kind='outro', outOfScope=' ', extra=1),
        ]
        self.workspace.concepts(rows)
        code, _, err = self.workspace.run('--check-structure')
        self.assertEqual(1, code)
        for expected in ("padrão inválido '('", 'id repetido', "questão inexistente 'k.inexistente'",
                         "assunto desconhecido 'X99'", 'id deve começar pelo assunto',
                         "questão 'k.a08' é de outro assunto", 'patterns vazio', 'questionKeys vazio',
                         "kind inválido 'outro'", 'outOfScope sem justificativa',
                         "campos desconhecidos ['extra']"):
            self.assertIn(expected, err)

    def test_question_without_concept_is_named(self):
        self.workspace.concepts(VALID[:1])
        code, _, err = self.workspace.run()
        self.assertEqual(1, code)
        self.assertIn('questão sem conceito: k.a08 (A08)', err)

    def test_question_with_legal_basis_needs_a_dispositivo_concept(self):
        self.workspace.lesson('I08', 'Use o método PasteSpecial para colar valores.')
        self.workspace.concepts([VALID[0], dict(VALID[1], kind='termo')])
        code, _, err = self.workspace.run('--check-structure')
        self.assertEqual(1, code)
        self.assertIn('questão com base legal sem conceito dispositivo: k.a08 (A08)', err)
        self.assertNotIn('k.i08', err)
        self.workspace.concepts(VALID + [concept('A08-termo', 'A08', ['contas'], ['k.a08'])])
        self.workspace.run('--write-report')
        code, _, err = self.workspace.run('--check-structure')
        self.assertEqual(0, code, err)

    def test_missing_list_and_invalid_json_fail(self):
        code, _, err = self.workspace.run()
        self.assertEqual(1, code)
        self.assertIn('arquivo ausente', err)
        self.workspace.write(lc.CONCEPTS, '{nao json\n[1]\n')
        code, _, err = self.workspace.run()
        self.assertEqual(1, code)
        self.assertIn('linha 1: JSON inválido', err)
        self.assertIn('linha 2: esperado um objeto', err)

    def test_curation_helpers_print_questions_and_lesson(self):
        code, out, _ = self.workspace.run('--questions', 'A08')
        self.assertEqual(0, code)
        self.assertIn('### k.a08 [PRATICA_AULA]', out)
        self.assertIn('  * Certa', out)
        self.assertIn('Base legal: Lei 4.320, art. 41', out)
        self.assertNotIn('k.i08', out)
        self.assertIn('método PasteSpecial cola', self.workspace.run('--lesson', 'I08')[1])


class RepositoryCoverageTests(unittest.TestCase):
    def test_committed_list_is_valid_and_report_is_current(self):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = lc.main(['--root', str(ROOT), '--check-structure'])
        self.assertEqual(0, code, err.getvalue()[-2000:])

    def test_every_topic_has_concepts(self):
        topics = lc.load_topics(ROOT)
        concepts, problems = lc.load_concepts(ROOT)
        self.assertEqual([], problems)
        self.assertEqual(51, len(topics))
        self.assertEqual(set(topics), {row['topicId'] for row in concepts})


if __name__ == '__main__':
    unittest.main()
