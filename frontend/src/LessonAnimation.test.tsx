import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import NativeLesson from './NativeLesson';
import type { AnimationNode, NativeLessonDocument } from './native-lesson';

const prefix = 'lesson-I05-animation-demo-';
const svg = { tag: 'svg', attrs: { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 100 50' }, children: [
  { tag: 'rect', attrs: { id: prefix + 'box', width: '10', height: '10', opacity: '1' }, children: [
    { tag: 'animate', attrs: { attributeName: 'opacity', values: '0;1', keyTimes: '0;1', dur: '6s', begin: '0s', fill: 'freeze' }, children: [] }] }] };
const steps: AnimationNode = { tag: 'animation', id: prefix.slice(0, -1), mode: 'steps', title: 'Alça de preenchimento',
  description: 'Descrição completa da figura.', duration: 6, poster: 5,
  steps: [{ time: 1, caption: 'Primeira etapa' }, { time: 3, caption: 'Segunda etapa' }, { time: 5, caption: 'Terceira etapa' }], svg };
const { steps: _unused, ...cycleBase } = steps;
const cycle: AnimationNode = { ...cycleBase, mode: 'cycle', title: 'Casa dos pombos', poster: 4 };
const lesson = (node: AnimationNode): NativeLessonDocument => ({ format: 'native-lesson/v1', topicId: 'I05', title: 'Aula',
  children: [{ tag: 'section', attrs: { id: 'lesson-I05-s2' }, children: [{ tag: 'h2', children: ['Tipos'] }, node] }] });

let clock: { time: number; paused: boolean };
let observers: ((entries: { isIntersecting: boolean }[]) => void)[];
let reduced: boolean;
beforeEach(() => {
  clock = { time: 0, paused: false }; observers = []; reduced = false;
  Object.assign(SVGSVGElement.prototype, {
    pauseAnimations: vi.fn(() => { clock.paused = true; }),
    unpauseAnimations: vi.fn(() => { clock.paused = false; }),
    setCurrentTime: vi.fn((time: number) => { clock.time = time; }),
    getCurrentTime: vi.fn(() => clock.time),
  });
  vi.stubGlobal('IntersectionObserver', class { constructor(callback: (entries: { isIntersecting: boolean }[]) => void) { observers.push(callback); } observe() {} disconnect() {} });
  vi.stubGlobal('matchMedia', (query: string) => ({ matches: reduced && query.includes('reduce'), addEventListener() {}, removeEventListener() {} }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });
async function open(node: AnimationNode) {
  vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, json: async () => lesson(node) })));
  render(<NativeLesson topicId="I05" />);
  return screen.findByRole('figure', { name: node.title });
}

it('modo etapas começa parado na primeira etapa e navega com legenda anunciada', async () => {
  const figure = await open(steps);
  expect(clock).toEqual({ time: 1, paused: true });
  const controls = within(figure).getByRole('group', { name: 'Controles da animação: Alça de preenchimento' });
  expect(within(controls).getByText('Etapa 1 de 3')).toBeVisible();
  expect(within(controls).getByRole('button', { name: 'Anterior' })).toBeDisabled();
  const caption = within(figure).getByText('Primeira etapa');
  expect(caption).toHaveAttribute('aria-live', 'polite');
  fireEvent.click(within(controls).getByRole('button', { name: 'Próxima' }));
  expect(within(controls).getByText('Etapa 2 de 3')).toBeVisible();
  expect(caption).toHaveTextContent('Segunda etapa');
  expect(clock).toEqual({ time: 3, paused: true });
  fireEvent.click(within(controls).getByRole('button', { name: 'Próxima' }));
  expect(within(controls).getByRole('button', { name: 'Próxima' })).toBeDisabled();
  fireEvent.click(within(controls).getByRole('button', { name: 'Anterior' }));
  expect(clock.time).toBe(3);
  expect(within(figure).getByRole('img', { name: 'Alça de preenchimento Descrição completa da figura.' })).toBeInTheDocument();
  expect(within(figure).getByText('Descrição completa da figura.')).toBeVisible();
});

it('reproduzir tudo avança as etapas pelo tempo e para no fim', async () => {
  const figure = await open(steps);
  fireEvent.click(within(figure).getByRole('button', { name: 'Reproduzir tudo' }));
  expect(clock.paused).toBe(false);
  expect(within(figure).getByRole('button', { name: 'Pausar' })).toHaveAttribute('aria-pressed', 'true');
  await act(async () => { clock.time = 3.2; await new Promise(resolve => requestAnimationFrame(resolve)); });
  expect(within(figure).getByText('Etapa 2 de 3')).toBeVisible();
  await act(async () => { clock.time = 6; await new Promise(resolve => requestAnimationFrame(resolve)); await new Promise(resolve => requestAnimationFrame(resolve)); });
  expect(within(figure).getByText('Etapa 3 de 3')).toBeVisible();
  expect(within(figure).getByRole('button', { name: 'Reproduzir tudo' })).toBeInTheDocument();
  expect(clock).toEqual({ time: 5, paused: true });
});

it('ciclo começa no quadro estático, reproduz só visível e pausa a pedido', async () => {
  const figure = await open(cycle);
  expect(clock).toEqual({ time: 4, paused: true });
  expect(within(figure).queryByText(/Etapa/)).toBeNull();
  act(() => observers.forEach(callback => callback([{ isIntersecting: true }])));
  expect(clock.paused).toBe(false);
  fireEvent.click(within(figure).getByRole('button', { name: 'Pausar' }));
  expect(clock.paused).toBe(true);
  act(() => observers.forEach(callback => callback([{ isIntersecting: false }])));
  act(() => observers.forEach(callback => callback([{ isIntersecting: true }])));
  expect(clock.paused).toBe(true);
  fireEvent.click(within(figure).getByRole('button', { name: 'Reproduzir' }));
  expect(clock.paused).toBe(false);
  act(() => observers.forEach(callback => callback([{ isIntersecting: false }])));
  expect(clock.paused).toBe(true);
});

it('com movimento reduzido nada começa sozinho, mas o estudante pode reproduzir', async () => {
  reduced = true;
  const figure = await open(cycle);
  act(() => observers.forEach(callback => callback([{ isIntersecting: true }])));
  expect(clock).toEqual({ time: 4, paused: true });
  fireEvent.click(within(figure).getByRole('button', { name: 'Reproduzir' }));
  expect(clock.paused).toBe(false);
});

it('sem suporte a SMIL a figura aparece com o estado estático e controles inofensivos', async () => {
  for (const method of ['pauseAnimations', 'unpauseAnimations', 'setCurrentTime', 'getCurrentTime'] as const) delete (SVGSVGElement.prototype as Partial<SVGSVGElement>)[method];
  const figure = await open(steps);
  fireEvent.click(within(figure).getByRole('button', { name: 'Próxima' }));
  expect(within(figure).getByText('Segunda etapa')).toBeVisible();
  expect(figure.querySelector('rect')).toHaveAttribute('opacity', '1');
});
