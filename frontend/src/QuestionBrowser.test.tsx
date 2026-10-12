import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import QuestionBrowser from './QuestionBrowser';
import App from './App';
import { catalog, detail, page, questionId, response } from '../test/question-fixtures';
afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });
function api(list = page) {
  const fetch = vi.fn(async (url: string) => response(url.endsWith('/catalog') ? catalog : url.endsWith(questionId) ? detail : list));
  vi.stubGlobal('fetch', fetch);return fetch;
}
it('visitante consulta catálogo/lista/detalhe com fontes/fórmula e foco, sem responder', async () => {
  api();render(<QuestionBrowser />);
  fireEvent.click(await screen.findByRole('button', { name: /Consultar:/ }));
  expect(await screen.findByText('Fixture: πr² e 2 < 3')).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Detalhe da questão' })).toHaveFocus();
  expect(screen.getByRole('link', { name: 'Fonte sintética nomeada' })).toHaveAttribute('href', 'https://example.test/study');
  expect(screen.queryByRole('radio')).not.toBeInTheDocument();expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Fechar detalhe' }));
  expect(screen.getByRole('button', { name: /Consultar:/ })).toHaveFocus();
});
it('mudar disciplina limpa assunto/página/detalhe e evita combinação incompatível', async () => {
  const fetch = api();render(<QuestionBrowser />);await screen.findByRole('button', { name: /Consultar:/ });
  fireEvent.change(screen.getByLabelText('Assunto'), { target: { value: 'P01' } });
  await waitFor(() => expect(fetch.mock.calls.at(-1)?.[0]).toContain('topicId=P01'));
  fireEvent.change(screen.getByLabelText('Disciplina'), { target: { value: 'matematica-raciocinio' } });
  await waitFor(() => expect(fetch.mock.calls.at(-1)?.[0]).toContain('disciplineId=matematica-raciocinio'));
  expect(screen.getByLabelText('Assunto')).toHaveValue('');expect(fetch.mock.calls.at(-1)?.[0]).not.toContain('topicId=');
});
it('vazio e erro são distintos e retry recupera sem perder filtros', async () => {
  api({ ...page, items: [], total: 0 });render(<QuestionBrowser />);
  expect(await screen.findByText(/Nenhuma questão elegível/)).toBeInTheDocument();
  vi.stubGlobal('fetch', vi.fn(async () => response(null, 503)));
  fireEvent.change(screen.getByLabelText('Cargo'), { target: { value: 'secretario-de-escola' } });
  expect(await screen.findByRole('alert')).toHaveTextContent(/Não é um acervo vazio/);
  api();fireEvent.click(screen.getByRole('button', { name: 'Tentar questões novamente' }));
  expect(await screen.findByRole('button', { name: /Consultar:/ })).toBeInTheDocument();expect(screen.getByLabelText('Cargo')).toHaveValue('secretario-de-escola');
});
it('resposta antiga ignorando abort não substitui filtro novo', async () => {
  let old!: (value: unknown) => void;let oldSignal!: AbortSignal;let calls = 0;
  vi.stubGlobal('fetch', vi.fn((url: string, options: RequestInit) => {
    if (url.endsWith('/catalog')) return Promise.resolve(response(catalog));
    if (++calls === 1) { oldSignal = options.signal!;return new Promise(resolve => { old = resolve; }); }
    return Promise.resolve(response({ ...page, items: [], total: 0 }));
  }));
  render(<QuestionBrowser />);await screen.findByLabelText('Cargo');await waitFor(() => expect(old).toBeDefined());
  fireEvent.change(screen.getByLabelText('Cargo'), { target: { value: 'secretario-de-escola' } });
  expect(await screen.findByText(/Nenhuma questão elegível/)).toBeInTheDocument();expect(oldSignal.aborted).toBe(true);
  await act(async () => { old(response(page)); });expect(screen.queryByRole('button', { name: /Consultar:/ })).not.toBeInTheDocument();
});
it('detalhe tardio depois de fechar ou trocar filtros não reaparece', async () => {
  let finish!: (value: unknown) => void;
  vi.stubGlobal('fetch', vi.fn((url: string) => url.endsWith(questionId) ? new Promise(resolve => { finish = resolve; }) : Promise.resolve(response(url.endsWith('/catalog') ? catalog : page))));
  render(<QuestionBrowser />);fireEvent.click(await screen.findByRole('button', { name: /Consultar:/ }));
  await waitFor(() => expect(finish).toBeDefined());fireEvent.click(screen.getByRole('button', { name: 'Fechar detalhe' }));
  await act(async () => finish(response(detail)));expect(screen.queryByRole('heading', { name: 'Detalhe da questão' })).not.toBeInTheDocument();
});
it('JSON pendente termina com erro finito e retry, não fica carregando', async () => {
  vi.useFakeTimers();vi.stubGlobal('fetch', vi.fn(async (url: string) => url.endsWith('/catalog') ? response(catalog) : { ok: true, json: () => new Promise(() => {}) }));
  render(<QuestionBrowser />);await act(async () => { await vi.advanceTimersByTimeAsync(0); });
  await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
  expect(screen.getByRole('alert')).toHaveTextContent(/Consulta de questões indisponível/);expect(screen.getByRole('button', { name: 'Tentar questões novamente' })).toBeEnabled();
});
it.each([false, true])('mesmos filtros com identidade conectada=%s e aulas/login preservados', async connected => {
  const fetch = vi.fn(async (url: string) => {
    if (url.endsWith('/status')) return response({ status: 'UP' });
    if (url.endsWith('/auth/config')) return response({ googleEnabled: true });
    if (url.endsWith('/me')) return response(connected ? { id: questionId } : undefined, connected ? 200 : 401);
    return response(url.endsWith('/catalog') ? catalog : page);
  });vi.stubGlobal('fetch', fetch);render(<App />);
  expect(await screen.findByText(connected ? 'Você está conectado.' : 'Você está estudando como visitante.')).toBeInTheDocument();
  await screen.findByRole('button', { name: /Consultar:/ });
  fireEvent.change(screen.getByLabelText('Disciplina'), { target: { value: 'portugues' } });
  await waitFor(() => expect(fetch.mock.calls.some(([url]) => url.includes('disciplineId=portugues'))).toBe(true));
  expect(screen.getByRole('link', { name: 'Acessar material de estudo' })).toBeInTheDocument();
});
it('paginação mantém ordem do servidor e direciona foco ao resultado novo', async () => {
  vi.stubGlobal('fetch', vi.fn(async (url: string) => response(url.endsWith('/catalog') ? catalog : { ...page, page: url.includes('page=1') ? 1 : 0, total: 21 })));
  render(<QuestionBrowser />);await screen.findByRole('button', { name: /Consultar:/ });fireEvent.click(screen.getByRole('button', { name: 'Próxima página' }));
  expect(await screen.findByText(/Página 2/)).toBeInTheDocument();expect(screen.getByRole('heading', { name: 'Resultados da consulta' })).toHaveFocus();
  expect(screen.getByRole('button', { name: 'Página anterior' })).toBeEnabled();
});
it.each(['(x)_(1) + (y)_(2)', '_abc', '**abc', 'a_b_c', '[destaque: πr²]; 2^(3); H_(2)O; x < y e y > z'])('detalhe preserva texto/fórmula integral: %s', async text => {
  vi.stubGlobal('fetch',vi.fn(async (url: string) => response(url.endsWith('/catalog')?catalog:url.endsWith(questionId)?{...detail,statement:{text,resources:[]}}:page)));
  render(<QuestionBrowser />);fireEvent.click(await screen.findByRole('button',{name:/Consultar:/}));
  const statement=await screen.findByText(text,{selector:'article p.question-text'});
  expect(statement.textContent).toBe(text);expect(statement.querySelector('em,strong')).toBeNull();
});
