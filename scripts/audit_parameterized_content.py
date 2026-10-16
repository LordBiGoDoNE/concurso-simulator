"""Audite prévias Java sem banco/rede/escrita editorial; aprovação continua explícita."""
import argparse
import hashlib
import itertools
import json
import struct
from fractions import Fraction
from pathlib import Path

DOMAINS = {
    'DIRECT_PROPORTION': {'a': [2, 3, 4, 5], 'b': [6, 7, 8, 9], 'u': [2, 3, 5, 7]},
    'PERCENTAGE': {'p': [5, 10, 15, 20, 25], 'v': [100, 200, 400, 800]},
    'SIMPLE_INTEREST': {'c': [1000, 2000, 4000], 'i': [1, 2, 3], 't': [2, 3, 6]},
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(fields):
    data = struct.pack('>I', len(fields)) + b''.join(struct.pack('>I', len(b)) + b for b in (v.encode('utf-8') for v in fields))
    return hashlib.sha256(data).hexdigest()


def key(family, values):
    return family + ':[' + ', '.join(f'{k}={values[k]}' for k in sorted(values)) + ']'


def definition_digest(d, domain_digest):
    fields = ['parameter-template/v1', d['id'], str(d['version']), d['family'], d['generatorVersion'],
              d['baseQuestionId'], d['baseRevisionId'], d['baseFingerprint'], domain_digest,
              'matematica-raciocinio', d['classification']['topic']]
    roles = {'ADMINISTRATIVE_ASSISTANT': 'assistente-administrativo', 'SCHOOL_SECRETARY': 'secretario-de-escola'}
    require(d['classification']['discipline'] == 'MATHEMATICS_LOGIC'
            and set(d['classification']['roles']) == set(roles), 'Classificação incompatível')
    fields += sorted(roles[r] for r in d['classification']['roles']) + [str(len(d['references']))]
    for reference in d['references']:
        legal = reference['legalBasis']; fields += [reference['name'], reference['url'], str(legal is not None).lower()]
        if legal:
            fields += [legal['provision'], legal['checkedOn']]
    fields += [d['provenance'][k] for k in ['author', 'contentPath', 'answerPath', 'contentSha256', 'answerSha256']]
    return digest(fields)


def same_classification(a, b):
    return a['discipline'] == b['discipline'] and a['topic'] == b['topic'] and set(a['roles']) == set(b['roles'])


def audit_variant(variant):
    p = variant['parameters']['values']; family = variant['parameters']['family']
    require(family in DOMAINS and p.keys() == DOMAINS[family].keys(), 'Família/campos externos')
    require(all(type(v) is int and v in DOMAINS[family][k] for k, v in p.items()), 'Tupla externa')
    values = variant['alternatives']; index = variant['correctIndex']
    require(len(values) == 4 and all(type(v) is int and v > 0 for v in values)
            and len(set(values)) == 4 and type(index) is int and 0 <= index < 4, 'Alternativas inválidas')
    # Oráculo racional separado do gerador e do oráculo Java; não usa índices do manifesto.
    if family == 'DIRECT_PROPORTION':
        unit = Fraction(p['a'] * p['u'], p['a']); answer = sum(unit for _ in range(p['b']))
        calculation = f"{p['a'] * p['u']} / {p['a']} = {unit}; {p['b']} unidades = {answer} reais"
    elif family == 'PERCENTAGE':
        hundredth = Fraction(p['v'], 100); answer = sum(hundredth for _ in range(p['p']))
        calculation = f"1% = {hundredth}; {p['p']}% = {answer} reais"
    else:
        monthly = Fraction(p['c'], 100) * p['i']; answer = sum(monthly for _ in range(p['t']))
        calculation = f"juros mensais = {monthly}; {p['t']} meses = {answer} reais; montante = {p['c'] + answer}"
    require(answer.denominator == 1 and values[index] == answer, 'Resposta diverge do oráculo independente')
    statement = variant['statement']; resolution = variant['resolution']
    require(statement['text'].strip() and resolution['text'].strip(), 'Texto vazio')
    require(not statement['resources'] and not resolution['resources'], 'Recurso inesperado numeric-v1')
    require('R$ ' in statement['text'] and f'R$ {answer},00' in resolution['text'], 'Unidades/resultado ausentes')
    if family == 'SIMPLE_INTEREST':
        require('% ao mês' in statement['text'] and 'meses' in statement['text']
                and 'montante' in resolution['text'], 'Juros/tempo/montante ambíguos')
    return calculation


def audit(directory, allow_partial=False, allow_pending_bases=False):
    directory = Path(directory); root = Path(__file__).resolve().parents[1]
    files = sorted(directory.glob('*-preview.json'))
    bases = json.loads((directory / 'bases.json').read_text(encoding='utf-8'))
    seen_bases = set(); pending_bases = []
    families = {}; report = ['# Prévia editorial de variantes', '',
        '**Esta auditoria não concede aprovação editorial. Decisões registradas constam de eligibility-after.json.**', '',
        'Resultado do oráculo Python racional; conferir também linguagem, distratores, fontes e acessibilidade.', '']
    for file in files:
        preview = json.loads(file.read_text(encoding='utf-8'))
        version = preview['version']; d = version['definition']; manifest = version['manifest']; family = d['family']
        require(family in DOMAINS and family not in families, 'Família ausente/desconhecida/repetida')
        definition = json.loads(file.with_name(file.name.replace('-preview', '')).read_text(encoding='utf-8'))
        input_definition = dict(definition['definition']); preview_definition = dict(d)
        input_classification = input_definition.pop('classification'); preview_classification = preview_definition.pop('classification')
        require(definition['format'] == 'parameter-template/v1' and input_definition == preview_definition
                and same_classification(input_classification, preview_classification) and definition['expectedLatest'] == 0,
                'Arquivo de definição divergente')
        base = next((b for b in bases if b['family'] == family), None)
        require(base is not None and base['question_id'] not in seen_bases
                and base['question_id'] == d['baseQuestionId'] and base['revision_id'] == d['baseRevisionId']
                and base['fingerprint'] == d['baseFingerprint'] and same_classification(base['payload']['classification'], d['classification']),
                'Observação de base incompatível; não inventar elegibilidade')
        eligible = base['status'] == 'APPROVED' and base['eligible_revision_id'] == base['revision_id']
        if not eligible:
            require(allow_pending_bases and base['status'] in ['DRAFT', 'IN_REVIEW'] and base['eligible_revision_id'] is None,
                    'Base pendente de aprovação editorial: ' + family)
            pending_bases.append(family)
        seen_bases.add(base['question_id'])
        for path_key, hash_key in [('contentPath', 'contentSha256'), ('answerPath', 'answerSha256')]:
            source = (root / d['provenance'][path_key]).resolve()
            require(source.is_relative_to(root) and source.is_file()
                    and hashlib.sha256(source.read_bytes()).hexdigest() == d['provenance'][hash_key], 'Proveniência divergente')
        names = sorted(DOMAINS[family]); domain = [dict(zip(names, v)) for v in itertools.product(*(DOMAINS[family][k] for k in names))]
        variants = preview['variants']; require(len(domain) == len(variants) == len(manifest['entries']), 'Domínio incompleto')
        dd = digest(['parameter-domain/v1', family, *(key(family, p) for p in domain)])
        require(dd == preview['domainDigest'] == manifest['domainDigest'], 'Digest de domínio divergente')
        require(preview['definitionDigest'] == manifest['definitionDigest'], 'Digest de definição divergente')
        require(definition_digest(d, dd) == preview['definitionDigest'], 'Definição não corresponde ao digest')
        require(d['generatorVersion'] == manifest['generatorVersion'] == 'numeric-v1', 'Gerador desconhecido')
        fields = ['parameter-manifest/v1', preview['definitionDigest'], dd, 'numeric-v1', str(len(domain))]
        report += [f'## {family} — {len(domain)} variantes', '',
                   f"Base: `{d['baseQuestionId']}` / revisão `{d['baseRevisionId']}`.", '',
                   'Estado observado da base: ' + base['status'] + ('; elegível.' if eligible else '; NÃO ELEGÍVEL, aprovação pendente.'), '',
                   f"Definição: `{preview['definitionDigest']}`. Manifesto: `{preview['manifestDigest']}`.", '']
        for number, (p, v, entry) in enumerate(zip(domain, variants, manifest['entries']), 1):
            require(v['definition'] == d and v['parameters'] == entry['parameters']
                    == {'family': family, 'values': p}, 'Tupla/ordem/definição divergente')
            calculation = audit_variant(v)
            od = digest(['parameter-output/v1', preview['definitionDigest'], key(family, p), v['statement']['text'],
                         str(v['correctIndex']), *map(str, v['alternatives']), v['resolution']['text']])
            require(entry['outputDigest'] == od, 'Digest de saída divergente')
            fields += [key(family, p), od]
            report += [f"### {number}. `{key(family, p)}`", '', v['statement']['text'], '',
                       ' | '.join(f"{chr(65+i)}: R$ {value},00" for i, value in enumerate(v['alternatives'])), '',
                       f"Correta editorial: {chr(65+v['correctIndex'])}. Oráculo: {calculation}.", '',
                       v['resolution']['text'], '', f'Digest da saída: `{od}`.', '']
        require(digest(fields) == preview['manifestDigest'], 'Digest do manifesto divergente')
        families[family] = len(domain)
    missing = sorted(set(DOMAINS) - families.keys())
    require(allow_partial or not missing, 'Famílias pendentes: ' + ', '.join(missing))
    summary = {'format': 'parameter-editorial-audit/v1', 'expected': 111, 'validated': sum(families.values()),
               'families': families, 'missingFamilies': missing, 'editorialApproval': 'NOT_GRANTED_BY_AUDIT',
               'pendingBaseFamilies': sorted(pending_bases), 'allBasesEligible': not pending_bases,
               'scope': 'Oráculo aritmético/tuplas/digests/unidades; não certifica revisão humana ou elegibilidade futura.'}
    return summary, '\n'.join(report) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', required=True); parser.add_argument('--output', required=True)
    parser.add_argument('--allow-partial', action='store_true', help='Registre lacuna explicitamente, sem declarar 111/111')
    parser.add_argument('--allow-pending-bases', action='store_true', help='Somente preparação: identifique bases não elegíveis, sem aprová-las')
    args = parser.parse_args(); summary, report = audit(args.directory, args.allow_partial, args.allow_pending_bases)
    out = Path(args.output); out.mkdir(parents=True, exist_ok=True)
    (out / 'variants.md').write_text(report, encoding='utf-8', newline='\n')
    (out / 'audit.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == '__main__':
    main()
