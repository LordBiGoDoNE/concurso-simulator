import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import NativeLesson from './NativeLesson';
import { parseNativeLesson, type NativeLessonDocument, type LessonNode } from './native-lesson';
import catalog from './lesson-catalog.json';
import manifest from './lesson-documents/manifest.json';

const documents = import.meta.glob<NativeLessonDocument>('./lesson-documents/[A-Z][0-9][0-9].json', { eager: true, import: 'default' });
const documentFor = (id: string) => documents['./lesson-documents/' + id + '.json'];
const text = (node: LessonNode): string => typeof node === 'string' ? node : 'children' in node ? node.children.map(text).join('') : '';
function nodes(children: LessonNode[]): Exclude<LessonNode, string>[] {
  return children.flatMap(node => typeof node === 'string' ? [] : [node, ...('children' in node ? nodes(node.children) : [])]);
}
afterEach(() => vi.unstubAllGlobals());

it('cobre as 51 aulas com dados inertes e fontes reproduzíveis, sem exercícios ou navegação legada', () => {
  expect(Object.keys(documents)).toHaveLength(51);
  expect(Object.keys(manifest.documents)).toEqual(catalog.topics.map(topic => topic.id + '.json'));
  for (const topic of catalog.topics) {
    const document = documentFor(topic.id);
    expect(parseNativeLesson(document, topic.id)).toEqual(document);
    expect(document.title).toBe(topic.title);
    const elements = nodes(document.children);
    expect(elements.some(node => node.tag === 'h2')).toBe(true);
    expect(elements.some(node => 'attrs' in node && node.attrs?.href?.startsWith('https:'))).toBe(true);
    expect(elements.some(node => ['h1', 'iframe', 'script', 'style', 'img', 'nav', 'header', 'footer'].includes(node.tag))).toBe(false);
    expect(elements.some(node => 'attrs' in node && /(?:-q\d+|-exercicios)$/.test(node.attrs?.id ?? ''))).toBe(false);
    expect(document.children.map(text).join(' ')).not.toMatch(/EDIÇÃO V3|Índice V3|Gabarito e revisão|ESQUEMA AUTORAL V3|Sua vez: tente antes de abrir/i);
  }
  expect(nodes(documentFor('M01').children).some(node => node.tag === 'math')).toBe(true);
  expect(nodes(documentFor('I05').children).some(node => node.tag === 'table')).toBe(true);
  expect(nodes(documentFor('M04').children).find(node => node.tag === 'percentage-grid')).toMatchObject({ value: 15 });
});

it.each([
  { tag: 'iframe', children: [] },
  { tag: 'div', attrs: { onload: 'alert(1)' }, children: [] },
  { tag: 'a', attrs: { href: 'javascript:alert(1)' }, children: [] },
  { tag: 'a', attrs: { href: 'https://user:password@example.test' }, children: [] },
  { tag: 'a', attrs: { href: '#lesson-P01-missing' }, children: [] },
  { tag: 'path', attrs: { fill: 'url(https://example.test/active.svg)' }, children: [] },
])('recusa marcação ativa, atributos e referências fora do contrato: %j', child => {
  expect(() => parseNativeLesson({ format: 'native-lesson/v1', topicId: 'P01', title: 'Aula', children: [child] }, 'P01')).toThrow();
});

it('renderiza teoria, exemplo, esquema e fontes na mesma página; índice local mantém a rota', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, json: async () => documentFor('P01') })));
  render(<NativeLesson topicId="P01" />);
  const article = await screen.findByRole('article', { name: 'Conteúdo da aula: Interpretação de Textos' });
  expect(within(article).getByRole('heading', { name: 'Exemplos resolvidos passo a passo' })).toBeVisible();
  expect(within(article).getByRole('img')).toBeInTheDocument();
  expect(within(article).getByRole('heading', { name: 'Referências e conteúdos auxiliares' })).toBeInTheDocument();
  const before = window.location.hash;
  Element.prototype.scrollIntoView = vi.fn();
  fireEvent.click(within(article).getByRole('button', { name: '1. Compreender não é adivinhar' }));
  expect(window.location.hash).toBe(before);
  expect(document.getElementById('lesson-P01-s1')).toHaveFocus();
  expect(article.querySelector('h1, iframe')).toBeNull();
  expect(within(article).getAllByRole('link').every(link => link.getAttribute('rel') === 'noopener noreferrer')).toBe(true);
  expect(fetch).toHaveBeenCalledWith(expect.stringContaining('P01'), expect.objectContaining({ credentials: 'omit' }));
});

it('falha explícita permite repetir a leitura sem criar prática', async () => {
  const fetchMock = vi.fn().mockRejectedValueOnce(new TypeError('offline')).mockResolvedValue({ ok: true, json: async () => documentFor('M04') });
  vi.stubGlobal('fetch', fetchMock);render(<NativeLesson topicId="M04" />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar esta aula.');
  fireEvent.click(screen.getByRole('button', { name: 'Tentar carregar aula novamente' }));
  const article = await screen.findByRole('article');
  expect(article.querySelectorAll('math').length).toBe(5);
  expect(article.querySelectorAll('.native-percentage-grid > span')).toHaveLength(100);
  expect(article.querySelectorAll('.native-percentage-grid > .filled')).toHaveLength(15);
  expect(fetchMock).toHaveBeenCalledTimes(2);
  expect(fetchMock.mock.calls.every(([, options]) => !options?.method)).toBe(true);
});

it('trocar aula cancela a leitura anterior e ignora resposta tardia', async () => {
  let finish!: (value: unknown) => void; let signal!: AbortSignal;
  vi.stubGlobal('fetch', vi.fn().mockImplementationOnce((_url, options) => { signal = options.signal; return new Promise(resolve => { finish = resolve; }); }).mockResolvedValue({ ok: true, json: async () => documentFor('S02') }));
  const ui = render(<NativeLesson topicId="P01" />);ui.rerender(<NativeLesson topicId="S02" />);
  await screen.findByRole('article', { name: 'Conteúdo da aula: Escrituração Escolar' });expect(signal.aborted).toBe(true);
  await act(async () => finish({ ok: true, json: async () => documentFor('P01') }));
  await waitFor(() => expect(screen.getByRole('article')).toHaveAccessibleName('Conteúdo da aula: Escrituração Escolar'));
});
