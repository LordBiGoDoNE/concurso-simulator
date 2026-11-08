"""Compile the 51 preserved lessons into inert data for the native React reader.

No HTML/CSS is embedded at runtime. Legacy navigation and exercise banks are
excluded; teaching text, worked examples, diagrams, tables and sources remain.
"""
import argparse
import hashlib
import json
import re
import tempfile
from pathlib import Path
from urllib.parse import unquote, urlsplit
import xml.etree.ElementTree as ET

from extract_questions import Document, Node
from enrich_math import enhance

HTML_TAGS = set('section aside div span p h2 h3 h4 ul ol li strong b em i small br code var sub sup table thead tbody tr th td caption figure figcaption details summary a math mrow mi mn mo mfrac msup msub msqrt mtext mroot mtable mtr mtd'.split())
SVG_TAGS = set('svg g path rect circle ellipse line polyline polygon text tspan title desc defs marker'.split())
ATTRS = set('id href type role aria-label aria-labelledby aria-hidden scope colspan rowspan start display xmlns viewBox width height x y dx dy x1 y1 x2 y2 cx cy r rx ry points d transform fill stroke stroke-width stroke-dasharray stroke-linecap stroke-linejoin fill-rule opacity fill-opacity stroke-opacity font-family font-size font-weight font-style text-anchor dominant-baseline markerWidth markerHeight refX refY orient marker-end'.split())
TONES = {'card': 'section', 'visual-card': 'diagram', 'box': 'note', 'note': 'note',
         'example': 'example', 'solution': 'solution', 'formula': 'formula',
         'callout--attention': 'attention', 'callout--trap': 'attention',
         'callout--summary': 'summary', 'callout--reference': 'reference',
         'callout--law': 'reference', 'callout-label': 'label', 'refs': 'sources',
         'visual-description': 'description', 'table-scroll': 'table', 'steps': 'steps', 'toc': 'contents'}

def canonical(path):
    return path.read_text(encoding='utf-8').replace('\r\n', '\n')

def sha(data):
    return hashlib.sha256(data).hexdigest()

def dump(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':')) + '\n'

def classes(node):
    return set(node.attrs.get('class', '').split())

def clean_text(text):
    return text.replace('para a V3', 'para este material').replace('da V3', 'deste material')

class Compiler:
    def __init__(self, root, topic, sources):
        self.root, self.topic, self.sources = root, topic, sources
        self.prefix = 'lesson-' + topic + '-'
        self.figures = 0
        self.removed = []

    def source(self, path):
        if path.is_symlink():
            raise ValueError('Fonte vinculada não permitida')
        path = path.resolve()
        if not path.is_relative_to(self.root) or path.is_symlink():
            raise ValueError('Fonte fora do projeto ou link')
        text = canonical(path)
        self.sources[path.relative_to(self.root).as_posix()] = sha(text.encode())
        return text

    def attrs(self, attrs, prefix=None):
        result = {}
        for key, value in attrs.items():
            if key in {'class', 'loading', 'target', 'rel'}:
                continue
            if key not in ATTRS:
                raise ValueError(f'{self.topic}: atributo não permitido: {key}')
            if key == 'xmlns' and value not in {'http://www.w3.org/2000/svg', 'http://www.w3.org/1998/Math/MathML'}:
                raise ValueError('Namespace não permitido')
            prefix = prefix or self.prefix
            if key == 'id':
                if not re.fullmatch(r'[A-Za-z][\w.-]*', value):
                    raise ValueError('ID inválido')
                value = prefix + value
            if key == 'aria-labelledby':
                value = ' '.join(prefix + ident for ident in value.split())
            if 'url(' in value:
                match = re.fullmatch(r'url\(#([A-Za-z][\w.-]*)\)', value)
                if key != 'marker-end' or not match:
                    raise ValueError('Referência SVG externa/não permitida')
                value = 'url(#' + prefix + match[1] + ')'
            result[key] = value
        return result

    def diagram(self, node, source):
        path = (source.parent / unquote(node.attrs['src'])).resolve()
        if path.parent != (self.root / 'assets/esquemas').resolve() or path.suffix != '.svg':
            raise ValueError('Esquema fora da seleção local')
        text = self.source(path)
        if '<!DOCTYPE' in text.upper() or '<?' in text:
            raise ValueError('SVG com declaração ativa')
        svg = ET.fromstring(text)
        self.figures += 1
        prefix = self.prefix + f'figure-{self.figures}-'
        def convert(element):
            tag = element.tag.removeprefix('{http://www.w3.org/2000/svg}')
            if tag not in SVG_TAGS:
                raise ValueError('Elemento SVG não permitido')
            label = ''.join(element.itertext())
            if tag == 'text' and (label == 'GABY  /  LIMEIRA 2026' or label.startswith('ESQUEMA AUTORAL V3 ·')):
                return None  # Edition/brand chrome, not the teaching diagram.
            children = [element.text] if element.text else []
            for child in element:
                converted = convert(child)
                if converted: children.append(converted)
                if child.tail: children.append(child.tail)
            return {'tag': tag, 'attrs': self.attrs(element.attrib, prefix), 'children': children}
        return convert(svg)

    def convert(self, node, source):
        if isinstance(node, str):
            return clean_text(node)
        cls = classes(node)
        headings = [child.text().strip() for child in node.children if isinstance(child, Node) and child.tag == 'h2']
        if (node.tag in {'nav', 'footer', 'header'} or 'v3-footer' in cls
                or 'tag' in cls or 'exercise-workshop' in cls
                or node.attrs.get('id') == 'exercicios'
                or cls & {'q', 'question', 'question-solution', 'inline-resolution'}
                or headings and headings[0] in {'Como acompanhar esta aula', 'Sua vez: tente antes de abrir'}):
            self.removed.append(node.attrs.get('id') or next(iter(sorted(cls)), node.tag))
            return None
        if node.tag == 'figcaption' and 'Esquema autoral da V3' in node.text():
            return None
        if node.tag == 'img':
            return self.diagram(node, source)
        if 'hundred' in cls:
            label = node.attrs.get('aria-label', '')
            match = re.search(r'Os (\d+) primeiros', label)
            if not match or len([x for x in node.children if isinstance(x, Node)]) != 100:
                raise ValueError('Grade percentual sem contagem exata')
            return {'tag': 'percentage-grid', 'value': int(match[1]), 'label': label}
        if 'bar' in cls:
            parts = [child.text() for child in node.children if isinstance(child, Node)]
            if len(parts) != 2 or not re.match(r'\d+%', parts[0]):
                raise ValueError('Barra percentual inválida')
            return {'tag': 'percentage-bar', 'value': int(parts[0].split('%')[0]), 'labels': parts}
        if node.tag not in HTML_TAGS:
            raise ValueError(f'{self.topic}: elemento não permitido: {node.tag}')
        attrs = self.attrs(node.attrs)
        if node.tag == 'a':
            href = node.attrs.get('href', '')
            parsed = urlsplit(href)
            if parsed.scheme in {'https', 'http'} and parsed.netloc and not parsed.username and not parsed.password:
                attrs['href'] = href
            elif href.startswith('#'):
                attrs['href'] = '#' + self.prefix + href[1:]
            elif any(isinstance(child, Node) and child.tag == 'img' for child in node.children):
                return self.children(node, source)[0]  # Inline native SVG; no legacy image link.
            else:
                raise ValueError(f'{self.topic}: link local legado fora das seções removidas: {href}')
        tag = node.tag
        if 'visual-description' in cls:
            tag = 'div'  # Keep the accessible explanation readable, including on mobile.
        children = self.children(node, source, skip_summary='visual-description' in cls)
        tone = next((TONES[key] for key in reversed(TONES) if key in cls), None)
        result = {'tag': tag, 'children': children}
        if attrs: result['attrs'] = attrs
        if tone: result['tone'] = tone
        return result

    def children(self, node, source, skip_summary=False):
        result = []
        for child in node.children:
            if skip_summary and isinstance(child, Node) and child.tag == 'summary':
                continue
            value = self.convert(child, source)
            if value is not None and value != '': result.append(value)
        return result

def compile_all(root):
    sources = {}
    documents = {}
    catalog = json.loads((root / 'frontend/src/lesson-catalog.json').read_text(encoding='utf-8'))
    modules = json.loads((root / 'manifesto_v3.json').read_text(encoding='utf-8'))['modulos']
    topics = {topic['id']: topic for topic in catalog['topics']}
    if len(modules) != 51 or set(topics) != {module['id'] for module in modules}:
        raise ValueError('Catálogo das 51 aulas divergente')
    with tempfile.TemporaryDirectory(dir=root / 'backend/build', prefix='native-source-') as temporary:
        enhanced = Path(temporary)
        enhance(root, enhanced)
        for module in modules:
            ident = module['id']
            compiler = Compiler(root, ident, sources)
            original = root / module['conteudo']
            compiler.source(original)
            source = enhanced / module['conteudo']
            if source.exists():
                # The existing enrichment reads this public answer file while
                # preparing its output; exercise blocks never enter our data.
                compiler.source(root / module['gabarito'])
            # Enhanced content uses the same source-relative image paths.
            content = canonical(source) if source.exists() else canonical(original)
            document = Document(content)
            main = next(node for node in document.root.nodes() if node.tag == 'main')
            children = compiler.children(main, original)
            documents[ident + '.json'] = dump({'format': 'native-lesson/v1', 'topicId': ident,
                'title': topics[ident]['title'], 'children': children})
    for name in ['scripts/compile_native_lessons.py', 'scripts/enrich_math.py', 'scripts/math_content.py',
                 'scripts/extract_questions.py', 'manifesto_v3.json', 'frontend/src/lesson-catalog.json']:
        sources[name] = sha(canonical(root / name).encode())
    documents['manifest.json'] = dump({'format': 'native-lessons-manifest/v1',
        'documents': {name: sha(data.encode()) for name, data in documents.items()}, 'sources': dict(sorted(sources.items())),
        'scope': '51 aulas: teoria, exemplos, esquemas e fontes; exercícios somente no painel de prática.'})
    return documents

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--output', type=Path)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    root = args.root.resolve()
    (root / 'backend/build').mkdir(parents=True, exist_ok=True)
    output = args.output or root / 'frontend/src/lesson-documents'
    documents = compile_all(root)
    if args.check:
        if set(path.name for path in output.glob('*.json')) != set(documents):
            raise ValueError('Seleção de documentos gerados divergente')
        for name, text in documents.items():
            if not (output / name).exists() or canonical(output / name) != text:
                raise ValueError('Documento desatualizado: ' + name)
    else:
        output.mkdir(parents=True, exist_ok=True)
        for name, text in documents.items():
            (output / name).write_text(text, encoding='utf-8', newline='\n')
    print(json.dumps({'lessons': len(documents)-1, 'status': 'verified' if args.check else 'generated',
                      'bytes': sum(len(text.encode()) for text in documents.values())}))

if __name__ == '__main__':
    main()
