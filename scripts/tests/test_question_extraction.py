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
        row = {"ID": "P01-Q01", "Questao": "1", "Arquivo": module["conteudo"], "Origem": "Fixture, sem validade editorial"}
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
        for text in ('**Importante**', '_π_', '• Passo 1', '(1)/(2)'): self.assertIn(text, value['text'])
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


if __name__ == '__main__': unittest.main()
