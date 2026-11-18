"""Regressions for the guided math rewrite and the authored deeper sections."""
import re
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'scripts'))
from enrich_math import deep_sections, enhance

DEEP = ('<section class="card" id="a1"><h2>Aprofundamento 1 — Teste</h2>'
        '<p>Conteúdo aprofundado que precisa sobreviver.</p></section>')


class DeepSectionTests(unittest.TestCase):
    def test_deep_sections_are_found_in_page_order(self):
        html = ('<section class="card" id="s1"><h2>1. Guia</h2></section>'
                '<section class="card" id="a2"><h2>B</h2></section>'
                '<section class="card" id="a1"><h2>A</h2></section>')
        self.assertEqual([('a2', 'B'), ('a1', 'A')], deep_sections(html))

    def test_rewrite_keeps_deep_section_and_lists_it_in_the_map(self):
        with tempfile.TemporaryDirectory() as temporary:
            source, output = Path(temporary) / 'src', Path(temporary) / 'out'
            shutil.copytree(ROOT / '02_Matematica_Logica', source / '02_Matematica_Logica')
            (source / 'assets').mkdir()
            shutil.copy2(ROOT / 'assets/matematica.css', source / 'assets/matematica.css')
            study = next(p for p in (source / '02_Matematica_Logica').glob('Matemática 01 - *.html')
                         if 'Gabarito' not in p.name)
            text = study.read_text(encoding='utf-8')
            text = re.sub(r'(<section class="card" id="a\d+">.*?</section>)', '', text, flags=re.S)
            last = list(re.finditer(r'<section class="card" id="s\d+">.*?</section>', text, re.S))[-1]
            study.write_text(text[:last.end()] + DEEP + text[last.end():], encoding='utf-8')
            enhance(source, output)
            result = (output / '02_Matematica_Logica' / study.name).read_text(encoding='utf-8')
        self.assertIn('Conteúdo aprofundado que precisa sobreviver.', result)
        toc = re.search(r'<ol class="toc">(.*?)</ol>', result, re.S)[1]
        self.assertIn('<a href="#a1">Aprofundamento 1 — Teste</a>', toc)
        self.assertLess(toc.index('href="#s1"'), toc.index('href="#a1"'))


if __name__ == '__main__':
    unittest.main()
