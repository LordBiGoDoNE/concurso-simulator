"""Conversão é estrutural, não revisão/aprovação do conteúdo real."""
import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from extract_questions import Document, accessible, candidate, extract, image_resource
from question_package import read_local, safe_url, fingerprint

ROOT = Path(__file__).resolve().parents[2]


class ExtractionTests(unittest.TestCase):
    def fixture(self, text="Quanto é 2 + 2?", answer="A", attributes="", option="4"):
        module = {"id": "P01", "conteudo": "01_Portugues/fixture.html", "gabarito": "01_Portugues/answers.html",
                  "referencias": [{"titulo": "Referência sintética", "url": "https://example.test/study"}]}
        row = {"ID": "P01-Q01", "Questao": "1", "Arquivo": module["conteudo"], "Origem": "Fixture, sem validade editorial",
               "Tipo": "Autoral — língua portuguesa", "Fundamento_legal": ""}
        q = Document(f'<div class="q" id="q1"><strong>Questão 1.</strong>{text}<ol type="A"><li {attributes}>{option}</li><li>5</li></ol></div>').questions()
        a = Document(f'<div class="q" id="q1"><h3>Questão 1 — alternativa {answer}</h3><p>2 + 2 = 4, em duas etapas.</p></div>').questions()
        return module, row, q, a

    def convert(self, fixture=None):
        module, row, q, a = fixture or self.fixture()
        return candidate(ROOT, module, row, q, a, "a" * 64, "b" * 64)

    def test_complete_inventory_and_no_silent_loss_or_editorial_approval(self):
        package = extract(ROOT)
        self.assertEqual(len(package["inventory"]), 510)
        self.assertEqual(len(package["items"]) + len(package["issues"]), 510)
        self.assertEqual(package["issues"], [{"key": "P05-Q01", "message": "Alternativas repetidas"}])
        self.assertEqual(len(package["items"]), 509)
        self.assertEqual(len(package["catalog"]), 51)
        self.assertTrue(all(row["editorialReviewRequired"] for row in package["inventory"]))
        self.assertEqual(sum(bool(row["legalBasisPending"]) for row in package["inventory"]), 30)
        self.assertTrue(package["possibleDuplicates"])
        for item in package["items"]:
            self.assertEqual(item["fingerprint"], fingerprint(item["content"]))
            self.assertIsNone(item["expectedLatestRevision"])
            self.assertNotIn("APPROVED", json.dumps(item))
            for ref in item["content"]["references"]: self.assertIsNone(ref["legalBasis"])
        math = next(i for i in package["items"] if i["origin"]["key"] == "M04-Q07")
        self.assertIn("/(", math["content"]["resolution"]["text"])
        self.assertNotIn("Ver resposta", math["content"]["statement"]["text"])

    def test_stable_options_and_fingerprint_unicode_and_every_relevant_field(self):
        item = self.convert(self.fixture(text="πr², é e é; 2 < 3"))
        self.assertEqual(item, self.convert(self.fixture(text="πr², é e é; 2 < 3")))
        self.assertIn("é e é", item["content"]["statement"]["text"])
        for field in ("statement", "resolution"):
            changed = copy.deepcopy(item["content"]); changed[field]["text"] += "!"
            self.assertNotEqual(fingerprint(changed), item["fingerprint"])
        changed = copy.deepcopy(item["content"]); changed["references"][0]["name"] += "!"
        self.assertNotEqual(fingerprint(changed), item["fingerprint"])
        changed = copy.deepcopy(item["content"]); changed["classification"]["roleIds"] = ["secretario-de-escola"]
        self.assertNotEqual(fingerprint(changed), item["fingerprint"])

    def test_ids_join_numbers_csv_and_answer_not_paragraph_proximity(self):
        for update in ({"ID": "P01-Q02"}, {"Questao": "2"}, {"Arquivo": "outro.html"}):
            f = self.fixture(); f[1].update(update)
            with self.assertRaises((ValueError, KeyError)): self.convert(f)
        for answer in ("E", "A e B", ""):
            with self.subTest(answer=answer), self.assertRaises(ValueError): self.convert(self.fixture(answer=answer))
        f = self.fixture(); del f[3]["q1"]
        with self.assertRaises(KeyError): self.convert(f)

    def test_scripts_events_unknown_markup_and_correctness_attributes_are_rejected(self):
        for text in ('<script>alert(1)</script>', '<iframe src="x"></iframe>', '<p onclick="bad()">Texto</p>', '<span data-correct="true">Texto</span>'):
            with self.subTest(text=text), self.assertRaises(ValueError): self.convert(self.fixture(text=text))
        for attrs in ('onclick="bad()"', 'class="correct"', 'style="color:red"'):
            with self.subTest(attrs=attrs), self.assertRaises(ValueError): self.convert(self.fixture(attributes=attrs))
        for text in ('<div class="q" id="q1"></div><div class="q" id="q1"></div>', '<div><p></div>', '<div>'):
            with self.assertRaises(ValueError): Document(text).questions()

    def test_plain_representation_keeps_emphasis_lists_and_fraction_structure(self):
        node = Document('<p><strong>Importante</strong>: <em>π</em></p><ul><li>Passo 1</li><li>Passo 2</li></ul><math xmlns="http://www.w3.org/1998/Math/MathML"><mfrac><mn>1</mn><mn>2</mn></mfrac></math>')
        value = accessible(node.root.children, ROOT, '01_Portugues/fixture.html')
        for text in ('[destaque: Importante]', '[ênfase: π]', '• Passo 1', '(1)/(2)'): self.assertIn(text, value['text'])
        self.assertNotIn('<', value['text'])

    def test_roots_traversal_symlinks_unsafe_urls_and_assets(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); (root/'assets').mkdir(); (root/'assets/image.png').write_bytes(b'\x89PNG\r\n\x1a\nfixture')
            (root/'assets/link.png').symlink_to(root/'assets/image.png')
            for path in ('../secret', '/etc/passwd', 'assets/../secret', 'assets\\image.png', 'assets/link.png', 'unapproved/image.png'):
                with self.subTest(path=path), self.assertRaises(ValueError): read_local(root, path)
            resource = image_resource(Document('<img src="../assets/image.png" alt="Figura sintética">').root.children[0], root, '01_Portugues/fixture.html')
            self.assertEqual(resource['sha256'], hashlib.sha256((root/'assets/image.png').read_bytes()).hexdigest())
            for src in ('../../outside.png', 'https://example.test/image.png', '../assets/link.png', '../assets/image.png?answer=A'):
                with self.subTest(src=src), self.assertRaises(ValueError): image_resource(Document(f'<img src="{src}" alt="Descrição">').root.children[0], root, '01_Portugues/fixture.html')
            (root/'assets/active.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg"><script>bad()</script></svg>')
            with self.assertRaises(ValueError): image_resource(Document('<img src="../assets/active.svg" alt="Descrição">').root.children[0], root, '01_Portugues/fixture.html')
        for url in ('javascript:alert(1)', 'data:text/html,x', 'https://user:pass@example.test/x', 'https://example.test/gabarito.html'):
            with self.assertRaises(ValueError): safe_url(url)

    def test_html_exponents_indices_and_nested_math_keep_structure(self):
        node = Document('<p>2<sup>3</sup>; H<sub>2</sub>O; x<sup>1<sub>n</sub></sup></p>')
        text = accessible(node.root.children, ROOT, '01_Portugues/fixture.html')['text']
        self.assertEqual(text, '2^(3); H_(2)O; x^(1_(n))')

    def test_svg_processing_instructions_anywhere_are_rejected_without_rewriting(self):
        opening = '<svg xmlns="http://www.w3.org/2000/svg">'
        pi = '<?xml-stylesheet type="text/css" href="https://example.test/style.css"?>'
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); (root/'assets').mkdir(); asset = root/'assets/fixture.svg'
            image = Document('<img src="../assets/fixture.svg" alt="Figura sintética">').root.children[0]
            for xml in (pi + opening + '</svg>', opening + pi + '</svg>', opening + '</svg>' + pi,
                        '<?private sentinel?>' + opening + '</svg>',
                        '<!DOCTYPE svg [<!ENTITY private "synthetic">]>' + opening + '&private;</svg>'):
                for encoding in ('utf-8', 'utf-16'):
                    with self.subTest(xml=xml, encoding=encoding):
                        data = xml.encode(encoding); asset.write_bytes(data)
                        with self.assertRaises(ValueError): image_resource(image, root, '01_Portugues/fixture.html')
                        self.assertEqual(asset.read_bytes(), data)
            for encoding in ('utf-8', 'utf-16'):
                xml = '<?xml version="1.0" encoding="'+encoding+'"?>' + opening + '<!-- <?private benign text?> --><rect width="1"/></svg>'
                data = xml.encode(encoding); asset.write_bytes(data)
                self.assertEqual(image_resource(image, root, '01_Portugues/fixture.html')['sha256'], hashlib.sha256(data).hexdigest())

    def test_unknown_asides_create_pending_issue_not_silent_candidates(self):
        for aside in ('<aside>Considere x = 4.</aside>',
                      '<aside class="callout callout--source">Considere x = 4.</aside>'):
            for side in (2, 3):
                with self.subTest(aside=aside, side=side):
                    f = self.fixture(); f[side]['q1'].children.extend(Document(aside).root.children)
                    with self.assertRaises(ValueError): self.convert(f)

    def test_small_resolution_text_is_preserved_not_treated_as_navigation(self):
        f = self.fixture(); f[3]['q1'].children.extend(Document('<p class="small">Considere x = 4.</p>').root.children)
        item = self.convert(f)
        self.assertIn('Considere x = 4.', item['content']['resolution']['text'])
        self.assertNotEqual(item['fingerprint'], self.convert()['fingerprint'])

    def test_only_exact_known_source_and_navigation_blocks_are_excluded(self):
        source = ('<aside class="callout callout--source"><span class="callout-label">Fonte da questão</span>'
                  '<p><strong>Autoral — língua portuguesa</strong> · P01-Q01.</p>'
                  '<p>Origem: material aprofundado do projeto [Gaby], mantido na V3.</p>'
                  '<p>Base para estudo e conferência: <a href="#referencias">Referências temáticas deste módulo</a>.</p></aside>')
        navigation = '<p class="small"><a href="fixture.html#q1">Rever o enunciado da questão 1</a></p>'
        f = self.fixture()
        for side in (2, 3): f[side]['q1'].children.extend(Document(source).root.children)
        f[3]['q1'].children.extend(Document(navigation).root.children)
        self.assertEqual(self.convert(f), self.convert())
        for html in (source.replace('P01-Q01', 'P01-Q02'), source.replace('</aside>', '<p>Considere x = 4.</p></aside>'),
                     source.replace('<aside ', '<aside onclick="bad()" '),
                     navigation.replace('</p>', 'Considere x = 4.</p>'), navigation.replace('#q1', '#q2'),
                     navigation.replace('fixture.html#q1', 'https://example.test/fixture.html#q1')):
            with self.subTest(html=html):
                f = self.fixture(); f[3]['q1'].children.extend(Document(html).root.children)
                with self.assertRaises(ValueError): self.convert(f)

    def test_legal_source_block_requires_exact_manifest_reference_and_csv_basis(self):
        f = self.fixture(); f[1]['Fundamento_legal'] = 'Art. 1º'
        source = ('<aside class="callout callout--source"><span class="callout-label">Fonte da questão</span>'
                  '<p><strong>Autoral — língua portuguesa</strong> · P01-Q01.</p>'
                  '<p>Origem: material aprofundado do projeto [Gaby], mantido na V3.</p>'
                  '<p>Base para estudo e conferência: <a href="https://example.test/study" target="_blank" rel="noopener">Referência sintética</a> · Art. 1º.</p></aside>')
        baseline = self.convert(f)
        for side in (2, 3): f[side]['q1'].children.extend(Document(source).root.children)
        self.assertEqual(self.convert(f), baseline)
        for changed in (source.replace('Art. 1º', 'Art. 2º'), source.replace('/study', '/other'),
                        source.replace('Referência sintética', 'Outra referência'),
                        source.replace('</aside>', '<p>Considere x = 4.</p></aside>')):
            with self.subTest(changed=changed):
                bad = self.fixture(); bad[1]['Fundamento_legal'] = 'Art. 1º'
                bad[2]['q1'].children.extend(Document(changed).root.children)
                with self.assertRaises(ValueError): self.convert(bad)

    def test_unsupported_auxiliary_content_stays_in_full_inventory_as_pending(self):
        baseline = extract(ROOT)
        module = json.loads((ROOT/'manifesto_v3.json').read_text())['modulos'][0]
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for source in baseline['sourceFiles']:
                target = root/source['path']; target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((ROOT/source['path']).read_bytes())
            lesson = root/module['conteudo']
            original = lesson.read_text()
            lesson.write_text(original.replace('<strong>Questão 1.</strong>',
                              '<strong>Questão 1.</strong><aside>Considere x = 4.</aside>', 1))
            package = extract(root)
            self.assertEqual(len(package['inventory']), 510)
            row = next(r for r in package['inventory'] if r['key'] == 'P01-Q01')
            self.assertEqual(row['status'], 'invalid')
            self.assertTrue(row['editorialReviewRequired'])
            self.assertTrue(any(i['key'] == 'P01-Q01' for i in package['issues']))
            self.assertFalse(any(i['origin']['key'] == 'P01-Q01' for i in package['items']))
            self.assertEqual(len(package['items']), len(baseline['items']) - 1)
            self.assertEqual((ROOT/module['conteudo']).read_text(), original)

    def test_svg_attributes_use_inert_value_grammars_not_url_blacklist(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); (root/'assets').mkdir(); asset = root/'assets/fixture.svg'
            image = Document('<img src="../assets/fixture.svg" alt="Figura sintética">').root.children[0]
            for attr in (r'style="filter:u\72l(https://example.test/effects.svg#fx)"',
                         r'style="filter:u\000072l(https://example.test/effects.svg#fx)"',
                         r'fill="u\72l(https://example.test/paint.svg#p)"',
                         r'stroke="u\72l(https://example.test/paint.svg#p)"',
                         'style="fill:red"', 'filter="none"', 'fill="var(--paint)"',
                         'href="https://example.test/remote.svg"', 'onclick="bad()"',
                         'xml:base="https://example.test/"', 'width="calc(1 + 2)"',
                         'id="x;filter:bad"', 'transform="translate(1);filter:bad"'):
                with self.subTest(attr=attr):
                    attrs = 'height="1" ' + attr if attr.startswith('width=') else 'width="1" height="1" ' + attr
                    data = ('<svg xmlns="http://www.w3.org/2000/svg"><rect '+attrs+'/></svg>').encode()
                    asset.write_bytes(data)
                    with self.assertRaises(ValueError): image_resource(image, root, '01_Portugues/fixture.html')
                    self.assertEqual(asset.read_bytes(), data)
            for xml in ('<svg xmlns="http://www.w3.org/2000/svg" xmlns:x="https://example.test/ns"><x:rect/></svg>',
                        '<svg xmlns="http://www.w3.org/2000/svg"><rect xmlns="https://example.test/ns"/></svg>'):
                asset.write_text(xml)
                with self.assertRaises(ValueError): image_resource(image, root, '01_Portugues/fixture.html')
            xml = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 80" role="img" aria-labelledby="title desc">'
                   '<title id="title">Figura</title><desc id="desc">Descrição</desc>'
                   '<g transform="translate(1,2) scale(2)"><path d="M0 0 L6 3 L0 6 Z" fill="#16796d" stroke="black" stroke-width="1.5"/>'
                   '<rect x="-1" y=".5" width="20px" height="30%" rx="2" fill="none" opacity=".5"/>'
                   '<text x="1" y="2" font-family="Segoe UI, Arial, sans-serif" font-size="14" font-weight="700" text-anchor="middle">πr²</text>'
                   '<polyline points="0,0 1,2 3,4" stroke-dasharray="1 2"/></g></svg>')
            asset.write_text(xml)
            self.assertEqual(image_resource(image, root, '01_Portugues/fixture.html')['sha256'], hashlib.sha256(asset.read_bytes()).hexdigest())


if __name__ == '__main__': unittest.main()
