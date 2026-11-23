import { render, screen, within } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import NativeLesson from './NativeLesson';
import { parseNativeLesson, type LessonNode } from './native-lesson';

const prefix = 'lesson-I05-animation-demo-';
type Element = { tag: string; attrs?: Record<string, string>; children: unknown[] };
const svg = (...children: Element[]): Element => ({
  tag: 'svg',
  attrs: { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 100 50' },
  children,
});
const animation = (
  overrides: Record<string, unknown> = {},
  figure: Element = svg(
    {
      tag: 'rect',
      attrs: { id: prefix + 'box', width: '10', height: '10', opacity: '0' },
      children: [
        {
          tag: 'animate',
          attrs: {
            id: prefix + 'show',
            attributeName: 'opacity',
            from: '0',
            to: '1',
            begin: '0s',
            dur: '1s',
            fill: 'freeze',
          },
          children: [],
        },
      ],
    },
    {
      tag: 'circle',
      attrs: { r: '3', visibility: 'hidden' },
      children: [
        {
          tag: 'set',
          attrs: { attributeName: 'visibility', to: 'visible', begin: prefix + 'show.end+0.5s' },
          children: [],
        },
      ],
    },
    { tag: 'path', attrs: { id: prefix + 'route', d: 'M0 0 L90 40' }, children: [] },
    {
      tag: 'circle',
      attrs: { r: '2' },
      children: [
        {
          tag: 'animateMotion',
          attrs: { dur: '2s', repeatCount: 'indefinite' },
          children: [{ tag: 'mpath', attrs: { href: '#' + prefix + 'route' }, children: [] }],
        },
      ],
    },
  ),
) => ({
  tag: 'animation',
  id: prefix.slice(0, -1),
  mode: 'steps',
  title: 'Alça de preenchimento',
  description: 'Descrição completa.',
  duration: 4,
  poster: 4,
  steps: [
    { time: 0, caption: 'Primeira etapa' },
    { time: 2, caption: 'Segunda etapa' },
  ],
  svg: figure,
  ...overrides,
});
const lesson = (...children: unknown[]) => ({ format: 'native-lesson/v1', topicId: 'I05', title: 'Aula', children });
const parse = (...children: unknown[]) => parseNativeLesson(lesson(...children), 'I05');
const withAnimate = (attrs: Record<string, string>, children: Element[] = []) =>
  animation(
    {},
    svg(
      { tag: 'rect', attrs: { id: prefix + 'box', width: '1', height: '1' }, children: [] },
      { tag: 'g', children: [{ tag: 'animate', attrs, children }] },
    ),
  );
afterEach(() => vi.unstubAllGlobals());

it('aceita animação declarativa válida nos modos etapas e ciclo', () => {
  expect(parse(animation())).toBeTruthy();
  const { steps: _steps, ...cycle } = animation({ mode: 'cycle', poster: 2 });
  expect(parse(cycle)).toBeTruthy();
});

it.each([
  ['href', { attributeName: 'href', to: 'javascript:alert(1)', dur: '1s' }],
  ['xlink:href', { attributeName: 'xlink:href', to: '#x', dur: '1s' }],
  ['style', { attributeName: 'style', to: 'color:red', dur: '1s' }],
  ['evento', { attributeName: 'onclick', to: 'alert(1)', dur: '1s' }],
  ['fill', { attributeName: 'fill', to: 'red', dur: '1s' }],
  ['início por clique', { attributeName: 'opacity', to: '1', begin: 'click', dur: '1s' }],
  ['início indefinido', { attributeName: 'opacity', to: '1', begin: 'indefinite', dur: '1s' }],
  ['referência externa', { attributeName: 'opacity', to: '1', begin: 'lesson-P01-x.end', dur: '1s' }],
  ['ID ausente', { attributeName: 'opacity', to: '1', begin: prefix + 'missing.end', dur: '1s' }],
  ['url', { attributeName: 'opacity', values: 'url(#x);1', dur: '1s' }],
  ['fill de animação', { attributeName: 'opacity', to: '1', fill: 'red', dur: '1s' }],
  ['repeatCount', { attributeName: 'opacity', to: '1', repeatCount: '-1', dur: '1s' }],
  ['duração', { attributeName: 'opacity', to: '1', dur: 'logo' }],
])('recusa animação insegura: %s', (_name, attrs) => {
  expect(() => parse(withAnimate(attrs))).toThrow();
});

it.each([
  ['dur numérico', { attributeName: 'opacity', to: '1', dur: 1 }],
  ['fill em lista', { attributeName: 'opacity', to: '1', dur: '1s', fill: ['freeze'] }],
  ['begin em lista', { attributeName: 'opacity', to: '1', dur: '1s', begin: ['0s'] }],
])('recusa valor de atributo de animação que não é texto: %s', (_name, attrs) => {
  expect(() => parse(withAnimate(attrs as unknown as Record<string, string>))).toThrow();
});

it('recusa tom de bloco que não é texto, mesmo que a lista contenha um tom válido', () => {
  expect(() => parse({ tag: 'section', tone: ['note'], children: ['Texto'] })).toThrow();
  expect(parse({ tag: 'section', tone: 'note', children: ['Texto'] })).toBeTruthy();
});

it('recusa elementos de animação fora da figura animada, aninhados ou com alvo externo', () => {
  expect(() =>
    parse({
      tag: 'svg',
      attrs: { viewBox: '0 0 1 1' },
      children: [{ tag: 'animate', attrs: { attributeName: 'opacity', dur: '1s' }, children: [] }],
    }),
  ).toThrow();
  expect(() => parse(svg({ tag: 'rect', attrs: { dur: '1s' }, children: [] }))).toThrow();
  expect(() =>
    parse(
      withAnimate({ attributeName: 'opacity', dur: '1s' }, [
        { tag: 'animate', attrs: { attributeName: 'opacity', dur: '1s' }, children: [] },
      ]),
    ),
  ).toThrow();
  expect(() =>
    parse(
      animation(
        {},
        svg({
          tag: 'circle',
          children: [
            {
              tag: 'animateMotion',
              attrs: { dur: '1s' },
              children: [{ tag: 'mpath', attrs: { href: 'https://example.test/p' }, children: [] }],
            },
          ],
        }),
      ),
    ),
  ).toThrow();
  expect(() =>
    parse(animation({}, svg({ tag: 'mpath', attrs: { href: '#' + prefix + 'box' }, children: [] }))),
  ).toThrow();
  expect(() =>
    parse(
      animation(
        {},
        svg({
          tag: 'g',
          children: [
            { tag: 'animateTransform', attrs: { attributeName: 'transform', type: 'matrix', dur: '1s' }, children: [] },
          ],
        }),
      ),
    ),
  ).toThrow();
  expect(() => parse({ tag: 'animation', children: [animation()] })).toThrow();
});

it('recusa begin e mpath que apontam para ID existente na aula, mas fora da própria figura', () => {
  const outside = {
    tag: 'section',
    attrs: { id: 'lesson-I05-s2' },
    children: [
      {
        tag: 'svg',
        attrs: { viewBox: '0 0 1 1' },
        children: [{ tag: 'path', attrs: { id: 'lesson-I05-figure-1-x', d: 'M0 0' }, children: [] }],
      },
    ],
  };
  expect(parse(outside, animation())).toBeTruthy();
  expect(() =>
    parse(outside, withAnimate({ attributeName: 'opacity', to: '1', begin: 'lesson-I05-s2.end', dur: '1s' })),
  ).toThrow();
  expect(() =>
    parse(
      outside,
      animation(
        {},
        svg({
          tag: 'circle',
          children: [
            {
              tag: 'animateMotion',
              attrs: { dur: '1s' },
              children: [{ tag: 'mpath', attrs: { href: '#lesson-I05-figure-1-x' }, children: [] }],
            },
          ],
        }),
      ),
    ),
  ).toThrow();
  const other = animation(
    { id: 'lesson-I05-animation-other' },
    svg({ tag: 'rect', attrs: { id: 'lesson-I05-animation-other-box' }, children: [] }),
  );
  expect(parse(other, animation())).toBeTruthy();
  expect(() =>
    parse(
      other,
      withAnimate({ attributeName: 'opacity', to: '1', begin: 'lesson-I05-animation-other-box.end', dur: '1s' }),
    ),
  ).toThrow();
});

it('recusa href em elemento de animação que não seja mpath e ID duplicado na figura', () => {
  expect(() =>
    parse(withAnimate({ attributeName: 'opacity', to: '1', dur: '1s', href: '#' + prefix + 'box' })),
  ).toThrow();
  expect(() =>
    parse(withAnimate({ attributeName: 'opacity', to: '1', dur: '1s', 'xlink:href': '#' + prefix + 'box' })),
  ).toThrow();
  expect(() => parse(withAnimate({ id: prefix + 'box', attributeName: 'opacity', to: '1', dur: '1s' }))).toThrow();
});

it.each([
  ['modo', { mode: 'loop' }],
  ['título', { title: ' ' }],
  ['duração', { duration: 0 }],
  ['quadro', { poster: 5 }],
  ['ID', { id: 'lesson-P01-animation-demo' }],
  [
    'ordem',
    {
      steps: [
        { time: 2, caption: 'a' },
        { time: 1, caption: 'b' },
      ],
    },
  ],
  ['legenda', { steps: [{ time: 0, caption: '' }] }],
  [
    'mais de 8',
    { duration: 9, poster: 9, steps: Array.from({ length: 9 }, (_, time) => ({ time, caption: String(time) })) },
  ],
  ['campo extra', { extra: true }],
  ['sem viewBox', { svg: { tag: 'svg', attrs: {}, children: [] } }],
])('recusa metadados inconsistentes: %s', (_name, overrides) => {
  expect(() => parse(animation(overrides))).toThrow();
});

it('renderiza a animação como figura com título, descrição e SVG acessível', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => ({
      ok: true,
      json: async () =>
        lesson({
          tag: 'section',
          attrs: { id: 'lesson-I05-s2' },
          children: [{ tag: 'h2', children: ['Tipos'] }, animation() as unknown as LessonNode],
        }),
    })),
  );
  render(<NativeLesson topicId="I05" />);
  const figure = await screen.findByRole('figure', { name: 'Alça de preenchimento' });
  expect(within(figure).getByRole('img', { name: 'Alça de preenchimento Descrição completa.' })).toBeInTheDocument();
  expect(within(figure).getByText('Descrição completa.')).toBeVisible();
  expect(figure.querySelector('animateMotion mpath')?.getAttribute('href')).toBe('#' + prefix + 'route');
  expect(figure.querySelector('set')?.getAttribute('begin')).toBe(prefix + 'show.end+0.5s');
});
