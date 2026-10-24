"""Compile 255 candidatos próprios de prática; sem rede, DB ou aprovação implícita."""
import argparse
import ast
import hashlib
import json
import math
import operator
import re
import unicodedata
import uuid
from collections import Counter
from difflib import SequenceMatcher
from fractions import Fraction
from pathlib import Path
from question_package import CATALOG, ROLES, fingerprint, safe_url

DIRECTORY = Path('editorial/lesson-practice/2026-10')
FILES = {'portugues': 'P', 'matematica': 'M', 'informatica': 'I', 'administrativo': 'A', 'escolar': 'S'}
NAMESPACE = 'lesson-practice-v1'
AUTHOR = 'Codex — elaboração editorial assistida por IA, sem revisão humana independente'

def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode('utf-8')).hexdigest()

def calculate(expression):
    """Oracle aritmético limitado: sem eval, atributos, variáveis ou chamadas abertas."""
    if len(expression) > 200: raise ValueError('Expressão extensa')
    tree = ast.parse(expression, mode='eval')
    if len(list(ast.walk(tree))) > 80: raise ValueError('Expressão complexa')
    def visit(n):
        if isinstance(n, ast.Constant) and type(n.value) is int and abs(n.value) <= 10**9: return Fraction(n.value)
        if isinstance(n, ast.UnaryOp) and isinstance(n.op, (ast.UAdd, ast.USub)):
            return visit(n.operand) * (-1 if isinstance(n.op, ast.USub) else 1)
        if isinstance(n, ast.BinOp) and type(n.op) in {ast.Add, ast.Sub, ast.Mult, ast.Div}:
            result = {ast.Add:operator.add, ast.Sub:operator.sub, ast.Mult:operator.mul, ast.Div:operator.truediv}[type(n.op)](visit(n.left),visit(n.right))
            if abs(result) > 10**12: raise ValueError('Resultado fora do limite')
            return result
        if isinstance(n, ast.Call) and isinstance(n.func, ast.Name) and not n.keywords:
            args = [visit(a) for a in n.args]
            if any(a.denominator != 1 or abs(a) > 10**9 for a in args): raise ValueError('Argumentos não inteiros')
            args = [int(a) for a in args]
            if n.func.id in {'gcd','lcm'} and len(args)==2:
                return Fraction((math.gcd if n.func.id=='gcd' else math.lcm)(*args))
            if n.func.id=='sqrt' and len(args)==1 and args[0]>=0:
                root=math.isqrt(args[0])
                if root*root==args[0]: return Fraction(root)
        raise ValueError('Operação não permitida')
    return visit(tree.body)

def answer_number(text):
    m=re.search(r'[−-]?\d+(?:[.,]\d+)*(?:/\d+)?',text)
    if not m: raise ValueError('Resposta numérica ausente')
    return Fraction(m[0].replace('−','-').replace('.','').replace(',','.'))

def normalized(text):
    return ' '.join(re.findall(r'\w+',unicodedata.normalize('NFKC',text).casefold()))

def compile_practice(root):
    root=Path(root).resolve(); directory=root/DIRECTORY
    refs=json.loads((directory/'sources.json').read_text(encoding='utf-8'))
    sources=[];items=[];rows=[];seen=set();numeric=[]
    for name,prefix in FILES.items():
        relative=(DIRECTORY/(name+'.jsonl')).as_posix();path=root/relative
        if path.is_symlink():raise ValueError('Fonte não pode ser symlink')
        data=path.read_bytes();sha=hashlib.sha256(data).hexdigest();sources.append({'path':relative,'sha256':sha})
        for line in data.decode('utf-8').splitlines():
            row=json.loads(line)
            if set(row)-{'key','topicId','statement','alternatives','correctIndex','resolution','sources','provision','checkExpr'}:raise ValueError('Campo desconhecido')
            key=row['key'];topic=row['topicId']
            if topic not in CATALOG or not topic.startswith(prefix) or key in seen or not re.fullmatch(re.escape(topic)+r'-P0[1-5]',key):raise ValueError('Origem inválida: '+key)
            seen.add(key)
            if len(row['alternatives'])!=4 or len({' '.join(unicodedata.normalize('NFKC',x).split()).casefold() for x in row['alternatives']})!=4 or type(row['correctIndex']) is not int or not 0<=row['correctIndex']<4:raise ValueError('Alternativas inválidas: '+key)
            if not row['statement'].strip() or not row['resolution'].startswith('1. ') or '2. ' not in row['resolution']:raise ValueError('Enunciado/resolução inválidos: '+key)
            if 'checkExpr' in row:
                expected=calculate(row['checkExpr']); actual=answer_number(row['alternatives'][row['correctIndex']])
                if expected!=actual: raise ValueError(f'Cálculo divergente {key}: {expected} != {actual}')
                numeric.append({'key':key,'expression':row['checkExpr'],'result':str(expected)})
            references=[]
            if not row['sources']:raise ValueError('Sem referência: '+key)
            for refkey in row['sources']:
                ref=refs[refkey];basis=None
                if ref['legal'] and row.get('provision'):basis={'provision':row['provision'],'checkedOn':ref['verifiedOn']}
                references.append({'name':ref['name'],'url':safe_url(ref['url']),'legalBasis':basis})
            roleids=list(ROLES) if prefix in 'PMI' else [ROLES[0 if prefix=='A' else 1]]
            rich=lambda value:{'text':value,'resources':[]}
            options=[{'id':str(uuid.uuid5(uuid.NAMESPACE_URL,f'{NAMESPACE}/{key}/text/{hashlib.sha256(x.encode("utf-8")).hexdigest()}')),'content':rich(x)} for x in row['alternatives']]
            content={'statement':rich(row['statement']),'alternatives':options,'correctAlternativeId':options[row['correctIndex']]['id'],'resolution':rich(row['resolution']),
                'classification':{'disciplineId':CATALOG[topic],'topicId':topic,'roleIds':roleids},'references':references,
                'provenance':{'author':AUTHOR,'contentPath':relative,'answerPath':relative,'contentSha256':sha,'answerSha256':sha}}
            items.append({'origin':{'namespace':NAMESPACE,'key':key},'content':content,'fingerprint':fingerprint(content),'expectedLatestRevision':None});rows.append(row)
    expected={f'{t}-P0{i}' for t in CATALOG for i in range(1,6)}
    if seen!=expected:raise ValueError('Lote exige cinco questões por cada um dos51 assuntos')
    counts=Counter(normalized(r['statement']) for r in rows)
    if any(c>1 for c in counts.values()):raise ValueError('Enunciados próprios repetidos')
    pack={'format':'lesson-practice-package/v1','catalog':[{'topicId':t,'disciplineId':d} for t,d in CATALOG.items()],
          'sourceFiles':sources,'inventory':[{'key':r['key'],'status':'candidate','legalBasisPending':None,'editorialReviewRequired':True} for r in rows],
          'items':items,'issues':[],'possibleDuplicates':[]}
    return pack,rows,numeric

def comparison_snapshot(root):
    """Compara também o conteúdo curado atual, não somente o HTML de origem."""
    from curate_questions import compile_curation
    original, curated, *_ = compile_curation(root)
    current = {item['origin']['key']: item for item in original['items']}
    current.update({item['origin']['key']: item for item in curated})
    return {**original, 'items': list(current.values()), 'curatedRevisions': len(curated)}

def numeric_template(text):
    text = re.sub(r'Microsoft\s*365|Windows\s*11', '', text, flags=re.I)
    text = re.sub(r'\b[A-Z]{1,3}[0-9]+\b', 'CELULA', text, flags=re.I)
    return re.sub(r'\d+(?:[.,/]\d+)*', 'N', normalized(text))

def direct_intent(text, topic):
    # Sinais de perguntas diretas conhecidas; não são um classificador semântico.
    t = ''.join(c for c in unicodedata.normalize('NFKD',text.casefold()) if not unicodedata.combining(c))
    if topic=='M09' and 'perimetro' in t and 'retang' in t: return 'rectangle-perimeter'
    if topic=='M09' and 'area' in t and 'triang' in t and not any(w in t for w in ('retir','restant','remanesc')): return 'triangle-area'
    if topic=='M10' and 'dobro' in t and 'proximo' in t: return 'double-next'
    if topic=='I05' and 'endereco' in t and any(w in t for w in ('indica','identifica')): return 'cell-address'
    if topic=='M03' and re.search(r'pesso|funcion|trabalhad|servidor|equipe',t) and re.search(r'dias?|horas?',t) and not any(w in t for w in ('apos','depois','adicion','saem','restant')):
        return 'single-stage-work-time'
    return None

def audit_duplicates(pack, legacy):
    """Triagem textual, não certificado semântico de originalidade."""
    exact=[];near=[]; internal=[];templates=[];intents=[]
    texts=[(i['origin']['key'],normalized(i['content']['statement']['text']), i['content']['classification']['topicId']) for i in pack['items']]
    old=[(i['origin']['key'],normalized(i['content']['statement']['text']), i['content']['classification']['topicId']) for i in legacy['items']]
    for k,s,t in texts:
        for oldk,olds,oldt in old:
            if s==olds:exact.append([k,oldk])
            elif t==oldt and SequenceMatcher(None,s,olds,autojunk=False).ratio()>=.75:near.append([k,oldk])
            if t==oldt:
                if SequenceMatcher(None,numeric_template(s),numeric_template(olds),autojunk=False).ratio()>=.55:templates.append([k,oldk])
                intent=direct_intent(s,t)
                if intent and intent==direct_intent(olds,t):intents.append([k,oldk,intent])
    for index,(k,s,t) in enumerate(texts):
        for other,others,othert in texts[:index]:
            if t==othert and SequenceMatcher(None,s,others,autojunk=False).ratio()>=.75:internal.append([k,other])
    return {'exactLegacy':exact,'similarLegacy':near,'similarWithinPractice':internal,'threshold':.75,'legacyCandidates':len(old),'curatedRevisions':legacy.get('curatedRevisions',0),'numericTemplateMatches':templates,'numericTemplateThreshold':.55,'directIntentMatches':intents,
        'limits':'Triagem de texto; não prova ausência de equivalência semântica. Pares sinalizados exigem leitura editorial.'}

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--root',default='.');p.add_argument('--output',default='backend/build/lesson-practice/editorial');args=p.parse_args()
    root=Path(args.root).resolve();out=root/args.output;out.mkdir(parents=True,exist_ok=True)
    pack,rows,numeric=compile_practice(root)
    duplicates=audit_duplicates(pack,comparison_snapshot(root))
    if duplicates['exactLegacy']:raise ValueError('Questões iguais ao legado')
    selection=[{'key':i['origin']['key'],'fingerprint':i['fingerprint']} for i in pack['items']]
    report={'status':'draft-not-approved','count':len(rows),'coverage':dict(sorted(Counter(r['topicId'] for r in rows).items())),
        'selectionDigest':digest(selection),'packageDigest':digest(pack),'numericChecks':numeric,'duplicates':duplicates}
    for name,value in [('package.json',pack),('audit.json',report),('selection.json',selection)]:
        (out/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    parts=['# Seleção editorial de prática — rascunho','',f'255 questões, cinco por assunto. Digest da seleção: `{report["selectionDigest"]}`.',
        f'Digest canônico do pacote: `{report["packageDigest"]}`. Nenhum item aprovado ou importado por este compilador.',
        'Elaboração assistida por IA; sem revisão humana independente. Referências de estudo apoiam conceitos, não atribuem autoria das questões às instituições.',
        'Questões situacionais não criam regras municipais. Os cálculos indicados passaram pelo oráculo aritmético; isso não certifica toda interpretação.','']
    for r in rows:
        item=next(i for i in pack['items'] if i['origin']['key']==r['key'])
        parts += [f'## {r["key"]}', '',r['statement'],'']+[f'{chr(65+i)}. {v}' for i,v in enumerate(r['alternatives'])]+['',f'Correta: {chr(65+r["correctIndex"])}. {r["resolution"]}','']
        parts += [f'Fonte: [{ref["name"]}]({ref["url"]})'+(' — '+ref['legalBasis']['provision'] if ref['legalBasis'] else '') for ref in item['content']['references']]
        parts += ['',f'Fingerprint: `{item["fingerprint"]}`','']
    (out/'content-selection.md').write_text('\n'.join(parts),encoding='utf-8',newline='\n')
    print(json.dumps({'items':len(rows),'topics':len(report['coverage']),'numericChecks':len(numeric),'duplicates':duplicates,'selectionDigest':report['selectionDigest']},ensure_ascii=False))

if __name__=='__main__':main()
