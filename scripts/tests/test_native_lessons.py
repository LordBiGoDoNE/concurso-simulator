"""Regressions for the complete native lesson selection and its provenance."""
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'scripts'))
from compile_native_lessons import Compiler, canonical, compile_all
from extract_questions import Document


class NativeLessonsTests(unittest.TestCase):
    def test_all_published_documents_match_the_reproducible_selection(self):
        output = ROOT / 'frontend/src/lesson-documents'
        generated = compile_all(ROOT)
        self.assertEqual(52, len(generated))
        self.assertEqual(set(generated), {path.name for path in output.glob('*.json')})
        for name, data in generated.items():
            self.assertEqual(data, canonical(output / name), name)

    def test_theory_examples_and_sources_remain_while_chrome_and_exercises_leave(self):
        source = Document('<main><header><h1>Antigo</h1></header><nav>Índice V3</nav>'
            '<section id="s1"><h2>Teoria</h2><p>Explicação</p></section>'
            '<section><h2>Exemplos resolvidos passo a passo</h2><div class="example">Exemplo</div></section>'
            '<section id="exercicios"><div class="q" id="q1">Questão e resposta</div></section>'
            '<section class="refs"><a href="https://example.test/fonte">Fonte</a></section><footer>V1</footer></main>')
        main = next(node for node in source.root.nodes() if node.tag == 'main')
        result = json.dumps(Compiler(ROOT, 'P01', {}).children(main, ROOT / 'example.html'), ensure_ascii=False)
        for retained in ['Teoria', 'Explicação', 'Exemplo', 'https://example.test/fonte', 'lesson-P01-s1']:
            self.assertIn(retained, result)
        for removed in ['Antigo', 'Índice V3', 'Questão e resposta', 'V1', 'exercicios']:
            self.assertNotIn(removed, result)

    def test_unexpected_local_links_and_active_markup_fail_compilation(self):
        for html in ['<a href="old/Gabarito.html">Resposta</a>', '<script>alert(1)</script>', '<p onclick="alert(1)">Texto</p>']:
            document = Document('<main>' + html + '</main>')
            main = next(node for node in document.root.nodes() if node.tag == 'main')
            with self.assertRaises(ValueError):
                Compiler(ROOT, 'P01', {}).children(main, ROOT / 'example.html')


if __name__ == '__main__':
    unittest.main()
