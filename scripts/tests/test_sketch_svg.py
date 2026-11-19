"""Deterministic hand-drawn animation generator and its limits."""
import copy
import json
import re
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'scripts'))
from sketch_svg import FOLDER, MAX_BYTES, Sketch, build

SOURCE = {
    'id': 'demo', 'topic': 'I05', 'anchor': 's2', 'title': 'Demonstração', 'description': 'Descrição completa.',
    'mode': 'steps', 'duration': 6, 'poster': 5, 'seed': 1, 'size': [480, 200],
    'steps': [{'time': 1, 'caption': 'Primeira'}, {'time': 5, 'caption': 'Segunda'}],
    'elements': [
        {'type': 'box', 'x': 10, 'y': 10, 'w': 100, 'h': 40, 'label': 'A1'},
        {'type': 'text', 'x': 200, 'y': 40, 'text': 'some', 'show': [0, 3]},
        {'type': 'arrow', 'points': [[10, 100], [200, 100]], 'draw': [1, 2]},
        {'type': 'group', 'show': [2], 'move': [{'to': [40, 0], 'at': [2, 4]}],
         'children': [{'type': 'circle', 'cx': 300, 'cy': 100, 'r': 20, 'fill': 'accent-soft'}]},
    ],
}


class SketchTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        (self.root / FOLDER / 'fontes').mkdir(parents=True)

    def tearDown(self):
        self.temporary.cleanup()

    def write(self, *sources):
        for source in sources:
            (self.root / FOLDER / 'fontes' / (source['id'] + '.json')).write_text(json.dumps(source), encoding='utf-8')
        return build(self.root)

    def test_committed_animations_match_their_sources(self):
        outputs = build(ROOT)
        folder = ROOT / FOLDER
        self.assertEqual({path.name for path in folder.glob('*.svg')} | {'manifest.json'}, set(outputs))
        for name, text in outputs.items():
            self.assertEqual(text, (folder / name).read_text(encoding='utf-8'), name)

    def test_output_is_deterministic_and_builds_the_manifest(self):
        first = self.write(SOURCE)
        second = build(self.root)
        self.assertEqual(first, second)
        manifest = json.loads(first['manifest.json'])
        self.assertEqual('lesson-animations/v1', manifest['format'])
        entry = manifest['animations'][0]
        self.assertEqual('demo.svg', entry['file'])
        self.assertEqual(SOURCE['steps'], entry['steps'])
        self.assertNotIn('elements', entry)

    def test_every_animation_spans_the_whole_timeline_from_zero(self):
        svg = self.write(SOURCE)['demo.svg']
        animations = re.findall(r'<animate(?:Transform)? [^>]*>', svg)
        self.assertGreaterEqual(len(animations), 4)
        for element in animations:
            self.assertIn('begin="0s"', element)
            self.assertIn('dur="6s"', element)
            self.assertIn('fill="freeze"', element)
            times = re.search(r'keyTimes="([^"]+)"', element)[1].split(';')
            self.assertEqual(('0', '1'), (times[0], times[-1]))
            self.assertEqual(times, sorted(times, key=float))

    def test_static_attributes_show_the_poster_state(self):
        svg = Sketch(copy.deepcopy(SOURCE)).svg()
        self.assertIn('<g opacity="0">', svg)  # text hidden after 3 s
        self.assertIn('opacity="1" transform="translate(40 0)"', svg)  # moved group at the poster
        self.assertIn('stroke-dashoffset="0"', svg)  # arrow fully drawn

    def test_limits_fail_with_the_animation_named(self):
        cases = {
            'ciclo': dict(SOURCE, mode='cycle', duration=13, poster=1),
            'largura': dict(SOURCE, size=[481, 200]),
            'texto': dict(SOURCE, elements=[{'type': 'text', 'x': 1, 'y': 1, 'text': 'x', 'size': 16}]),
            'cor': dict(SOURCE, elements=[{'type': 'box', 'x': 1, 'y': 1, 'w': 1, 'h': 1, 'fill': 'red'}]),
            'tipo': dict(SOURCE, elements=[{'type': 'image'}]),
            'etapas': dict(SOURCE, steps=[{'time': i, 'caption': str(i)} for i in range(9)]),
            'peso': dict(SOURCE, elements=[{'type': 'circle', 'cx': i, 'cy': 50, 'r': 9} for i in range(400)]),
        }
        for name, source in cases.items():
            with self.subTest(name=name), self.assertRaises(ValueError):
                for path in (self.root / FOLDER / 'fontes').glob('*.json'):
                    path.unlink()
                self.write(source)

    def test_at_most_two_animations_per_lesson_and_matching_file_names(self):
        with self.assertRaises(ValueError):
            self.write(*(dict(SOURCE, id=f'demo{index}') for index in range(3)))
        for path in (self.root / FOLDER / 'fontes').glob('*.json'):
            path.unlink()
        (self.root / FOLDER / 'fontes' / 'other.json').write_text(json.dumps(SOURCE), encoding='utf-8')
        with self.assertRaises(ValueError):
            build(self.root)

    def test_weight_limit_matches_the_spec(self):
        self.assertEqual(30_000, MAX_BYTES)


if __name__ == '__main__':
    unittest.main()
