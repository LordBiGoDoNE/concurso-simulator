"""Hand-drawn SVG animations for the native lessons, generated deterministically.

Sources live in assets/animacoes/fontes/<id>.json. The tool writes
assets/animacoes/<id>.svg and assets/animacoes/manifest.json; --check regenerates
both in memory and fails when the committed files differ or a limit is exceeded.

Every animated property is a single SMIL element that starts at 0 s and lasts the
whole animation, with values/keyTimes. Seeking to any time therefore shows the
right state, which the lesson reader uses for steps, pause and reduced motion.
Static attributes hold the poster state, so a browser without SMIL still shows
the complete figure.
"""
import argparse
import json
import math
import random
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FOLDER = 'assets/animacoes'
PALETTE = {'paper': '#fbf8f1', 'ink': '#2b2b2b', 'muted': '#5f6368', 'accent': '#0f766e',
           'accent-soft': '#d6eee8', 'warm': '#b45309', 'warm-soft': '#fbe5cc', 'soft': '#efeae0',
           'white': '#ffffff', 'none': 'none'}
MAX_BYTES, MAX_CYCLE, MAX_STEPS, MAX_PER_TOPIC = 30_000, 12, 8, 2
MAX_WIDTH, MIN_TEXT = 480, 18
FADE = 0.3
META = ('id', 'topic', 'anchor', 'title', 'description', 'mode', 'duration', 'poster')


def fmt(value):
    text = f'{value:.1f}'.rstrip('0').rstrip('.')
    return '0' if text in ('-0', '') else text


def esc(text):
    return text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;')


def color(name):
    if name not in PALETTE:
        raise ValueError(f'Cor fora da paleta: {name}')
    return PALETTE[name]


class Timeline:
    """Piecewise values over [0, duration] rendered as one SMIL element."""

    def __init__(self, duration, poster):
        self.duration, self.poster = duration, poster

    def keys(self, points):
        clean = []
        for time, value in sorted(points, key=lambda point: point[0]):
            time = min(max(time, 0), self.duration)
            if clean and abs(clean[-1][0] - time) < 1e-9:
                clean[-1] = (time, value)
            else:
                clean.append((time, value))
        if clean[0][0] > 0:
            clean.insert(0, (0, clean[0][1]))
        if clean[-1][0] < self.duration:
            clean.append((self.duration, clean[-1][1]))
        return clean

    def at(self, points, time, mix):
        points = self.keys(points)
        for (t0, v0), (t1, v1) in zip(points, points[1:]):
            if t0 <= time <= t1:
                return mix(v0, v1, 0 if t1 == t0 else (time - t0) / (t1 - t0))
        return points[-1][1]

    def element(self, tag, attribute, points, render, extra=''):
        points = self.keys(points)
        times = ';'.join(f'{time / self.duration:.4f}'.rstrip('0').rstrip('.') or '0' for time, _ in points)
        values = ';'.join(render(value) for _, value in points)
        return (f'<{tag} attributeName="{attribute}"{extra} values="{values}" keyTimes="{times}" '
                f'dur="{fmt(self.duration)}s" begin="0s" fill="freeze"/>')


def opacity_points(show, duration):
    start, end = show[0], show[1] if len(show) > 1 and show[1] is not None else None
    points = [(0, 0)] if start > 0 else [(0, 1)]
    if start > 0:
        points += [(start, 0), (start + FADE, 1)]
    if end is not None:
        points += [(end, 1), (end + FADE, 0)]
    return points


def move_points(moves):
    points, current = [(0, (0, 0))], (0, 0)
    for move in moves:
        t0, t1 = move['at']
        points += [(t0, current), (t1, tuple(move['to']))]
        current = tuple(move['to'])
    return points


class Sketch:
    def __init__(self, source):
        self.source = source
        self.timeline = Timeline(source['duration'], source['poster'])
        self.counter = 0

    def rng(self):
        self.counter += 1
        return random.Random(f'{self.source["seed"]}:{self.counter}')

    # Hand-drawn primitives -------------------------------------------------
    def stroke(self, points, rng, closed=False):
        segments = list(zip(points, points[1:] + ([points[0]] if closed else [])))
        paths = []
        for strength in (1.0, 0.7):
            parts = []
            for (x0, y0), (x1, y1) in segments:
                length = math.hypot(x1 - x0, y1 - y0) or 1
                wobble = min(2.2, 0.5 + length * 0.008) * strength
                jx0, jy0 = x0 + rng.uniform(-wobble, wobble), y0 + rng.uniform(-wobble, wobble)
                jx1, jy1 = x1 + rng.uniform(-wobble, wobble), y1 + rng.uniform(-wobble, wobble)
                bow = rng.uniform(-1, 1) * min(3, length * 0.012) * strength
                nx, ny = -(y1 - y0) / length, (x1 - x0) / length
                cx, cy = (jx0 + jx1) / 2 + nx * bow, (jy0 + jy1) / 2 + ny * bow
                parts.append(f'M{fmt(jx0)} {fmt(jy0)}Q{fmt(cx)} {fmt(cy)} {fmt(jx1)} {fmt(jy1)}')
            paths.append(''.join(parts))
        return paths

    def ellipse_points(self, cx, cy, rx, ry, rng, count=14):
        start = rng.uniform(0, math.tau)
        points = []
        for index in range(count + 2):
            angle = start + index * math.tau / count
            jitter = 1 + rng.uniform(-0.03, 0.03)
            points.append((cx + rx * jitter * math.cos(angle), cy + ry * jitter * math.sin(angle)))
        return points

    def smooth(self, points):
        path = f'M{fmt(points[0][0])} {fmt(points[0][1])}'
        for (x0, y0), (x1, y1) in zip(points[1:], points[2:]):
            path += f'Q{fmt(x0)} {fmt(y0)} {fmt((x0 + x1) / 2)} {fmt((y0 + y1) / 2)}'
        return path

    def paths(self, data, stroke, width, fill='none', draw=False):
        extra = ' pathLength="1" stroke-dasharray="1"' if draw else ''
        widths = (width, width * 0.6)
        return ''.join(f'<path d="{d}" fill="{fill}" stroke="{stroke}" stroke-width="{fmt(w)}" '
                       f'stroke-linecap="round" stroke-linejoin="round"{extra}/>' for d, w in zip(data, widths))

    # Elements --------------------------------------------------------------
    def text(self, item):
        size = item.get('size', MIN_TEXT)
        if size < MIN_TEXT:
            raise ValueError(f'Texto menor que {MIN_TEXT}: {item.get("text")}')
        weight = f' font-weight="{item["weight"]}"' if 'weight' in item else ''
        return (f'<text x="{fmt(item["x"])}" y="{fmt(item["y"])}" font-size="{fmt(size)}" '
                f'fill="{color(item.get("color", "ink"))}" text-anchor="{item.get("anchor", "middle")}"{weight}>'
                f'{self.spans(item)}</text>')

    def spans(self, item):
        if 'spans' not in item:
            return esc(item['text'])
        return ''.join(f'<tspan fill="{color(tone)}">{esc(part)}</tspan>' if tone else esc(part)
                       for part, tone in item['spans'])

    def box(self, item):
        rng = self.rng()
        x, y, w, h = item['x'], item['y'], item['w'], item['h']
        corners = [(x, y), (x + w, y), (x + w, y + h), (x, y + h)]
        body = ''
        if item.get('fill', 'white') != 'none':
            fill = [(px + rng.uniform(-1.5, 1.5), py + rng.uniform(-1.5, 1.5)) for px, py in corners]
            body += f'<path d="M{"L".join(fmt(px) + " " + fmt(py) for px, py in fill)}Z" fill="{color(item.get("fill", "white"))}"/>'
        body += self.paths(self.stroke(corners, rng, closed=True), color(item.get('stroke', 'ink')), item.get('width', 2))
        if 'label' in item:
            body += self.text({'x': x + w / 2, 'y': y + h / 2 + item.get('label_size', MIN_TEXT) * 0.35,
                               'text': item['label'], 'size': item.get('label_size', MIN_TEXT),
                               'color': item.get('label_color', 'ink'), **({'weight': item['weight']} if 'weight' in item else {})})
        return body

    def ellipse(self, item):
        rng = self.rng()
        rx, ry = item.get('rx', item.get('r')), item.get('ry', item.get('r'))
        points = self.ellipse_points(item['cx'], item['cy'], rx, ry, rng)
        body = ''
        if item.get('fill', 'none') != 'none':
            opacity = f' fill-opacity="{fmt(item["fill_opacity"])}"' if 'fill_opacity' in item else ''
            body += f'<path d="{self.smooth(points[:-1])}Z" fill="{color(item["fill"])}"{opacity}/>'
        second = self.ellipse_points(item['cx'], item['cy'], rx, ry, rng)
        body += self.paths([self.smooth(points), self.smooth(second)], color(item.get('stroke', 'ink')), item.get('width', 2))
        return body

    def line(self, item, arrow=False):
        rng = self.rng()
        points = [tuple(point) for point in item['points']]
        stroke, width = color(item.get('color', 'ink')), item.get('width', 2)
        draw = 'draw' in item
        shaft = self.paths(self.stroke(points, rng), stroke, width, draw=draw)
        if draw:
            t0, t1 = item['draw']
            shaft = '<g>' + shaft + self.timeline.element(
                'animate', 'stroke-dashoffset', [(0, 1), (t0, 1), (t1, 0)], fmt) + '</g>'
            shaft = shaft.replace('<g>', f'<g stroke-dashoffset="{fmt(self.static([(0, 1), (t0, 1), (t1, 0)]))}">', 1)
        if not arrow:
            return shaft
        (x0, y0), (x1, y1) = points[-2], points[-1]
        angle = math.atan2(y1 - y0, x1 - x0)
        size = item.get('head', 12)
        wings = [(x1 - size * math.cos(angle - 0.45), y1 - size * math.sin(angle - 0.45)),
                 (x1 - size * math.cos(angle + 0.45), y1 - size * math.sin(angle + 0.45))]
        head = ''.join(self.paths(self.stroke([wing, (x1, y1)], rng), stroke, width) for wing in wings)
        if draw:
            head = self.wrap(head, {'show': [item['draw'][1] - FADE]})
        return shaft + head

    def static(self, points):
        return self.timeline.at(points, self.timeline.poster, lambda a, b, r: a + (b - a) * r)

    def wrap(self, body, item):
        """Group with the element's show/move timeline; static attributes = poster state."""
        animations, attrs = '', ''
        if 'show' in item:
            points = opacity_points(item['show'], self.timeline.duration)
            attrs += f' opacity="{fmt(self.static(points))}"'
            animations += self.timeline.element('animate', 'opacity', points, fmt)
        if 'move' in item:
            points = move_points(item['move'])
            mix = lambda a, b, r: (a[0] + (b[0] - a[0]) * r, a[1] + (b[1] - a[1]) * r)
            dx, dy = self.timeline.at(points, self.timeline.poster, mix)
            attrs += f' transform="translate({fmt(dx)} {fmt(dy)})"'
            animations += self.timeline.element('animateTransform', 'transform', points,
                                                lambda value: f'{fmt(value[0])} {fmt(value[1])}', ' type="translate"')
        if not animations:
            return body
        return f'<g{attrs}>{body}{animations}</g>'

    def render(self, item):
        kind = item['type']
        if kind == 'group':
            body = ''.join(self.render(child) for child in item['children'])
        elif kind == 'text':
            body = self.text(item)
        elif kind == 'box':
            body = self.box(item)
        elif kind in ('circle', 'ellipse'):
            body = self.ellipse(item)
        elif kind in ('line', 'arrow'):
            body = self.line(item, arrow=kind == 'arrow')
        else:
            raise ValueError(f'Tipo de elemento desconhecido: {kind}')
        return self.wrap(body, item)

    def svg(self):
        width, height = self.source['size']
        if width > MAX_WIDTH:
            raise ValueError(f'{self.source["id"]}: largura acima de {MAX_WIDTH}')
        body = ''.join(self.render(item) for item in self.source['elements'])
        return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}">'
                f'<rect width="{width}" height="{height}" rx="12" fill="{PALETTE["paper"]}"/>{body}</svg>\n')


def build(root=ROOT):
    folder = root / FOLDER
    sources = sorted((folder / 'fontes').glob('*.json'))
    outputs, entries, per_topic = {}, [], {}
    for path in sources:
        source = json.loads(path.read_text(encoding='utf-8'))
        if path.stem != source['id']:
            raise ValueError(f'{path.name}: nome do arquivo difere do ID')
        if source['mode'] == 'cycle' and source['duration'] > MAX_CYCLE:
            raise ValueError(f'{source["id"]}: ciclo acima de {MAX_CYCLE} s')
        if source['mode'] == 'steps' and len(source['steps']) > MAX_STEPS:
            raise ValueError(f'{source["id"]}: mais de {MAX_STEPS} etapas')
        per_topic[source['topic']] = per_topic.get(source['topic'], 0) + 1
        if per_topic[source['topic']] > MAX_PER_TOPIC:
            raise ValueError(f'{source["topic"]}: mais de {MAX_PER_TOPIC} animações')
        svg = Sketch(source).svg()
        if len(svg.encode()) > MAX_BYTES:
            raise ValueError(f'{source["id"]}: {len(svg.encode())} bytes, acima de {MAX_BYTES}')
        outputs[source['id'] + '.svg'] = svg
        entry = {key: source[key] for key in META}
        entry['file'] = source['id'] + '.svg'
        if source['mode'] == 'steps':
            entry['steps'] = source['steps']
        entries.append(entry)
    entries.sort(key=lambda entry: (entry['topic'], entry['id']))
    outputs['manifest.json'] = json.dumps({'format': 'lesson-animations/v1', 'animations': entries},
                                          ensure_ascii=False, indent=2) + '\n'
    return outputs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    folder = args.root / FOLDER
    outputs = build(args.root)
    present = {path.name for path in folder.glob('*.svg')} | {'manifest.json'}
    if args.check:
        if present != set(outputs):
            raise SystemExit('Arquivos de animação divergentes das fontes: ' + ', '.join(sorted(present ^ set(outputs))))
        for name, text in outputs.items():
            if (folder / name).read_text(encoding='utf-8') != text:
                raise SystemExit('Animação desatualizada; rode scripts/sketch_svg.py: ' + name)
    else:
        for name in present - set(outputs):
            (folder / name).unlink()
        for name, text in outputs.items():
            (folder / name).write_text(text, encoding='utf-8', newline='\n')
    print(json.dumps({'animations': len(outputs) - 1, 'status': 'verified' if args.check else 'generated'}))


if __name__ == '__main__':
    try:
        main()
    except ValueError as error:
        sys.exit(str(error))
