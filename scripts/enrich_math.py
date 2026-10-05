"""Generate static beginner-friendly M01–M09 pages from preserved V3 sources.

Used by build_pages.py. The output contains complete HTML, not JS or remote math.
M04 is already hand-authored and is copied unchanged.
"""

import argparse
from html import escape
from pathlib import Path
import re
import shutil

from math_content import FORMULAS, LESSONS, PRACTICE, SOLUTIONS


def readable(text, number, expand=True):
    if number == 6 and expand:
        # Expand shorthand in teaching text; preserve original exam-style options.
        text = re.sub(r'\b(\d+)x\b', r'\1 × x', text)
        text = re.sub(r'\b(\d+)\(', r'\1 × (', text)
        text = text.replace('a(b + c)', 'a × (b + c)')
    html = escape(text)
    if number == 6:
        html = re.sub(r'\b([xy])\b', r'<var>\1</var>', html)
    return html


def steps(route, number=0):
    return '<ol class="steps">' + ''.join(f'<li>{readable(step, number)}</li>' for step in route.split('|')) + '</ol>'


def lesson_section(number, index, lesson):
    title, explanation, route, formula, warning = lesson
    flow = '<figure class="flow" aria-label="Caminho do exemplo">'
    flow += ''.join(f'<div>{readable(step, number)}</div>' + ('<span aria-hidden="true">↓</span>' if i < len(route.split('|')) - 1 else '') for i, step in enumerate(route.split('|')))
    flow += '<figcaption>Leia de cima para baixo e refaça cada etapa no papel.</figcaption></figure>'
    return f'<section class="card" id="s{index}"><h2>{index}. {escape(title)}</h2><p>{readable(explanation, number, expand=False)}</p>{flow}<h3>Entenda cada passo</h3>{steps(route, number)}<div class="formula"><strong>Em linha:</strong> {readable(formula, number)}</div><aside class="note"><strong>Atenção:</strong> {readable(warning, number)}</aside></section>'


def answer(number, question, letter):
    route = SOLUTIONS[number][question - 1]
    return f'<div class="inline-resolution"><h4>Resposta: alternativa {letter}</h4><p>Leia o pedido e acompanhe o raciocínio:</p>{steps(route, number)}<p><strong>Confira:</strong> refaça a operação final, verifique as unidades e compare com o que foi pedido.</p></div>'


def enhance(source, output):
    output.mkdir(parents=True, exist_ok=True)
    pages = source / '02_Matematica_Logica'
    target = output / '02_Matematica_Logica'
    target.mkdir(exist_ok=True)
    for number, lessons in LESSONS.items():
        study = next(p for p in pages.glob(f'Matemática {number:02d} - *.html') if 'Gabarito' not in p.name)
        correction = next(p for p in pages.glob(f'Matemática {number:02d} - *Gabarito*.html'))
        text = study.read_text(encoding='utf-8')
        answers = correction.read_text(encoding='utf-8')
        letters = dict((int(q), letter) for q, letter in re.findall(r'Questão (\d+) — alternativa ([A-D])', answers))
        assert len(letters) == len(SOLUTIONS[number]) == 10
        # Fix ambiguous options while retaining the intended answer keys.
        if number == 5:
            text = re.sub(r'(<div class="q" id="q4">.*?)(<li>7,0</li>)', r'\1<li>8</li>', text, count=1)
            assert '<li>7,0</li>' not in text
        if number == 7:
            assert 'ambas A e B' in text
            text = text.replace('ambas A e B', 'ambas B e C', 1)
        sections = ''.join(lesson_section(number, i, lesson) for i, lesson in enumerate(lessons, 1))
        if number == 1:
            definitions = '<p><strong>Os nomes dos conjuntos:</strong> naturais contam (0, 1, 2… nesta convenção); inteiros incluem os negativos; racionais podem ser escritos como fração de inteiros com denominador não nulo (por exemplo 1/4 = 0,25); irracionais não podem (por exemplo √2). Racionais e irracionais formam os reais. Decimal periódico também é racional: 0,333… = 1/3.</p>'
            sections = sections.replace('<h3>Entenda cada passo</h3>', definitions + '<h3>Entenda cada passo</h3>', 1)
        if number == 9:
            sections = sections.replace('quadrado: A = l²;', 'quadrado: P = 4 × l, A = l²;', 1)
        old_sections = re.findall(r'<section class="card" id="s\d+">.*?</section>', text, re.S)
        assert old_sections
        text = text.replace(old_sections[0], sections, 1)
        for section in old_sections[1:]:
            text = text.replace(section, '', 1)
        toc = '<ol class="toc">' + ''.join(f'<li><a href="#s{i}">{i}. {escape(lesson[0])}</a></li>' for i, lesson in enumerate(lessons, 1)) + '</ol>'
        text = re.sub(r'<ol class="toc">.*?</ol>', lambda _: toc, text, count=1, flags=re.S)
        title, math, linear = FORMULAS[number]
        math_box = f'<section class="card"><h2>Fórmula em duas formas: {escape(title)}</h2><div class="formula"><math xmlns="http://www.w3.org/1998/Math/MathML" display="block"><mrow>{math}</mrow></math><p><strong>Em linha:</strong> {readable(linear, number)}</p></div><p>As duas escritas descrevem a mesma conta. Use o formato que ajudar a entender cada operação.</p></section>'
        text = text.replace(sections, sections + math_box, 1)
        prompt, solution = PRACTICE[number]
        practice = f'<section class="card"><h2>Sua vez: tente antes de abrir</h2><p>{readable(prompt, number)}</p><details><summary>Conferir o raciocínio da atividade</summary>{steps(solution, number)}</details><p>Refaça no papel. Se errou, descubra em qual passo antes de seguir para as dez questões.</p></section>'
        text, replaced = re.subn(r'<section class="card"><h2>Exemplos resolvidos passo a passo</h2>.*?</section>', lambda _: practice, text, count=1, flags=re.S)
        if not replaced:
            text = text.replace(math_box, math_box + practice, 1)
        recap = '<h2>Resumo depois de entender os exemplos</h2><ul>' + ''.join(f'<li>{readable(lesson[3], number)}</li>' for lesson in lessons) + '</ul>'
        text = re.sub(r'(<section class="card callout callout--summary">).*?</section>', lambda m: m[1] + recap + '</section>', text, count=1, flags=re.S)
        def insert_question(match):
            question = int(match[1])
            content = match[0][:-6]
            label = f'Ver resposta e resolução — questão {question}'
            link = escape(correction.name, quote=True) + f'#q{question}'
            return content + f'<details class="question-solution"><summary>{label}</summary>{answer(number, question, letters[question])}<p><a href="{link}">Abrir esta correção no gabarito separado</a></p></details></div>'
        text, count = re.subn(r'<div class="q" id="q(\d+)">.*?</div>', insert_question, text, flags=re.S)
        assert count == 10
        def expand_correction(match):
            question = int(match[1])
            block = match[0]
            return re.sub(r'<p>.*?</p>', lambda _: answer(number, question, letters[question]), block, count=1, flags=re.S)
        answers, count = re.subn(r'<div class="q" id="q(\d+)">.*?</div>', expand_correction, answers, flags=re.S)
        assert count == 10
        instruction = '<p class="note">Tente primeiro sem consulta. Depois use “Ver resposta e resolução” abaixo da questão; clique novamente para recolher. O gabarito separado continua disponível.</p>'
        text = text.replace('<h2>Exercícios do módulo</h2>', '<h2>Exercícios do módulo</h2>' + instruction, 1)
        notice = '<section class="card"><h2>Como acompanhar esta aula</h2><p>Comece pelos desenhos e pelos exemplos. Depois leia a fórmula e explique cada passo com suas palavras. Os esquemas visuais originais continuam disponíveis; as correções agora explicam as etapas.</p><p class="small">Revisão didática para iniciantes. Questões autorais do projeto [Gaby], com duas correções editoriais de alternativas nos módulos M05 e M07. Não constitui nova auditoria do edital.</p></section>'
        text = text.replace('<nav class="v3-nav"', notice + '<nav class="v3-nav"', 1)
        for page, html in [(study, text), (correction, answers)]:
            html = html.replace('</head>', '<link rel="stylesheet" href="../assets/matematica.css"></head>', 1)
            html = html.replace('<span class="edition">Edição V3 · Estudo visual e referências</span>', '<span class="edition">Revisão didática · Aula guiada e resolução passo a passo</span>', 1)
            (target / page.name).write_text(html, encoding='utf-8')
    assets = output / 'assets'
    assets.mkdir(exist_ok=True)
    shutil.copy2(source / 'assets/matematica.css', assets / 'matematica.css')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.source.resolve() == args.output.resolve():
        parser.error('Use uma pasta de saída separada para preservar os arquivos-base.')
    enhance(args.source, args.output)
