"""Regressões da conferência dos pacotes finais #173 sobre cópias de pacotes reais."""
import base64
import hashlib
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import question_expansion_final as final

ROOT = Path(__file__).resolve().parents[2]
PACKAGES = ROOT / 'editorial/question-bank-expansion/2026-11/packages'
TOPICS = {'A01': 'conhecimentos-administrativos'}
NAMES = {'conhecimentos-administrativos': 'Conhecimentos Administrativos'}


def sha(data):
    return hashlib.sha256(data).hexdigest()


class FinalPackagesTests(unittest.TestCase):
    def setUp(self):
        self.temp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.temp)
        manifest = json.loads((PACKAGES / 'manifest.json').read_bytes())
        manifest['packages'] = [p for p in manifest['packages'] if p['topicId'] in TOPICS]
        for p in manifest['packages']:
            shutil.copytree(PACKAGES / p['purpose'] / p['topicId'], self.temp / p['purpose'] / p['topicId'])
        self.manifest = manifest
        self.write_manifest()
        for patch in (mock.patch.object(final, 'CATALOG', TOPICS),
                      mock.patch.object(final, 'DISCIPLINE_NAMES', NAMES)):
            patch.start()
            self.addCleanup(patch.stop)

    def write_manifest(self):
        (self.temp / 'manifest.json').write_text(json.dumps(self.manifest), encoding='utf-8')

    def request(self, purpose):
        return self.temp / purpose / 'A01' / 'preview-request.json'

    def rewrite(self, purpose, change):
        path = self.request(purpose)
        request = json.loads(path.read_bytes())
        change(request)
        path.write_text(json.dumps(request, ensure_ascii=False), encoding='utf-8')
        entry = next(p for p in self.manifest['packages'] if p['purpose'] == purpose)
        entry['requestSha256'] = sha(path.read_bytes())
        self.write_manifest()

    def test_real_packages_pass_and_inventory_lists_each_original_once(self):
        report, inventory = final.check(self.temp)
        self.assertEqual([], report['problems'])
        self.assertEqual(2, report['cells'])
        self.assertEqual(report['originals'], len(inventory['originals']))
        self.assertEqual(report['originals'], len({o['identity'] for o in inventory['originals']}))
        self.assertTrue(all(o['proof']['sha256'] and o['answer']['sha256'] for o in inventory['originals']))

    def test_edited_content_breaks_fingerprint_and_manifest_hash(self):
        path = self.request('SIMULADO')
        request = json.loads(path.read_bytes())
        request['package']['items'][0]['content']['statement']['text'] += ' '
        path.write_text(json.dumps(request, ensure_ascii=False), encoding='utf-8')
        problems = final.check(self.temp)[0]['problems']
        self.assertTrue(any('SHA-256 da requisição' in p for p in problems))
        self.assertTrue(any('fingerprint não corresponde' in p for p in problems))

    def test_cell_below_quota_and_selection_mismatch_are_reported(self):
        def drop(request):
            request['package']['items'] = request['package']['items'][:24]
        self.rewrite('PRATICA_AULA', drop)
        problems = final.check(self.temp)[0]['problems']
        self.assertTrue(any('24 itens, mínimo 25' in p for p in problems))
        self.assertTrue(any('seleção não corresponde' in p for p in problems))

    def test_reused_official_identity_across_modalities_is_rejected(self):
        dossier = json.loads((self.temp / 'SIMULADO/A01/dossier.json').read_bytes())
        identity = next(d['officialIdentity'] for d in dossier if d['officialIdentity'])
        target = self.temp / 'PRATICA_AULA/A01/dossier.json'
        practice = json.loads(target.read_bytes())
        original = next(d for d in practice if d['officialIdentity'])
        original['officialIdentity'] = identity
        target.write_text(json.dumps(practice, ensure_ascii=False), encoding='utf-8')

        def same_identity(request):
            for file in request['package']['files']:
                if file['path'] == f"sources/{original['key']}.json":
                    source = json.loads(base64.b64decode(file['base64']))
                    source['editorial']['exam']['identity'] = identity
                    raw = json.dumps(source, ensure_ascii=False).encode('utf-8')
                    file['base64'], file['sha256'] = base64.b64encode(raw).decode(), sha(raw)
        self.rewrite('PRATICA_AULA', same_identity)
        problems = final.check(self.temp)[0]['problems']
        self.assertTrue(any('usada mais de uma vez' in p and identity in p for p in problems))

    def test_missing_local_proofs_and_stale_reviews_fail(self):
        reviews = self.temp / 'reviews.json'
        dossier = json.loads((self.temp / 'SIMULADO/A01/dossier.json').read_bytes())
        reviews.write_text(json.dumps([{'key': d['key'], 'fingerprint': '0' * 64} for d in dossier]),
                           encoding='utf-8')
        report = final.check(self.temp, edition_root=self.temp / 'absent', reviews_path=reviews)[0]
        self.assertTrue(any('arquivo local de proof' in p for p in report['problems']))
        self.assertTrue(any('sem revisão individual' in p for p in report['problems']))

    def test_exam_or_answer_key_reference_in_open_projection_is_reported(self):
        def corrective(request):
            item = request['package']['items'][0]
            item['content']['references'].append({'name': 'Banca — Concurso, prova e gabarito definitivo oficiais',
                                                  'url': 'https://www.cebraspe.org.br/concursos/pf_21', 'legalBasis': None})
        self.rewrite('SIMULADO', corrective)
        problems = final.check(self.temp)[0]['problems']
        self.assertTrue(any('referência pública aponta para prova/gabarito' in p for p in problems))

    def test_reference_equal_to_any_provenance_exam_url_is_reported(self):
        dossier = json.loads((self.temp / 'PRATICA_AULA/A01/dossier.json').read_bytes())
        original = next(d for d in dossier if d['officialIdentity'])
        request = json.loads(self.request('PRATICA_AULA').read_bytes())
        source = next(f for f in request['package']['files'] if f['path'] == f"sources/{original['key']}.json")
        proof_url = json.loads(base64.b64decode(source['base64']))['editorial']['exam']['proof']['url']
        disguised = proof_url.replace('https://', 'http://', 1).replace('://www.', '://', 1) + '/'

        def same_as_proof(request):
            # A referência fica em outro pacote/modalidade e não segue nenhum padrão conhecido.
            request['package']['items'][0]['content']['references'].append(
                {'name': 'Material complementar', 'url': disguised, 'legalBasis': None})
        self.rewrite('SIMULADO', same_as_proof)
        problems = final.check(self.temp)[0]['problems']
        self.assertTrue(any('coincide com prova/gabarito da proveniência' in p for p in problems), problems)

    def test_study_reference_rejects_exam_pages_by_name_url_or_board_host(self):
        from question_package import study_reference
        for name, url in (('Prova e gabarito definitivo', 'https://example.org/estudo'),
                          ('Material de estudo', 'https://www.cebraspe.org.br/concursos/inss_22'),
                          ('Página do concurso', 'https://example.org/concursos/2026'),
                          ('Caderno de prova oficial', 'https://example.org/a.pdf'),
                          ('Edital', 'https://anexos.cdn.selecao.net.br/uploads/301/x.pdf'),
                          ('Arquivo municipal', 'https://pss.crato.ce.gov.br/uploads/arquivos/provas/PROVA%20X.pdf')):
            with self.assertRaises(ValueError, msg=name):
                study_reference(name, url)
        for name, url in (('IMPA/OBMEP — Banco de Questões; questão transcrita de prova oficial', 'https://www.obmep.org.br/banco.htm'),
                          ('Resolução CNE/CEB nº 4/2010', 'https://www.planalto.gov.br/ccivil_03/leis/l9394.htm')):
            self.assertEqual(url, study_reference(name, url))

    def test_path_escape_is_not_resolved(self):
        self.assertIsNone(final.local_file(self.temp, '../manifest.json'))
        self.assertIsNone(final.local_file(self.temp, 'C:/Windows/win.ini'))


if __name__ == '__main__':
    unittest.main()
