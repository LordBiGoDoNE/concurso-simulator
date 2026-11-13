import { describe, expect, it } from 'vitest';
import { THEME_KEY } from './theme';
import tokensCss from './tokens.css?raw';
import indexHtml from '../index.html?raw';

const stylesheets = import.meta.glob<string>('./**/*.css', { query: '?raw', import: 'default', eager: true });

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
  ['ink', 'teal-soft', 4.5], ['muted', 'teal-soft', 4.5], ['teal', 'teal-soft', 4.5],
  ['control-border', 'paper', 3], ['control-border', 'canvas', 3],
  ['focus', 'canvas', 3], ['focus', 'paper', 3], ['focus', 'blue-soft', 3], ['focus', 'surface-tint', 3],
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
    const literal = /#[0-9a-fA-F]{3,8}\b|\b(?:rgba?|hsla?|oklch|lab|lch)\(|\b(?:white|black|red|green|blue|gray|grey|orange|yellow)\b(?!-)/;
    const offenders: string[] = [];
    const files = Object.keys(stylesheets).filter(file => file !== './tokens.css');
    expect(files).toContain('./style.css');
    for (const file of files) {
      stylesheets[file].split('\n').forEach((line, index) => {
        const withoutVars = line.replace(/var\(--[\w-]+\)/g, '');
        if (literal.test(withoutVars)) offenders.push(`${file}:${index + 1}`);
      });
    }
    expect(offenders).toEqual([]);
  });

  it('aplica o tema salvo antes da primeira pintura com a mesma chave', () => {
    const html = indexHtml;
    expect(html).toContain(`localStorage.getItem('${THEME_KEY}')`);
    expect(html.indexOf(THEME_KEY)).toBeLessThan(html.indexOf('/src/main.tsx'));
  });
});
