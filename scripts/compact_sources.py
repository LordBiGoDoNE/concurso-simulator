"""Replace repeated question attribution boxes with concise study-reference links.

Original provenance remains in the V3 source and question catalog. These links
are conceptual references, not claims that an institution authored a question.
"""

import argparse
from html import escape
from html.parser import HTMLParser
from pathlib import Path
import re
from urllib.parse import unquote, urlsplit


class Reference(HTMLParser):
    def __init__(self):
        super().__init__()
        self.href = None
        self.links = []
        self.current = None
        self.label = []

    def handle_starttag(self, tag, attrs):
        if tag == 'a':
            self.current = dict(attrs).get('href')
            self.label = []
            if self.href is None:
                self.href = self.current

    def handle_data(self, data):
        if self.current:
            self.label.append(data)

    def handle_endtag(self, tag):
        if tag == 'a' and self.current:
            self.links.append((self.current, ' '.join(''.join(self.label).split())))
            self.current = None


def named_sources(reference, page, text, site):
    url = urlsplit(reference.href)
    if url.scheme in {'http', 'https'}:
        return reference.links
    assert not url.scheme and not url.netloc and url.fragment, f'Unsupported reference in {page}'
    target = (page.parent / unquote(url.path)).resolve() if url.path else page.resolve()
    assert target.is_relative_to(site.resolve()), 'Reference must stay inside the site'
    content = target.read_text(encoding='utf-8') if url.path else text
    section = re.search(r'<section\b[^>]*\bid="' + re.escape(unquote(url.fragment)) + r'"[^>]*>.*?</section>', content, re.S)
    assert section, f'Reference section not found in {target}'
    parser = Reference()
    parser.feed(section[0])
    # Preserve all named references instead of claiming an undocumented
    # article-to-question mapping or choosing an arbitrary institution.
    links = list(dict.fromkeys((href, name) for href, name in parser.links if urlsplit(href).scheme in {'http', 'https'} and name))
    assert links, f'No named study references in {target}'
    return links


def compact(site):
    total = 0
    # The criteria page has an illustrative legend, not a question reference.
    for page in sorted(site.glob('0[1-5]_*/*.html')):
        text = page.read_text(encoding='utf-8')

        def replace(match):
            reference = Reference()
            reference.feed(match[0])
            assert reference.href, f'Missing source reference in {page}'
            url = urlsplit(reference.href)
            assert not url.scheme or url.scheme in {'http', 'https'}, 'Unsafe reference URL'
            links = named_sources(reference, page, text, site)
            assert all(name for _, name in links), 'Every source needs its real name'
            names = ' · '.join(f'<a href="{escape(href, quote=True)}">{escape(name)}</a>' for href, name in links)
            label = 'Fonte' if len(links) == 1 else 'Fontes'
            return f'<p class="question-source small">{label}: {names}.</p>'

        result, count = re.subn(r'<aside class="callout callout--source">.*?</aside>', replace, text, flags=re.S)
        if count:
            assert result.count('class="question-source small"') >= count
            page.write_text(result, encoding='utf-8')
            total += count
    print(f'OK: {total} caixas de fonte substituídas por links sucintos.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--site', type=Path, required=True)
    args = parser.parse_args()
    compact(args.site)
