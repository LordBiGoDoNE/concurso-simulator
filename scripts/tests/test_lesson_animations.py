"""Animation inventory and SMIL allowlist of the native lesson compiler."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'scripts'))
from compile_native_lessons import ANIMATIONS, Compiler, load_animations, place

SVG = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 50">{}</svg>'
VALID = SVG.format(
    '<rect id="box" x="0" y="0" width="10" height="10" opacity="0">'
    '<animate id="show" attributeName="opacity" from="0" to="1" begin="0s" dur="1s" fill="freeze"/></rect>'
    '<circle r="3" visibility="hidden"><set attributeName="visibility" to="visible" begin="show.end+0.5s"/></circle>'
    '<path id="route" d="M0 0 L90 40"/>'
    '<circle r="2"><animateMotion dur="2s" begin="show.end" repeatCount="indefinite"><mpath href="#route"/></animateMotion></circle>'
    '<g><animateTransform attributeName="transform" type="translate" values="0 0;5 0" dur="1s" repeatCount="2"/></g>')


def cycle(**changes):
    value = {key: item for key, item in entry(mode='cycle', duration=6, poster=3).items() if key != 'steps'}
    value.update(changes)
    return value


def entry(**changes):
    value = {'id': 'demo', 'topic': 'I05', 'anchor': 's2', 'file': 'demo.svg', 'title': 'Demonstração',
             'description': 'Descrição completa da animação.', 'mode': 'steps', 'duration': 4, 'poster': 4,
             'steps': [{'time': 0, 'caption': 'Primeira'}, {'time': 2, 'caption': 'Segunda'}]}
    value.update(changes)
    return value


class AnimationCompilerTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name).resolve()
        (self.root / ANIMATIONS).mkdir(parents=True)

    def tearDown(self):
        self.temporary.cleanup()

    def compile(self, svg, **changes):
        (self.root / ANIMATIONS / 'demo.svg').write_text(svg, encoding='utf-8')
        sources = {}
        return Compiler(self.root, 'I05', sources).animation(entry(**changes)), sources

    def manifest(self, *entries, **document):
        value = {'format': 'lesson-animations/v1', 'animations': list(entries)}
        value.update(document)
        (self.root / ANIMATIONS / 'manifest.json').write_text(json.dumps(value), encoding='utf-8')
        return load_animations(self.root, {}, {'I05', 'M12'})

    def test_valid_animation_becomes_a_native_node_with_prefixed_references(self):
        node, sources = self.compile(VALID)
        prefix = 'lesson-I05-animation-demo-'
        self.assertEqual(('animation', prefix[:-1], 'steps'), (node['tag'], node['id'], node['mode']))
        self.assertEqual([{'time': 0, 'caption': 'Primeira'}, {'time': 2, 'caption': 'Segunda'}], node['steps'])
        text = json.dumps(node)
        for expected in [prefix + 'box', prefix + 'show.end+0.5s', '#' + prefix + 'route', '"attributeName": "visibility"']:
            self.assertIn(expected, text)
        self.assertIn(ANIMATIONS + '/demo.svg', sources)

    def test_cycle_animation_has_no_steps(self):
        (self.root / ANIMATIONS / 'demo.svg').write_text(VALID, encoding='utf-8')
        node = Compiler(self.root, 'I05', {}).animation(cycle())
        self.assertNotIn('steps', node)

    def test_forbidden_animated_targets_fail(self):
        for name in ['href', 'xlink:href', 'style', 'onclick', 'class', 'fill', 'd']:
            with self.subTest(name=name), self.assertRaises(ValueError):
                self.compile(SVG.format(f'<g><animate attributeName="{name}" to="1" dur="1s"/></g>'))
        for markup in ['<set attributeName="href" to="javascript:alert(1)"/>',
                       '<animateTransform attributeName="opacity" type="translate" dur="1s"/>',
                       '<animateTransform attributeName="transform" type="matrix" dur="1s"/>',
                       '<animateMotion attributeName="x" dur="1s"/>']:
            with self.subTest(markup=markup), self.assertRaises(ValueError):
                self.compile(SVG.format('<g>' + markup + '</g>'))

    def test_user_events_and_outside_references_fail(self):
        cases = ['<animate attributeName="opacity" to="1" begin="click" dur="1s"/>',
                 '<animate attributeName="opacity" to="1" begin="indefinite" dur="1s"/>',
                 '<animate attributeName="opacity" to="1" begin="outside.end" dur="1s"/>',
                 '<animate attributeName="opacity" values="url(#x);1" dur="1s"/>',
                 '<animate attributeName="opacity" to="1" fill="red" dur="1s"/>',
                 '<animate attributeName="opacity" to="1" repeatCount="-1" dur="1s"/>',
                 '<animate attributeName="opacity" to="1" dur="soon"/>',
                 '<animateMotion dur="1s"><mpath href="https://example.test/p"/></animateMotion>',
                 '<animateMotion dur="1s"><mpath href="#missing"/></animateMotion>',
                 '<animate attributeName="opacity" to="1" dur="1s"><animate attributeName="opacity" to="0" dur="1s"/></animate>',
                 '<mpath href="#box"/>']
        for markup in cases:
            with self.subTest(markup=markup), self.assertRaises(ValueError):
                self.compile(SVG.format('<rect id="box" width="1" height="1"/><g>' + markup + '</g>'))

    def test_href_outside_mpath_fails_naming_animation_and_attribute(self):
        xlink = ' xmlns:xlink="http://www.w3.org/1999/xlink"'
        for markup, name in [('<animate href="#box" attributeName="opacity" to="1" dur="1s"/>', 'href'),
                             ('<set xlink:href="#box" attributeName="opacity" to="1"/>', 'xlink:href'),
                             ('<animateMotion href="#box" dur="1s"/>', 'href'),
                             ('<animateTransform href="#box" attributeName="transform" type="scale" dur="1s"/>', 'href'),
                             ('<rect href="#box" width="1" height="1"/>', 'href')]:
            with self.subTest(markup=markup), self.assertRaises(ValueError) as raised:
                self.compile(SVG.replace('<svg ', '<svg' + xlink + ' ').format('<rect id="box" width="1" height="1"/><g>' + markup + '</g>'))
            message = str(raised.exception)
            self.assertIn('Animação demo (' + ANIMATIONS + '/demo.svg)', message)
            self.assertIn(f'atributo {name} ', message)
        self.compile(SVG.replace('<svg ', '<svg' + xlink + ' ').format(
            '<path id="p" d="M0 0"/><circle r="1"><animateMotion dur="1s"><mpath xlink:href="#p"/></animateMotion></circle>'))

    def test_duplicate_id_in_animated_figure_fails_naming_animation_and_attribute(self):
        with self.assertRaises(ValueError) as raised:
            self.compile(SVG.format('<rect id="box" width="1" height="1"><animate id="box" attributeName="opacity" to="1" dur="1s"/></rect>'))
        self.assertIn('Animação demo (' + ANIMATIONS + '/demo.svg)', str(raised.exception))
        self.assertIn('atributo id duplicado na figura: box', str(raised.exception))

    def test_outside_reference_names_animation_and_missing_id(self):
        with self.assertRaises(ValueError) as raised:
            self.compile(SVG.format('<g><animate attributeName="opacity" to="1" begin="s2.end" dur="1s"/></g>'))
        self.assertIn('Animação demo (' + ANIMATIONS + '/demo.svg)', str(raised.exception))
        self.assertIn('fora da figura: s2', str(raised.exception))

    def test_timing_attributes_outside_animation_elements_and_static_figures_fail(self):
        with self.assertRaises(ValueError):
            self.compile(SVG.format('<rect width="1" height="1" dur="1s"/>'))
        (self.root / 'assets/esquemas').mkdir(parents=True)
        (self.root / 'assets/esquemas/E.svg').write_text(VALID, encoding='utf-8')
        compiler = Compiler(self.root, 'I05', {})
        node = type('Node', (), {'attrs': {'src': 'assets/esquemas/E.svg'}})()
        with self.assertRaises(ValueError):
            compiler.diagram(node, self.root / 'lesson.html')

    def test_svg_without_viewbox_or_with_declarations_fails(self):
        for svg in ['<svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>',
                    '<?xml version="1.0"?>' + VALID, '<g xmlns="http://www.w3.org/2000/svg"/>']:
            with self.subTest(svg=svg[:30]), self.assertRaises(ValueError):
                self.compile(svg)

    def test_manifest_accepts_valid_entries_grouped_by_topic(self):
        grouped = self.manifest(entry(), cycle(id='other', topic='M12'))
        self.assertEqual(['I05', 'M12'], sorted(grouped))

    def test_manifest_rejects_inconsistent_entries(self):
        cases = {
            'formato': ((), {'format': 'other'}),
            'ID repetido': ((entry(), entry()), {}),
            'aula desconhecida': ((entry(topic='P01'),), {}),
            'âncora': ((entry(anchor='1x'),), {}),
            'caminho': ((entry(file='../demo.svg'),), {}),
            'modo': ((entry(mode='loop'),), {}),
            'duração': ((entry(duration=0),), {}),
            'quadro': ((entry(poster=5),), {}),
            'ordem': ((entry(steps=[{'time': 2, 'caption': 'a'}, {'time': 1, 'caption': 'b'}]),), {}),
            'empate': ((entry(steps=[{'time': 1, 'caption': 'a'}, {'time': 1, 'caption': 'b'}]),), {}),
            'fora da duração': ((entry(steps=[{'time': 5, 'caption': 'a'}]),), {}),
            'legenda': ((entry(steps=[{'time': 0, 'caption': ' '}]),), {}),
            'mais de 8': ((entry(steps=[{'time': i, 'caption': str(i)} for i in range(9)], duration=9, poster=9),), {}),
            'sem etapas': ((entry(steps=[]),), {}),
            'etapas no ciclo': ((cycle(steps=[]),), {}),
            'campo extra': ((entry(extra=1),), {}),
            'título': ((entry(title=''),), {}),
        }
        for name, (entries, document) in cases.items():
            with self.subTest(name=name), self.assertRaises(ValueError):
                self.manifest(*entries, **document)

    def test_absent_manifest_means_no_animation(self):
        (self.root / ANIMATIONS).rmdir()
        self.assertEqual({}, load_animations(self.root, {}, {'I05'}))

    def test_animation_goes_to_the_end_of_exactly_one_anchor_section(self):
        node = {'tag': 'animation'}
        section = {'tag': 'section', 'attrs': {'id': 'lesson-I05-s2'}, 'children': ['texto']}
        children = [{'tag': 'section', 'attrs': {'id': 'lesson-I05-s1'}, 'children': []}, section]
        place(children, 'lesson-I05-s2', node)
        self.assertEqual(['texto', node], section['children'])
        for broken in [[], [section, section], [{'tag': 'div', 'attrs': {'id': 'lesson-I05-s2'}, 'children': []}]]:
            with self.subTest(broken=len(broken)), self.assertRaises(ValueError):
                place(broken, 'lesson-I05-s2', node)


if __name__ == '__main__':
    unittest.main()
