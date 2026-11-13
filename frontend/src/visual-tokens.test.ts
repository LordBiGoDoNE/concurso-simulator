import { describe, expect, it } from 'vitest';
import { THEME_KEY } from './theme';
import tokensCss from './tokens.css?raw';
import indexHtml from '../index.html?raw';

const stylesheets = import.meta.glob<string>('./**/*.css', { query: '?raw', import: 'default', eager: true });
const components = import.meta.glob<string>(['./**/*.tsx', '!./**/*.test.tsx'], { query: '?raw', import: 'default', eager: true });

// Cores literais: hexadecimal e funções de cor (inclui color-mix, que mistura com valores fixos).
const literalColor = /#[0-9a-fA-F]{3,8}\b|\b(?:rgba?|hsla?|hwb|oklch|oklab|lab|lch|color|color-mix)\(/i;
// Lista completa de nomes de cor do CSS (CSS Color 4), sem transparent/currentColor.
const COLOR_NAMES = new Set(('aliceblue antiquewhite aqua aquamarine azure beige bisque black blanchedalmond blue blueviolet brown burlywood cadetblue chartreuse '
  + 'chocolate coral cornflowerblue cornsilk crimson cyan darkblue darkcyan darkgoldenrod darkgray darkgreen darkgrey darkkhaki darkmagenta darkolivegreen '
  + 'darkorange darkorchid darkred darksalmon darkseagreen darkslateblue darkslategray darkslategrey darkturquoise darkviolet deeppink deepskyblue dimgray '
  + 'dimgrey dodgerblue firebrick floralwhite forestgreen fuchsia gainsboro ghostwhite gold goldenrod gray green greenyellow grey honeydew hotpink indianred '
  + 'indigo ivory khaki lavender lavenderblush lawngreen lemonchiffon lightblue lightcoral lightcyan lightgoldenrodyellow lightgray lightgreen lightgrey '
  + 'lightpink lightsalmon lightseagreen lightskyblue lightslategray lightslategrey lightsteelblue lightyellow lime limegreen linen magenta maroon '
  + 'mediumaquamarine mediumblue mediumorchid mediumpurple mediumseagreen mediumslateblue mediumspringgreen mediumturquoise mediumvioletred midnightblue '
  + 'mintcream mistyrose moccasin navajowhite navy oldlace olive olivedrab orange orangered orchid palegoldenrod palegreen paleturquoise palevioletred '
  + 'papayawhip peachpuff peru pink plum powderblue purple rebeccapurple red rosybrown royalblue saddlebrown salmon sandybrown seagreen seashell sienna '
  + 'silver skyblue slateblue slategray slategrey snow springgreen steelblue tan teal thistle tomato turquoise violet wheat white whitesmoke yellow yellowgreen').split(' '));
const colorProperty = /^(?:color|background(?:-color)?|border(?:-[\w-]+)?|outline(?:-color)?|fill|stroke|box-shadow|text-shadow|text-decoration(?:-color)?|caret-color|accent-color|column-rule(?:-color)?|backgroundColor|borderColor)$/i;
const withoutVars = (value: string) => value.replace(/var\(--[\w-]+\)/g, '');
const namedColor = (value: string) => (value.toLowerCase().match(/[a-z]+/g) ?? []).some(word => COLOR_NAMES.has(word));

function block(selectorPattern: RegExp): Record<string, string> {
  const match = selectorPattern.exec(tokensCss);
  if (!match) throw new Error('bloco de tokens ausente: ' + selectorPattern);
  const body = tokensCss.slice(match.index + match[0].length, tokensCss.indexOf('}', match.index + match[0].length));
  return Object.fromEntries([...body.matchAll(/--([\w-]+):\s*([^;]+);/g)].map(m => [m[1], m[2].trim()]));
}

const light = block(/^:root \{/m);
const darkMedia = block(/:root:not\(\[data-theme="light"\]\) \{/);
const darkManual = block(/:root\[data-theme="dark"\] \{/);
const themes = { claro: light, escuro: { ...light, ...darkManual } } as const;

function luminance(hex: string): number {
  const h = hex.replace('#', '');
  const full = h.length === 3 ? [...h].map(c => c + c).join('') : h.slice(0, 6);
  const [r, g, b] = [0, 2, 4].map(i => parseInt(full.slice(i, i + 2), 16) / 255)
    .map(v => v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4);
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}
function contrast(a: string, b: string): number {
  const [x, y] = [luminance(a), luminance(b)].sort((p, q) => q - p);
  return (x + 0.05) / (y + 0.05);
}

// [frente, fundo, mínimo]: 4,5 para texto; 3 para bordas de controle, foco e ícones.
const pairs: [string, string, number][] = [
  ['ink', 'canvas', 4.5], ['ink', 'paper', 4.5], ['ink', 'blue-soft', 4.5], ['ink', 'surface-tint', 4.5],
  ['muted', 'canvas', 4.5], ['muted', 'paper', 4.5], ['muted', 'blue-soft', 4.5], ['muted', 'surface-tint', 4.5],
  ['blue', 'paper', 4.5], ['blue', 'canvas', 4.5], ['blue', 'blue-soft', 4.5],
  ['on-blue', 'blue', 4.5], ['on-blue', 'blue-hover', 4.5], ['on-blue', 'ink', 4.5],
  ['teal', 'paper', 4.5], ['teal', 'canvas', 4.5], ['teal', 'blue-soft', 4.5], ['teal', 'surface-tint', 4.5],
  ['warning', 'paper', 4.5], ['warning', 'canvas', 4.5], ['warning', 'blue-soft', 4.5], ['warning', 'surface-tint', 4.5],
  // Fundos das aulas (resumo e barra de percentual) também são superfície de texto.
  ['ink', 'teal-soft', 4.5], ['muted', 'teal-soft', 4.5], ['teal', 'teal-soft', 4.5], ['blue', 'teal-soft', 4.5],
  ['control-border', 'paper', 3], ['control-border', 'canvas', 3],
  // Contorno do estado "respondida" no mapa de questões: indicador de estado, não decoração.
  ['blue-border', 'paper', 3], ['blue-border', 'blue-soft', 3],
  ['focus', 'canvas', 3], ['focus', 'paper', 3], ['focus', 'blue-soft', 3], ['focus', 'surface-tint', 3], ['focus', 'teal-soft', 3],
];

describe('tokens visuais', () => {
  it('declara o mesmo tema escuro pela preferência do sistema e pela escolha manual', () => {
    expect(darkMedia).toEqual(darkManual);
    expect(Object.keys(darkManual).every(name => name in light)).toBe(true);
  });

  for (const [theme, values] of Object.entries(themes)) {
    it.each(pairs)(`mantém contraste no tema ${theme}: %s sobre %s ≥ %s:1`, (fg, bg, min) => {
      expect(values[fg], fg).toMatch(/^#[0-9a-f]{6}$/);
      expect(values[bg], bg).toMatch(/^#[0-9a-f]{6}$/);
      expect(contrast(values[fg], values[bg])).toBeGreaterThanOrEqual(min);
    });
  }

  it('não declara cores literais fora de tokens.css', () => {
    // Exceções permitidas: transparent, currentColor, inherit (não são cores fixas).
    const offenders: string[] = [];
    const files = Object.keys(stylesheets).filter(file => file !== './tokens.css');
    expect(files).toContain('./style.css');
    for (const file of files) {
      stylesheets[file].split('\n').forEach((line, index) => {
        if (literalColor.test(withoutVars(line))) offenders.push(`${file}:${index + 1}`);
        // Nomes de cor só contam em valores de propriedades de cor, para não confundir com classes como .blue-soft.
        for (const [, property, value] of line.matchAll(/([\w-]+)\s*:\s*([^;{}]+)/g)) {
          if (colorProperty.test(property) && namedColor(withoutVars(value))) offenders.push(`${file}:${index + 1}`);
        }
      });
    }
    expect(offenders).toEqual([]);
  });

  it('não declara cores literais em estilos inline dos componentes', () => {
    const offenders: string[] = [];
    for (const [file, source] of Object.entries(components)) {
      for (const [style] of source.matchAll(/style=\{\{[^}]*\}\}/g)) {
        const values = [...style.matchAll(/['"`]([^'"`]*)['"`]/g)].map(m => withoutVars(m[1]));
        if (values.some(value => literalColor.test(value) || namedColor(value))) offenders.push(`${file}: ${style}`);
      }
    }
    expect(offenders).toEqual([]);
  });

  it('a verificação de cores reconhece nomes, funções e literais', () => {
    for (const value of ['navy', 'Teal', 'silver', 'rebeccapurple', '1px solid gray']) expect(namedColor(value), value).toBe(true);
    for (const value of ['#abc', 'rgb(0 0 0)', 'hwb(0 0% 0%)', 'color(srgb 1 0 0)', 'color-mix(in srgb, var(--x), white)']) expect(literalColor.test(value), value).toBe(true);
    for (const value of ['transparent', 'currentColor', 'inherit', 'var(--blue-soft)', '1px solid var(--line)']) {
      expect(literalColor.test(withoutVars(value)) || namedColor(withoutVars(value)), value).toBe(false);
    }
    expect(colorProperty.test('border-radius') && namedColor('19px')).toBe(false);
  });

  it('aplica o tema salvo antes da primeira pintura com a mesma chave', () => {
    const html = indexHtml;
    expect(html).toContain(`localStorage.getItem('${THEME_KEY}')`);
    expect(html.indexOf(THEME_KEY)).toBeLessThan(html.indexOf('/src/main.tsx'));
  });
});
