import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import App from './App';

function api(offline = false) {
  const fetchMock = vi.fn(async (input: string, options?: RequestInit) => {
    if (offline) throw new TypeError('offline');
    const url = new URL(input);
    const body = url.pathname.endsWith('/auth/config') ? { googleEnabled: false }
      : url.pathname.endsWith('/me') ? null
      : url.pathname.endsWith('/availability') ? { topicId: url.searchParams.get('topicId'), available: 0, maximum: 5 }
      : url.pathname.endsWith('/status') ? { status: 'UP' } : {};
    return { ok: body !== null, status: body === null ? 401 : 200, json: async () => body };
  });
  vi.stubGlobal('fetch', fetchMock);return fetchMock;
}
afterEach(() => vi.unstubAllGlobals());

it('novo visitante escolhe cargo e encontra aulas sem API nem armazenamento', async () => {
  const fetchMock = api(true);render(<App />);
  expect(screen.getByText(/Não é um site oficial/)).toBeVisible();
  expect(screen.getByRole('button', { name: 'Encontrar aulas' })).toBeDisabled();
  fireEvent.change(screen.getByLabelText('Para qual cargo você vai estudar?'), { target: { value: 'secretario-de-escola' } });
  fireEvent.click(screen.getByRole('button', { name: 'Encontrar aulas' }));
  expect(await screen.findByText('43 aulas encontradas.')).toBeVisible();
  expect(screen.queryByRole('button', { name: /A01.*Princípios/ })).not.toBeInTheDocument();
  expect(screen.getByLabelText('Cargo para encontrar aulas')).toHaveValue('secretario-de-escola');
  expect(window.location.hash).toBe('#/aulas?cargo=secretario-de-escola');
  expect(fetchMock.mock.calls.some(([, options]) => options?.method === 'POST')).toBe(false);
  expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
});
it('compartilha/restaura aula e filtros, mantendo acervo vazio distinto de falha', async () => {
  window.history.replaceState(null, '', '#/aulas/P01?cargo=secretario-de-escola&disciplina=portugues&busca=interpretacao');
  const fetchMock = api();const first = render(<App />);
  fireEvent.click(screen.getByText('Pratique esta aula'));
  await screen.findByText(/ainda não tem questões próprias/);
  expect(screen.getByRole('heading', { name: 'Interpretação de Textos' })).toBeVisible();
  expect(screen.queryByLabelText('Cargo para praticar esta aula')).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Começar prática' })).toBeDisabled();
  first.unmount();render(<App />);fireEvent.click(screen.getByText('Pratique esta aula'));
  await screen.findByText(/ainda não tem questões próprias/);
  fireEvent.click(screen.getByRole('button', { name: /Voltar às aulas/ }));
  expect(screen.getByLabelText('Buscar aula')).toHaveValue('interpretacao');
  expect(screen.getByLabelText('Filtrar disciplina')).toHaveValue('portugues');
  expect(screen.getByText('1 aula encontrada.')).toBeVisible();
  expect(fetchMock.mock.calls.some(([, options]) => options?.method === 'POST')).toBe(false);
});
it('acompanha restauração do histórico sem tratar URLs como comandos de criação', async () => {
  const fetchMock = api();render(<App />);
  await screen.findByText('Você está estudando como visitante.');
  act(() => { window.history.replaceState(null, '', '#/aulas/P02?cargo=assistente-administrativo');window.dispatchEvent(new PopStateEvent('popstate')); });
  expect(await screen.findByRole('heading', { name: 'Sinônimos, Antônimos e Relações de Sentido' })).toBeVisible();
  act(() => { window.history.replaceState(null, '', '#/praticas');window.dispatchEvent(new HashChangeEvent('hashchange')); });
  expect(await screen.findByRole('heading', { name: 'Qual assunto vamos praticar?' })).toBeVisible();
  expect(fetchMock.mock.calls.some(([, options]) => options?.method === 'POST')).toBe(false);
});
it('cargo incompatível e parâmetro privado são descartados com orientação', async () => {
  window.history.replaceState(null, '', '#/aulas/S01?cargo=assistente-administrativo&token=privado');
  api();render(<App />);
  expect(screen.getByText(/O endereço tinha opções desconhecidas/)).toBeVisible();
  await waitFor(() => expect(window.location.hash).toBe('#/aulas?cargo=assistente-administrativo'));
  expect(screen.getByLabelText('Cargo para encontrar aulas')).toHaveValue('assistente-administrativo');
});
it('explorar todas as aulas limpa uma busca anterior sem criar atividades', async () => {
  window.history.replaceState(null, '', '#/inicio?cargo=assistente-administrativo&busca=interpretacao&aula=P01');
  const fetchMock = api();render(<App />);
  fireEvent.click(screen.getByRole('button', { name: 'Explorar todas as aulas' }));
  expect(screen.getByText('51 aulas encontradas.')).toBeVisible();
  expect(screen.getByLabelText('Buscar aula')).toHaveValue('');
  expect(window.location.hash).toBe('#/aulas');
  expect(fetchMock.mock.calls.some(([, options]) => options?.method === 'POST')).toBe(false);
});
