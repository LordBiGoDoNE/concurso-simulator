import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import App from './App';
import { view } from '../test/attempt-fixtures';
import { catalog, emptyPage, response } from '../test/question-fixtures';
const userA = '90000000-0000-0000-0000-000000000001'; const userB = '90000000-0000-0000-0000-000000000002';
const privateView = { ...view, items: [{ ...view.items[0], statement: { text: 'PRIVATE_CONTEXT_STATEMENT', resources: [] } }] };
afterEach(() => { vi.restoreAllMocks();vi.unstubAllGlobals(); });
function api(initial: string | null) {
  let user = initial; let expired = false; let resolveWrite: ((body: unknown) => void) | undefined;
  const fetchMock = vi.fn(async (url: string, options?: RequestInit) => {
    if (url.endsWith('/status')) return response({ status: 'UP' });
    if (url.endsWith('/auth/config')) return response({ googleEnabled: true });
    if (url.endsWith('/me')) return user ? response({ id: user }) : response({ error: 'unauthenticated' }, 401);
    if (url.endsWith('/csrf')) return response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/auth/logout')) { user = null;return response(null, 204); }
    if (url.endsWith('/question-bank/catalog')) return response(catalog);
    if (url.includes('/question-bank/questions')) return response(emptyPage);
    if (url.includes('/me/attempts')) return response({ page: 0, size: 20, items: [] });
    if (url.endsWith('/attempts') && options?.method === 'POST') return response(privateView, 201);
    if (url.endsWith(`/attempts/${view.id}`)) return expired ? response({ error: 'not_found' }, 404) : response(privateView);
    if (url.includes('/answers/') && expired) return response({ error: 'unauthenticated' }, 401);
    if (url.includes('/answers/')) return { ok: true, status: 200, json: () => new Promise(resolve => { resolveWrite = resolve; }) };
    throw new Error('unexpected request');
  });vi.stubGlobal('fetch', fetchMock);
  return { change: (id: string | null) => { user = id; }, expire: () => { user = null; expired = true; }, finishWrite: () => resolveWrite?.({ ...privateView, version: 1, items: [{ ...privateView.items[0], selectedAlternativeId: privateView.items[0].alternatives[0].id }] }), fetchMock };
}
async function start() { const button = await screen.findByRole('button', { name: 'Iniciar simulado' });fireEvent.click(button);await screen.findByText('PRIVATE_CONTEXT_STATEMENT'); }
it('logout limpa tentativa imediatamente e resposta tardia não reaparece nem entra em storage', async () => {
  const fixture = api(userA);render(<App />);await start();fireEvent.click(screen.getAllByRole('radio')[0]);
  await waitFor(() => expect(fixture.fetchMock.mock.calls.some(([u]) => u.includes('/answers/'))).toBe(true));
  fireEvent.click(screen.getByRole('button', { name: 'Sair' }));expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument();
  await screen.findByText('Você está estudando como visitante.');await act(async () => fixture.finishWrite());
  expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument();expect(screen.queryByRole('heading', { name: 'Histórico de simulados' })).not.toBeInTheDocument();
  expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
});
it('trocar para outra conta no refresh desmonta o contexto anterior e ignora escrita tardia', async () => {
  const fixture = api(userA);render(<App />);await start();fireEvent.click(screen.getAllByRole('radio')[0]);
  await waitFor(() => expect(fixture.fetchMock.mock.calls.some(([u]) => u.includes('/answers/'))).toBe(true));fixture.change(userB);
  fireEvent.click(screen.getByRole('button', { name: 'Atualizar sessão' }));await screen.findByRole('button', { name: 'Iniciar simulado' });
  await act(async () => fixture.finishWrite());expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument();
});
it('visitante vê duração/ausência de transferência e cancelar login preserva a realização', async () => {
  api(null);vi.spyOn(window, 'confirm').mockReturnValue(false);render(<App />);await start();
  expect(screen.getByText(/Modo visitante: tentativa temporária por até 24 horas/)).toBeInTheDocument();
  const event = new MouseEvent('click', { bubbles: true, cancelable: true });screen.getByRole('link', { name: 'Entrar com Google' }).dispatchEvent(event);
  expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining('não será transferida'));expect(event.defaultPrevented).toBe(true);
  expect(screen.getByText('PRIVATE_CONTEXT_STATEMENT')).toBeInTheDocument();
});
it('401 durante resposta limpa conteúdo e revalida identidade sem anunciar salvamento', async () => {
  const fixture = api(userA);render(<App />);await start();fixture.expire();fireEvent.click(screen.getAllByRole('radio')[0]);
  await screen.findByText('Você está estudando como visitante.');expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument();
  expect(screen.getByText(/O conteúdo privado foi limpo/)).toBeInTheDocument();
});
it('revalidação do visitante também confere acesso à tentativa quando a sessão mudou', async () => {
  const fixture = api(null);render(<App />);await start();fixture.expire();fireEvent(window, new Event('focus'));
  await waitFor(() => expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument());
  await screen.findByRole('button', { name: 'Iniciar simulado' });
  expect(fixture.fetchMock.mock.calls.some(([url]) => url.endsWith(`/attempts/${view.id}`))).toBe(true);
});
