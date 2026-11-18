"""Read-only coverage matrix: what the question bank demands vs. what each lesson explains.

The curated concept list says, per topic, which concepts the questions demand and
which text patterns show that the lesson explains them. This script only reads the
question packages and the compiled lessons; it never changes lessons or questions.
"""
import argparse
import json
import re
import sys
from pathlib import Path

CONCEPTS = 'editorial/lesson-coverage/2026-11/concepts.jsonl'
PACKAGES = 'editorial/question-bank-expansion/2026-11/packages'
LESSONS = 'frontend/src/lesson-documents'
CATALOG = 'frontend/src/lesson-catalog.json'
REPORT = 'docs/lesson-coverage.md'
PURPOSES = ('SIMULADO', 'PRATICA_AULA')
KINDS = ('termo', 'dispositivo', 'comando', 'procedimento', 'calculo')
FIELDS = {'id', 'topicId', 'concept', 'kind', 'patterns', 'questionKeys', 'outOfScope'}
COVERED, ABSENT, OUT_OF_SCOPE = 'coberto', 'ausente', 'fora de escopo'


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8'))


def load_topics(root):
    """Topic id -> title, in catalog order."""
    return {topic['id']: topic['title'] for topic in read_json(root / CATALOG)['topics']}


def load_questions(root):
    """Question key -> {topicId, purpose, statement, alternatives, correct, resolution, provisions}."""
    questions = {}
    for purpose in PURPOSES:
        for path in sorted((root / PACKAGES / purpose).glob('*/preview-request.json')):
            for item in read_json(path)['package']['items']:
                content = item['content']
                correct = content['correctAlternativeId']
                questions[item['origin']['key']] = {
                    'topicId': content['classification']['topicId'],
                    'purpose': purpose,
                    'statement': content['statement']['text'],
                    'alternatives': [a['content']['text'] for a in content['alternatives']],
                    'correct': next((a['content']['text'] for a in content['alternatives']
                                     if a['id'] == correct), ''),
                    'resolution': content['resolution']['text'],
                    'provisions': [r['legalBasis']['provision'] for r in content.get('references', [])
                                   if r.get('legalBasis') and r['legalBasis'].get('provision')],
                }
    return questions


INLINE = set('span strong b em i small code var sub sup a text tspan'.split())


def flatten(node, out):
    """Inline markup keeps words intact; every other element separates its text."""
    if isinstance(node, str):
        out.append(node)
    elif isinstance(node, dict):
        inline = node.get('tag') in INLINE
        if not inline:
            out.append(' ')
        for child in node.get('children', []):
            flatten(child, out)
        if not inline:
            out.append(' ')
    return out


def lesson_text(root, topic):
    """Whitespace-normalised teaching text of a compiled lesson."""
    parts = flatten(read_json(root / LESSONS / (topic + '.json')), [])
    return re.sub(r'\s+', ' ', ''.join(parts)).strip()


def load_concepts(root):
    """Parsed concept lines plus structural problems found while reading them."""
    path = root / CONCEPTS
    concepts, problems = [], []
    if not path.is_file():
        return concepts, [CONCEPTS + ': arquivo ausente']
    for number, line in enumerate(path.read_text(encoding='utf-8').splitlines(), 1):
        if not line.strip():
            continue
        try:
            concept = json.loads(line)
        except json.JSONDecodeError as error:
            problems.append('linha %d: JSON inválido (%s)' % (number, error.msg))
            continue
        if not isinstance(concept, dict):
            problems.append('linha %d: esperado um objeto' % number)
            continue
        concept['line'] = number
        concepts.append(concept)
    return concepts, problems


def validate(concepts, topics, questions):
    """Structural problems of the curated list; an empty result means it is usable."""
    problems, seen = [], set()
    for concept in concepts:
        where = 'linha %d (%s)' % (concept['line'], concept.get('id', '?'))
        unknown = set(concept) - FIELDS - {'line'}
        if unknown:
            problems.append('%s: campos desconhecidos %s' % (where, sorted(unknown)))
        identifier, topic = concept.get('id'), concept.get('topicId')
        if not isinstance(identifier, str) or not identifier:
            problems.append(where + ': id ausente')
        elif identifier in seen:
            problems.append(where + ': id repetido')
        seen.add(identifier)
        if topic not in topics:
            problems.append('%s: assunto desconhecido %r' % (where, topic))
        elif isinstance(identifier, str) and not identifier.startswith(topic + '-'):
            problems.append(where + ': id deve começar pelo assunto')
        if not isinstance(concept.get('concept'), str) or not concept.get('concept', '').strip():
            problems.append(where + ': conceito sem nome')
        if concept.get('kind') not in KINDS:
            problems.append('%s: kind inválido %r' % (where, concept.get('kind')))
        patterns = concept.get('patterns')
        if not isinstance(patterns, list) or not patterns:
            problems.append(where + ': patterns vazio')
        else:
            for pattern in patterns:
                try:
                    if not isinstance(pattern, str) or not pattern:
                        raise re.error('vazio')
                    re.compile(pattern)
                except re.error:
                    problems.append('%s: padrão inválido %r' % (where, pattern))
        keys = concept.get('questionKeys')
        if not isinstance(keys, list) or not keys:
            problems.append(where + ': questionKeys vazio')
        else:
            for key in keys:
                if key not in questions:
                    problems.append('%s: questão inexistente %r' % (where, key))
                elif questions[key]['topicId'] != topic:
                    problems.append('%s: questão %r é de outro assunto' % (where, key))
        if 'outOfScope' in concept and (not isinstance(concept['outOfScope'], str)
                                        or not concept['outOfScope'].strip()):
            problems.append(where + ': outOfScope sem justificativa')
    mapped = {key for concept in concepts for key in concept.get('questionKeys') or []
              if isinstance(key, str)}
    for key in sorted(set(questions) - mapped):
        problems.append('questão sem conceito: %s (%s)' % (key, questions[key]['topicId']))
    legal = {key for concept in concepts if concept.get('kind') == 'dispositivo'
             for key in concept.get('questionKeys') or [] if isinstance(key, str)}
    for key in sorted(mapped - legal):
        if key in questions and questions[key]['provisions']:
            problems.append('questão com base legal sem conceito dispositivo: %s (%s)'
                            % (key, questions[key]['topicId']))
    return problems


def is_covered(concept, text):
    """Every pattern must be present; alternation inside a pattern expresses 'any of'."""
    return all(re.search(pattern, text, re.IGNORECASE) for pattern in concept['patterns'])


def evaluate(root, concepts, topics):
    """Rows per topic, in catalog order: (concept, status)."""
    matrix = {topic: [] for topic in topics}
    texts = {}
    for concept in concepts:
        topic = concept['topicId']
        if topic not in texts:
            texts[topic] = lesson_text(root, topic)
        if 'outOfScope' in concept:
            status = OUT_OF_SCOPE
        else:
            status = COVERED if is_covered(concept, texts[topic]) else ABSENT
        matrix[topic].append((concept, status))
    return matrix


def count(rows, status):
    return sum(1 for _, row_status in rows if row_status == status)


def render(matrix, topics, questions):
    per_topic = {topic: 0 for topic in topics}
    for question in questions.values():
        per_topic[question['topicId']] += 1
    rows = [row for topic_rows in matrix.values() for row in topic_rows]
    lines = [
        '# Cobertura das aulas em relação ao banco de questões',
        '',
        'Gerado por `python scripts/lesson_coverage.py --write-report`. Não editar à mão.',
        '',
        'Fonte dos conceitos: `%s`. Um conceito está **coberto** quando todos os seus' % CONCEPTS,
        'padrões aparecem no texto da aula compilada; isso indica presença, não qualidade',
        'da explicação.',
        '',
        'Este relatório mostra só o estado atual. Histórico e limites da cobertura:',
        '`editorial/lesson-coverage/2026-11/README.md`.',
        '',
        '| Total | Questões | Conceitos | Cobertos | Ausentes | Fora de escopo |',
        '| --- | ---: | ---: | ---: | ---: | ---: |',
        '| %d assuntos | %d | %d | %d | %d | %d |' % (
            len(topics), len(questions), len(rows), count(rows, COVERED), count(rows, ABSENT),
            count(rows, OUT_OF_SCOPE)),
        '',
        '## Por assunto',
        '',
        '| Assunto | Título | Questões | Conceitos | Cobertos | Ausentes | Fora de escopo |',
        '| --- | --- | ---: | ---: | ---: | ---: | ---: |',
    ]
    for topic, title in topics.items():
        topic_rows = matrix[topic]
        lines.append('| %s | %s | %d | %d | %d | %d | %d |' % (
            topic, title, per_topic[topic], len(topic_rows), count(topic_rows, COVERED),
            count(topic_rows, ABSENT), count(topic_rows, OUT_OF_SCOPE)))
    for heading, status in (('Conceitos ausentes', ABSENT), ('Conceitos fora de escopo', OUT_OF_SCOPE)):
        lines += ['', '## ' + heading, '']
        found = False
        for topic in topics:
            selected = [concept for concept, row_status in matrix[topic] if row_status == status]
            if not selected:
                continue
            found = True
            lines += ['### %s — %s' % (topic, topics[topic]), '']
            for concept in selected:
                detail = '%d questões' % len(concept['questionKeys'])
                if status == OUT_OF_SCOPE:
                    detail += '; ' + concept['outOfScope'].strip()
                lines.append('- %s (%s; %s)' % (concept['concept'], concept['kind'], detail))
            lines.append('')
        if not found:
            lines += ['Nenhum.', '']
    return '\n'.join(lines).rstrip('\n') + '\n'


def print_questions(questions, topic, out):
    for key, question in questions.items():
        if question['topicId'] != topic:
            continue
        out.write('### %s [%s]\n%s\n' % (key, question['purpose'], question['statement']))
        for alternative in question['alternatives']:
            mark = '*' if alternative == question['correct'] else '-'
            out.write('  %s %s\n' % (mark, alternative))
        out.write('Resolução: %s\n' % question['resolution'])
        if question['provisions']:
            out.write('Base legal: %s\n' % '; '.join(question['provisions']))
        out.write('\n')


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--check', action='store_true',
                      help='falha se a lista for inválida, o relatório estiver desatualizado '
                           'ou houver conceito ausente sem justificativa')
    mode.add_argument('--check-structure', action='store_true',
                      help='falha se a lista for inválida ou o relatório estiver desatualizado')
    mode.add_argument('--write-report', action='store_true', help='grava ' + REPORT)
    mode.add_argument('--questions', metavar='ASSUNTO',
                      help='lista as questões de um assunto, para curadoria')
    mode.add_argument('--lesson', metavar='ASSUNTO', help='imprime o texto da aula compilada')
    args = parser.parse_args(argv)
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, 'reconfigure'):
            stream.reconfigure(encoding='utf-8')
    root = args.root.resolve()
    topics = load_topics(root)
    if args.lesson or args.questions:
        topic = args.lesson or args.questions
        if topic not in topics:
            parser.error('assunto desconhecido: ' + topic)
        if args.lesson:
            print(lesson_text(root, topic))
        else:
            print_questions(load_questions(root), topic, sys.stdout)
        return 0
    questions = load_questions(root)
    concepts, problems = load_concepts(root)
    problems += validate(concepts, topics, questions)
    if problems:
        for problem in problems:
            print('ERRO: ' + problem, file=sys.stderr)
        print('%d problema(s) na lista de conceitos.' % len(problems), file=sys.stderr)
        return 1
    matrix = evaluate(root, concepts, topics)
    report = render(matrix, topics, questions)
    if args.write_report:
        (root / REPORT).parent.mkdir(parents=True, exist_ok=True)
        (root / REPORT).write_text(report, encoding='utf-8', newline='\n')
        print('Relatório gravado em ' + REPORT)
        return 0
    rows = [row for topic_rows in matrix.values() for row in topic_rows]
    print('%d conceitos: %d cobertos, %d ausentes, %d fora de escopo.' % (
        len(rows), count(rows, COVERED), count(rows, ABSENT), count(rows, OUT_OF_SCOPE)))
    failed = False
    if args.check or args.check_structure:
        path = root / REPORT
        current = path.read_text(encoding='utf-8').replace('\r\n', '\n') if path.is_file() else None
        if current != report:
            print('ERRO: %s desatualizado; rode --write-report.' % REPORT, file=sys.stderr)
            failed = True
    if args.check:
        for topic in topics:
            for concept, status in matrix[topic]:
                if status == ABSENT:
                    print('ERRO: %s: conceito ausente na aula: %s' % (topic, concept['concept']),
                          file=sys.stderr)
                    failed = True
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main())
