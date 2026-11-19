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
# Declarative SVG animation (SMIL), accepted only inside figures listed in
# assets/animacoes/manifest.json. Animated attributes are limited to geometry and
# visibility so content can never animate links, styles or event handlers.
ANIMATION_TAGS = set('animate animateTransform animateMotion mpath set'.split())
ANIMATION_ATTRS = set('attributeName dur begin end repeatCount values keyTimes keySplines calcMode from to by rotate path additive accumulate'.split())
ANIMATED_SVG_ATTRS = set('visibility stroke-dashoffset pathLength'.split())
ANIMATABLE = set('opacity fill-opacity stroke-opacity transform x y cx cy r width height stroke-dashoffset visibility'.split())
TRANSFORM_TYPES = set('translate scale rotate skewX skewY'.split())
TIME = r'\d+(?:\.\d+)?(?:ms|s)?'
SYNC = r'([A-Za-z][\w.-]*?)\.(begin|end)((?:[+-]' + TIME + r')?)'
ANIMATIONS = 'assets/animacoes'
MAX_STEPS = 8

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
    practice = ('Para resolver questões, abra Pratique esta aula ao final da leitura. '
                'A correção aparece após encerrar a prática.')
    text = text.replace(
        'leia uma seção por vez, refaça a conta no papel e tente a atividade curta. '
        'As respostas das dez questões de concurso continuam no arquivo separado.',
        'leia uma seção por vez e refaça os exemplos no papel. ' + practice)
    text = text.replace('O gabarito comentado está em arquivo separado.', practice)
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

    def attrs(self, attrs, prefix=None, animated=False):
        result = {}
        for key, value in attrs.items():
            if key in {'class', 'loading', 'target', 'rel'}:
                continue
            if key not in ATTRS and not (animated and key in ANIMATION_ATTRS | ANIMATED_SVG_ATTRS):
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

    def svg_root(self, path):
        text = self.source(path)
        if '<!DOCTYPE' in text.upper() or '<?' in text:
            raise ValueError('SVG com declaração ativa')
        return ET.fromstring(text)

    def diagram(self, node, source):
        path = (source.parent / unquote(node.attrs['src'])).resolve()
        if path.parent != (self.root / 'assets/esquemas').resolve() or path.suffix != '.svg':
            raise ValueError('Esquema fora da seleção local')
        svg = self.svg_root(path)
        self.figures += 1
        return self.svg(svg, self.prefix + f'figure-{self.figures}-')

    def svg(self, svg, prefix, animated=False):
        def convert(element, parent=None):
            tag = element.tag.removeprefix('{http://www.w3.org/2000/svg}')
            if tag not in SVG_TAGS and not (animated and tag in ANIMATION_TAGS):
                raise ValueError('Elemento SVG não permitido')
            label = ''.join(element.itertext())
            if tag == 'text' and (label == 'GABY  /  LIMEIRA 2026' or label.startswith('ESQUEMA AUTORAL V3 ·')):
                return None  # Edition/brand chrome, not the teaching diagram.
            raw = {key.replace('{http://www.w3.org/1999/xlink}', 'xlink:'): value for key, value in element.attrib.items()}
            if animated and tag != 'mpath':
                for key in ('href', 'xlink:href'):
                    if key in raw:
                        raise ValueError(f'atributo {key} não permitido em {tag} (só mpath referencia a figura)')
            if tag in ANIMATION_TAGS:
                raw = self.animation_attrs(tag, raw, parent, prefix)
            elif any(key in ANIMATION_ATTRS for key in raw):
                raise ValueError(f'{self.topic}: atributo de animação fora de elemento de animação')
            children = [element.text] if element.text and tag not in ANIMATION_TAGS else []
            for child in element:
                converted = convert(child, tag)
                if converted: children.append(converted)
                if child.tail and tag not in ANIMATION_TAGS: children.append(child.tail)
            if tag in ANIMATION_TAGS and any(isinstance(child, dict) and child['tag'] != 'mpath' for child in children):
                raise ValueError('Animação aninhada não permitida')
            return {'tag': tag, 'attrs': self.attrs(raw, prefix, animated), 'children': children}
        return convert(svg)

    def animation_attrs(self, tag, raw, parent, prefix):
        """Validate one SMIL element; the referenced IDs are prefixed by attrs()."""
        result = dict(raw)
        name = raw.get('attributeName')
        if tag == 'mpath':
            if parent != 'animateMotion' or set(raw) - {'href', 'xlink:href'}:
                raise ValueError('mpath fora de animateMotion')
            href = raw.get('href') or raw.get('xlink:href') or ''
            if not re.fullmatch(r'#[A-Za-z][\w.-]*', href):
                raise ValueError('mpath deve apontar para um ID da própria figura')
            return {'href': '#' + prefix + href[1:]}
        if tag in {'animate', 'set'} and name not in ANIMATABLE:
            raise ValueError(f'{self.topic}: attributeName não permitido: {name}')
        if tag == 'animateTransform' and (name != 'transform' or raw.get('type') not in TRANSFORM_TYPES):
            raise ValueError('animateTransform só anima transform com tipo permitido')
        if tag == 'animateMotion' and name is not None:
            raise ValueError('animateMotion não usa attributeName')
        if raw.get('fill', 'remove') not in {'freeze', 'remove'}:
            raise ValueError('fill de animação deve ser freeze ou remove')
        if raw.get('repeatCount', '1') != 'indefinite' and not re.fullmatch(r'\d+(?:\.\d+)?', raw.get('repeatCount', '1')):
            raise ValueError('repeatCount inválido')
        for key in ('dur',):
            if key in raw and not re.fullmatch(TIME, raw[key]):
                raise ValueError('Duração de animação inválida')
        for key in ('begin', 'end'):
            if key in raw:
                parts = []
                for part in raw[key].split(';'):
                    part = part.strip()
                    if re.fullmatch(TIME, part):
                        parts.append(part)
                        continue
                    match = re.fullmatch(SYNC, part)
                    if not match:
                        raise ValueError(f'{self.topic}: {key} só aceita tempo ou ID da própria figura')
                    parts.append(prefix + match[1] + '.' + match[2] + match[3])
                result[key] = ';'.join(parts)
        for key, value in raw.items():
            if 'url(' in value or 'javascript' in value.lower() or '<' in value:
                raise ValueError('Valor de animação não permitido')
        return result

    def animation(self, entry):
        folder = (self.root / ANIMATIONS).resolve()
        path = folder / entry['file']
        if path.parent.resolve() != folder:
            raise ValueError('Animação fora de ' + ANIMATIONS)
        prefix = self.prefix + 'animation-' + entry['id'] + '-'
        where = f'Animação {entry["id"]} ({ANIMATIONS}/{entry["file"]})'
        try:
            svg = self.svg(self.svg_root(path), prefix, animated=True)
        except ValueError as error:
            raise ValueError(f'{where}: {error}') from error
        if svg['tag'] != 'svg' or 'viewBox' not in svg['attrs']:
            raise ValueError(f'{where}: deve ser um SVG com viewBox')
        ids, references = set(), set()
        def walk(element):
            attrs = element['attrs']
            if 'id' in attrs:
                if attrs['id'] in ids:
                    raise ValueError(f'{where}: atributo id duplicado na figura: {attrs["id"].removeprefix(prefix)}')
                ids.add(attrs['id'])
            if element['tag'] == 'mpath': references.add(attrs['href'][1:])
            for key in ('begin', 'end'):
                for part in attrs.get(key, '').split(';'):
                    match = re.fullmatch(SYNC, part)
                    if match: references.add(match[1])
            for child in element['children']:
                if isinstance(child, dict): walk(child)
        walk(svg)
        if references - ids:
            missing = ', '.join(sorted(ident.removeprefix(prefix) for ident in references - ids))
            raise ValueError(f'{where}: begin/end/mpath referencia ID fora da figura: {missing}')
        node = {'tag': 'animation', 'id': prefix[:-1], 'mode': entry['mode'], 'title': entry['title'],
                'description': entry['description'], 'duration': entry['duration'], 'poster': entry['poster'], 'svg': svg}
        if entry['mode'] == 'steps':
            node['steps'] = [{'time': step['time'], 'caption': step['caption']} for step in entry['steps']]
        return node

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

def number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and value >= 0

def load_animations(root, sources, topics):
    """Validated animation inventory, grouped by topic; absent file means none."""
    path = root / ANIMATIONS / 'manifest.json'
    if not path.exists():
        return {}
    text = canonical(path)
    sources[ANIMATIONS + '/manifest.json'] = sha(text.encode())
    data = json.loads(text)
    if set(data) != {'format', 'animations'} or data['format'] != 'lesson-animations/v1' or not isinstance(data['animations'], list):
        raise ValueError('Manifesto de animações inválido')
    grouped, seen = {}, set()
    required = {'id', 'topic', 'anchor', 'file', 'title', 'description', 'mode', 'duration', 'poster'}
    for entry in data['animations']:
        ident = entry.get('id') if isinstance(entry, dict) else None
        def fail(reason):
            raise ValueError(f'Animação {ident}: {reason}')
        if not isinstance(entry, dict) or not isinstance(ident, str) or not re.fullmatch(r'[A-Za-z][\w-]*', ident):
            raise ValueError('Animação sem ID válido')
        if ident in seen: fail('ID repetido')
        seen.add(ident)
        expected = required | ({'steps'} if entry.get('mode') == 'steps' else set())
        if set(entry) != expected: fail('campos divergentes')
        if entry['topic'] not in topics: fail('aula desconhecida')
        if not isinstance(entry['anchor'], str) or not re.fullmatch(r'[A-Za-z][\w.-]*', entry['anchor']): fail('âncora inválida')
        if not isinstance(entry['file'], str) or not re.fullmatch(r'[A-Za-z0-9][\w-]*\.svg', entry['file']): fail('arquivo inválido')
        if any(not isinstance(entry[key], str) or not entry[key].strip() for key in ('title', 'description')): fail('título e descrição obrigatórios')
        if entry['mode'] not in {'cycle', 'steps'}: fail('modo inválido')
        if not number(entry['duration']) or entry['duration'] == 0: fail('duração inválida')
        if not number(entry['poster']) or entry['poster'] > entry['duration']: fail('quadro estático fora da duração')
        if entry['mode'] == 'steps':
            steps = entry['steps']
            if not isinstance(steps, list) or not 1 <= len(steps) <= MAX_STEPS: fail(f'etapas devem ser de 1 a {MAX_STEPS}')
            previous = -1
            for step in steps:
                if not isinstance(step, dict) or set(step) != {'time', 'caption'}: fail('etapa inválida')
                if not number(step['time']) or step['time'] <= previous or step['time'] > entry['duration']: fail('etapas fora de ordem ou da duração')
                if not isinstance(step['caption'], str) or not step['caption'].strip(): fail('etapa sem legenda')
                previous = step['time']
        grouped.setdefault(entry['topic'], []).append(entry)
    return grouped

def place(children, ident, node):
    """Append the animation to the single section whose compiled ID is ident."""
    found = []
    def walk(items):
        for item in items:
            if isinstance(item, dict):
                if item.get('attrs', {}).get('id') == ident:
                    found.append(item)
                walk(item.get('children', []))
    walk(children)
    if len(found) != 1 or found[0]['tag'] != 'section':
        raise ValueError(f'Âncora de animação ausente, repetida ou fora de seção: {ident}')
    found[0]['children'].append(node)

def compile_all(root):
    sources = {}
    documents = {}
    catalog = json.loads((root / 'frontend/src/lesson-catalog.json').read_text(encoding='utf-8'))
    modules = json.loads((root / 'manifesto_v3.json').read_text(encoding='utf-8'))['modulos']
    topics = {topic['id']: topic for topic in catalog['topics']}
    if len(modules) != 51 or set(topics) != {module['id'] for module in modules}:
        raise ValueError('Catálogo das 51 aulas divergente')
    animations = load_animations(root, sources, topics)
    workspace = root / 'backend/build'
    workspace.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=workspace, prefix='native-source-') as temporary:
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
            for entry in animations.get(ident, []):
                place(children, compiler.prefix + entry['anchor'], compiler.animation(entry))
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
