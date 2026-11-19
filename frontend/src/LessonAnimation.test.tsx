import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { createRoot } from 'react-dom/client';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import LessonAnimation from './LessonAnimation';
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
let mediaListeners: (() => void)[];
let disconnect: ReturnType<typeof vi.fn<() => void>>;
const setReduced = (value: boolean) => act(() => { reduced = value; mediaListeners.forEach(listener => listener()); });
const intersect = (isIntersecting: boolean) => act(() => observers.forEach(callback => callback([{ isIntersecting }])));
const frame = () => act(async () => { await new Promise(resolve => requestAnimationFrame(resolve)); });
beforeEach(() => {
  clock = { time: 0, paused: false }; observers = []; reduced = false; mediaListeners = []; disconnect = vi.fn();
  Object.assign(SVGSVGElement.prototype, {
    pauseAnimations: vi.fn(() => { clock.paused = true; }),
    unpauseAnimations: vi.fn(() => { clock.paused = false; }),
    setCurrentTime: vi.fn((time: number) => { clock.time = time; }),
    getCurrentTime: vi.fn(() => clock.time),
  });
  vi.stubGlobal('IntersectionObserver', class { constructor(callback: (entries: { isIntersecting: boolean }[]) => void) { observers.push(callback); } observe() {} disconnect() { disconnect(); } });
  vi.stubGlobal('matchMedia', (query: string) => ({
    get matches() { return reduced && query.includes('reduce'); },
    addEventListener: (_type: string, listener: () => void) => { mediaListeners.push(listener); },
    removeEventListener: (_type: string, listener: () => void) => { mediaListeners = mediaListeners.filter(item => item !== listener); },
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });
async function open(node: AnimationNode) {
  vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, json: async () => lesson(node) })));
  render(<NativeLesson topicId="I05" />);
  const figure = await screen.findByRole('figure', { name: node.title });
  // The lesson commits outside act (after fetch) and its mount effects run in a later task.
  // They all run together, so the reduced-motion subscription marks that the observer exists too.
  await waitFor(() => expect(mediaListeners).not.toHaveLength(0));
  return figure;
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
  expect(within(figure).getByRole('button', { name: 'Pausar' })).not.toHaveAttribute('aria-pressed');
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

it('reproduzir tudo depois da última etapa recomeça do início', async () => {
  const figure = await open(steps);
  fireEvent.click(within(figure).getByRole('button', { name: 'Próxima' }));
  fireEvent.click(within(figure).getByRole('button', { name: 'Próxima' }));
  expect(clock).toEqual({ time: 5, paused: true });
  fireEvent.click(within(figure).getByRole('button', { name: 'Reproduzir tudo' }));
  expect(clock).toEqual({ time: 0, paused: false });
  await frame();
  expect(within(figure).getByText('Etapa 1 de 3')).toBeVisible();
});

it('ciclo volta ao início ao chegar ao fim da duração e segue reproduzindo', async () => {
  const figure = await open(cycle);
  intersect(true);
  expect(within(figure).getByRole('button', { name: 'Pausar' })).not.toHaveAttribute('aria-pressed');
  await act(async () => { clock.time = 6.1; await new Promise(resolve => requestAnimationFrame(resolve)); });
  expect(clock).toEqual({ time: 0, paused: false });
  // Played to the end and paused there: play again restarts from the beginning.
  fireEvent.click(within(figure).getByRole('button', { name: 'Pausar' }));
  clock.time = 6;
  fireEvent.click(within(figure).getByRole('button', { name: 'Reproduzir' }));
  expect(clock).toEqual({ time: 0, paused: false });
});

it('desmontar cancela o quadro pendente e desliga o observador', async () => {
  const cancel = vi.spyOn(window, 'cancelAnimationFrame');
  const figure = await open(cycle);
  intersect(true);
  expect(figure).toBeInTheDocument();
  cleanup();
  expect(cancel).toHaveBeenCalled();
  expect(disconnect).toHaveBeenCalled();
  expect(mediaListeners).toEqual([]);
  cancel.mockRestore();
});

it('pedir movimento reduzido durante a reprodução automática para o ciclo', async () => {
  const figure = await open(cycle);
  intersect(true);
  expect(clock.paused).toBe(false);
  setReduced(true);
  expect(clock.paused).toBe(true);
  expect(within(figure).getByRole('button', { name: 'Reproduzir' })).toBeInTheDocument();
  setReduced(false);
  expect(clock.paused).toBe(false);
});

it('com movimento reduzido pausar e reproduzir funcionam repetidas vezes', async () => {
  reduced = true;
  const figure = await open(cycle);
  intersect(true);
  for (let round = 0; round < 3; round++) {
    fireEvent.click(within(figure).getByRole('button', { name: 'Reproduzir' }));
    expect(clock.paused).toBe(false);
    fireEvent.click(within(figure).getByRole('button', { name: 'Pausar' }));
    expect(clock.paused).toBe(true);
  }
});

it('sem IntersectionObserver o ciclo não começa sozinho e reproduz a pedido repetidas vezes', async () => {
  vi.stubGlobal('IntersectionObserver', undefined);
  const figure = await open(cycle);
  expect(clock).toEqual({ time: 4, paused: true });
  for (let round = 0; round < 2; round++) {
    fireEvent.click(within(figure).getByRole('button', { name: 'Reproduzir' }));
    expect(clock.paused).toBe(false);
    fireEvent.click(within(figure).getByRole('button', { name: 'Pausar' }));
    expect(clock.paused).toBe(true);
  }
});

it('reproduzir antes de os efeitos de montagem rodarem não é desfeito', async () => {
  // A lesson that arrives by fetch commits outside act; its mount effects run in a later
  // task, so a click can come first (intermittent in CI). Reproduce that order exactly.
  reduced = true;
  const container = document.body.appendChild(document.createElement('div'));
  const environment = globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean };
  const committed = new Promise<void>(resolve => new MutationObserver((_, observer) => { observer.disconnect(); resolve(); }).observe(container, { childList: true, subtree: true }));
  const root = createRoot(container);
  environment.IS_REACT_ACT_ENVIRONMENT = false;
  try {
    root.render(<LessonAnimation node={cycle} svg={(ref, labelledBy) => <svg ref={ref} role="img" aria-labelledby={labelledBy} />} />);
    await committed;
  } finally { environment.IS_REACT_ACT_ENVIRONMENT = true; }
  expect(observers).toEqual([]);
  expect(clock).toEqual({ time: 4, paused: true });
  fireEvent.click(within(container).getByRole('button', { name: 'Reproduzir' }));
  expect(observers).toHaveLength(1);
  expect(clock).toEqual({ time: 0, paused: false });
  expect(within(container).getByRole('button', { name: 'Pausar' })).toBeInTheDocument();
  intersect(false);
  expect(clock.paused).toBe(false);
  intersect(true);
  intersect(false);
  expect(clock.paused).toBe(true);
  act(() => root.unmount());
  container.remove();
});
