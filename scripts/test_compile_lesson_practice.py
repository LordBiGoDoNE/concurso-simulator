import copy
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import compile_lesson_practice as lp

ROOT=Path(__file__).resolve().parents[1]
class PracticeEditorialTests(unittest.TestCase):
    def test_full_coverage_numeric_oracles_and_deterministic_fingerprints(self):
        pack,rows,numeric=lp.compile_practice(ROOT)
        self.assertEqual(255,len(rows));self.assertEqual(51,len({r['topicId'] for r in rows}))
        self.assertEqual(55,len(numeric));self.assertEqual(pack,lp.compile_practice(ROOT)[0])
        self.assertTrue(all(i['origin']['namespace']==lp.NAMESPACE for i in pack['items']))
    def test_oracle_rejects_code_and_supports_exact_fraction_and_units(self):
        for expr in ['__import__("os")','(1).__class__','2**999','sqrt(2)','lcm(2,3,4)']:
            with self.assertRaises(ValueError):lp.calculate(expr)
        self.assertEqual(lp.calculate('7/8-1/4'),lp.answer_number('5/8'))
        self.assertEqual(lp.calculate('-2'),lp.answer_number('−2 reais'))
        self.assertEqual(lp.calculate('2400'),lp.answer_number('2.400'))
    def test_wrong_answer_and_missing_question_are_rejected(self):
        import shutil
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);shutil.copytree(ROOT/lp.DIRECTORY,root/lp.DIRECTORY)
            file=root/lp.DIRECTORY/'matematica.jsonl';lines=file.read_text(encoding='utf-8').splitlines()
            original=lines.copy();row=json.loads(lines[0]);row['correctIndex']=1;lines[0]=json.dumps(row)
            file.write_text('\n'.join(lines),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Cálculo divergente'):lp.compile_practice(root)
            file.write_text('\n'.join(original[1:]),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'cinco questões'):lp.compile_practice(root)
    def test_current_curated_revisions_are_in_comparison_snapshot(self):
        from curate_questions import compile_curation
        _,curated,*_=compile_curation(ROOT)
        snapshot=lp.comparison_snapshot(ROOT)
        indexed={i['origin']['key']:i for i in snapshot['items']}
        self.assertEqual(250,snapshot['curatedRevisions'])
        for item in curated:self.assertEqual(item,indexed[item['origin']['key']])

    def test_known_superficial_variants_are_signalled(self):
        cases=[
          ('M09','Uma horta retangular de7 por4 metros tem qual perímetro?','Qual é o perímetro de um retângulo de7 por4 centímetros?'),
          ('M09','Qual a área de um triângulo de base10 e altura7?','Qual a área de um triângulo de base10 e altura6?'),
          ('M10','Uma sequência começa em3 e cada termo é o dobro do anterior. Qual é o próximo termo?','Uma sequência começa em1 e cada termo é o dobro do anterior. Qual é o próximo termo?'),
          ('I05','O endereço C7 indica qual linha e coluna?','O endereço C5 identifica qual linha e coluna?'),
          ('M03','Quatro pessoas fazem um serviço em6 dias. Quantos dias levariam3 pessoas?','Cinco trabalhadores fazem um serviço em8 dias. Quantos dias levariam4 trabalhadores?')]
        def item(key,topic,text):return {'origin':{'key':key},'content':{'statement':{'text':text},'classification':{'topicId':topic}}}
        for topic,a,b in cases:
            result=lp.audit_duplicates({'items':[item('own',topic,a)]},{'items':[item('old',topic,b)]})
            self.assertTrue(result['directIntentMatches'],topic)

    def test_excel_references_are_canonicalized_after_casefold_in_real_audit(self):
        def item(key,text):return {'origin':{'key':key},'content':{'statement':{'text':text},'classification':{'topicId':'I05'}}}
        practice={'items':[item('own','Qual o conteúdo do endereço AZ123 nesta seleção?')]}
        old={'items':[item('old','Qual o conteúdo do endereço B9 nesta seleção?')]}
        audit=lp.audit_duplicates(practice,old)
        self.assertIn(['own','old'],audit['numericTemplateMatches'])
        self.assertEqual('celula',lp.numeric_template(lp.normalized('AZ123')))

    def test_alternative_ids_follow_text_when_authored_order_and_answer_index_change(self):
        import shutil,uuid
        original,_,_=lp.compile_practice(ROOT)
        first=original['items'][0]
        mapping=lambda item:{a['content']['text']:a['id'] for a in item['content']['alternatives']}
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);shutil.copytree(ROOT/lp.DIRECTORY,root/lp.DIRECTORY)
            file=root/lp.DIRECTORY/'portugues.jsonl';rows=[json.loads(l) for l in file.read_text(encoding='utf-8').splitlines()]
            row=rows[0];correct=row['alternatives'][row['correctIndex']]
            row['alternatives']=[row['alternatives'][i] for i in [3,1,0,2]];row['correctIndex']=row['alternatives'].index(correct)
            file.write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in rows),encoding='utf-8')
            rearranged,_,_=lp.compile_practice(root)
        self.assertEqual(mapping(first),mapping(rearranged['items'][0]))
        self.assertEqual(first['content']['correctAlternativeId'],rearranged['items'][0]['content']['correctAlternativeId'])
        positional_guess=str(uuid.uuid5(uuid.NAMESPACE_URL,lp.NAMESPACE+'/'+first['origin']['key']+'/0'))
        self.assertNotIn(positional_guess,mapping(first).values())

    def test_duplicate_triage_finds_exact_and_similar_stems(self):
        pack,_,_=lp.compile_practice(ROOT);legacy=copy.deepcopy(pack);legacy['items']=legacy['items'][:1]
        self.assertEqual(1,len(lp.audit_duplicates(pack,legacy)['exactLegacy']))

if __name__=='__main__':unittest.main()
