"""Replace repeated question attribution boxes with concise study-reference links.

Original provenance remains in the V3 source and question catalog. These links
are conceptual references, not claims that an institution authored a question.
"""

import argparse
from html import escape
from html.parser import HTMLParser
from pathlib import Path
import re
from urllib.parse import urlsplit


class Reference(HTMLParser):
    def __init__(self):
        super().__init__()
        self.href = None

    def handle_starttag(self, tag, attrs):
        if tag == 'a' and self.href is None:
            self.href = dict(attrs).get('href')


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
            return f'<p class="question-source small">Fonte: <a href="{escape(reference.href, quote=True)}">referência de estudo</a>.</p>'

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
