"""Validate the generated mathematics edition and its separate answer pages."""

import argparse
from html.parser import HTMLParser
from pathlib import Path
import re
from urllib.parse import unquote, urlsplit


class Structure(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = set()
        self.links = []
        self.answers = 0

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if 'id' in attrs:
            assert attrs['id'] not in self.ids, f"Duplicate ID: {attrs['id']}"
            self.ids.add(attrs['id'])
        if tag == 'a' and attrs.get('href'):
            self.links.append(attrs['href'])
        if tag == 'details' and attrs.get('class') == 'question-solution':
            assert 'open' not in attrs, 'Answer must start collapsed'
            self.answers += 1


def check(source, site):
    total = 0
    for number in range(1, 10):
        directory = site / '02_Matematica_Logica'
        study = next(p for p in directory.glob(f'Matemática {number:02d} - *.html') if 'Gabarito' not in p.name)
        correction = next(directory.glob(f'Matemática {number:02d} - *Gabarito*.html'))
        text = study.read_text(encoding='utf-8')
        original = (source / '02_Matematica_Logica' / study.name).read_text(encoding='utf-8')
        answers = correction.read_text(encoding='utf-8')
        options = re.findall(r'<ol type="A">(.*?)</ol>', text, re.S)
        previous = re.findall(r'<ol type="A">(.*?)</ol>', original, re.S)
        assert len(options) == len(previous) == 10
        if number == 5:
            previous[3] = previous[3].replace('<li>7,0</li>', '<li>8</li>')
        if number == 7:
            previous[5] = previous[5].replace('ambas A e B', 'ambas B e C')
        assert options == previous, f'M{number:02}: unintended option edits'
        structure = Structure()
        structure.feed(text)
        assert structure.answers == 10, f'M{number:02}: expected 10 collapsed answers'
        assert {f'q{i}' for i in range(1, 11)} <= structure.ids
        if number != 4:
            letters = re.findall(r'Questão \d+ — alternativa ([A-D])', answers)
            assert re.findall(r'<h4>Resposta: alternativa ([A-D])', text) == letters
            assert re.findall(r'<h4>Resposta: alternativa ([A-D])', answers) == letters
            assert '<math ' in text
            # Each inline resolution must also occur in the separate answer file.
            for resolution in re.findall(r'<div class="inline-resolution">.*?</div>', text, re.S):
                assert resolution in answers
        for link in structure.links:
            url = urlsplit(link)
            if url.scheme or url.netloc or not url.fragment:
                continue
            target = study.parent / unquote(url.path) if url.path else study
            target_structure = Structure()
            target_structure.feed(target.read_text(encoding='utf-8'))
            assert unquote(url.fragment) in target_structure.ids, f'Broken fragment: {link}'
        total += structure.answers
    print(f'OK: 9 módulos, {total} respostas recolhidas, alternativas, gabaritos e âncoras conferidos.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--site', type=Path, required=True)
    args = parser.parse_args()
    check(args.source, args.site)
