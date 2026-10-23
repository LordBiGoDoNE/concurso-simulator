import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import App from './App';
import { view } from '../test/attempt-fixtures';
import { catalog, emptyPage, response } from '../test/question-fixtures';
const userA = '90000000-0000-0000-0000-000000000001'; const userB = '90000000-0000-0000-0000-000000000002';
const privateView = { ...view, items: [{ ...view.items[0], statement: { text: 'PRIVATE_CONTEXT_STATEMENT', resources: [] } }] };
afterEach(() => { vi.restoreAllMocks();vi.unstubAllGlobals(); });
function api(initial: string | null, denied?: { status: number; stalled: boolean }, history = false) {
  let user = initial; let expired = false; let resolveWrite: ((body: unknown) => void) | undefined;
  const fetchMock = vi.fn(async (url: string, options?: RequestInit) => {
    if (url.endsWith('/status')) return response({ status: 'UP' });
    if (url.endsWith('/auth/config')) return response({ googleEnabled: true });
    if (url.endsWith('/me')) return user ? response({ id: user }) : response({ error: 'unauthenticated' }, 401);
    if (url.endsWith('/csrf')) return response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/auth/logout')) { user = null;return response(null, 204); }
    if (url.endsWith('/question-bank/catalog')) return response(catalog);
    if (url.includes('/question-bank/questions')) return response(emptyPage);
    if (url.includes('/me/attempts')) return response({ page: 0, size: 20, items: history ? [{ id: view.id, roleId: view.roleId, createdAt: view.createdAt, finishedAt: null, total: 1, percentage: null }] : [] });
    if (url.endsWith('/attempts') && options?.method === 'POST') return response(privateView, 201);
    if (url.endsWith(`/attempts/${view.id}`)) return expired ? response({ error: 'not_found' }, 404) : response(privateView);
    if (url.includes('/answers/') && expired) return denied ? { ok: false, status: denied.status, json: () => denied.stalled ? new Promise(() => {}) : Promise.reject(new SyntaxError('truncated')) } : response({ error: 'unauthenticated' }, 401);
    if (url.includes('/answers/')) return { ok: true, status: 200, json: () => new Promise(resolve => { resolveWrite = resolve; }) };
    throw new Error('unexpected request');
  });vi.stubGlobal('fetch', fetchMock);
  return { change: (id: string | null) => { user = id; }, expire: () => { user = null; expired = true; }, finishWrite: () => resolveWrite?.({ ...privateView, version: 1, items: [{ ...privateView.items[0], selectedAlternativeId: privateView.items[0].alternatives[0].id }] }), fetchMock };
}
async function start() { fireEvent.click(screen.getByRole('button', { name: 'Simulados' })); const button = await screen.findByRole('button', { name: 'Iniciar simulado' });fireEvent.click(button);await screen.findByText('PRIVATE_CONTEXT_STATEMENT'); }
it('logout limpa tentativa imediatamente e resposta tardia não reaparece nem entra em storage', async () => {
  const fixture = api(userA);render(<App />);await start();fireEvent.click(screen.getAllByRole('radio')[0]);
  await waitFor(() => expect(fixture.fetchMock.mock.calls.some(([u]) => u.includes('/answers/'))).toBe(true));
  fireEvent.click(screen.getByRole('button', { name: 'Sair' }));expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument();
  await screen.findByText('Você está estudando como visitante.');await act(async () => fixture.finishWrite());
  expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument();expect(screen.queryByRole('heading', { name: 'Histórico de simulados' })).not.toBeInTheDocument();
  expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
});
it.each([{ status: 401, stalled: false }, { status: 404, stalled: true }])('limpa conteúdo privado mesmo com corpo de rejeição $status incompleto', async denied => {
  const fixture = api(userA, denied);render(<App />);await start();fixture.expire();fireEvent.click(screen.getAllByRole('radio')[0]);
  await screen.findByText('Você está estudando como visitante.');expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument();
  expect(screen.getByText(/O conteúdo privado foi limpo/)).toBeInTheDocument();expect(screen.queryByText('Respostas confirmadas.')).not.toBeInTheDocument();
});
it('abrir a tentativa atual preserva escolha e comando após falha de salvamento', async () => {
  const fixture = api(userA, undefined, true);render(<App />);await start();
  fixture.fetchMock.mockImplementationOnce(async () => response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' })).mockImplementationOnce(async () => { throw new TypeError('network'); });
  fireEvent.click(screen.getAllByRole('radio')[0]);await screen.findByRole('button', { name: 'Repetir salvamento' });
  const calls = fixture.fetchMock.mock.calls.length;fireEvent.click(screen.getByRole('button', { name: /Abrir simulado de/ }));
  expect(fixture.fetchMock).toHaveBeenCalledTimes(calls);expect(screen.getAllByRole('radio')[0]).toBeChecked();expect(screen.getByRole('button', { name: 'Repetir salvamento' })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Repetir salvamento' }));
  await waitFor(() => expect(fixture.fetchMock.mock.calls.filter(([url]) => url.includes('/answers/'))).toHaveLength(2));
  const writes = fixture.fetchMock.mock.calls.filter(([url]) => url.includes('/answers/'));expect(writes[0][1]?.body).toEqual(writes[1][1]?.body);
  await act(async () => fixture.finishWrite());await screen.findByText('Respostas confirmadas.');expect(screen.getAllByRole('radio')[0]).toBeChecked();
});
it('trocar para outra conta no refresh desmonta o contexto anterior e ignora escrita tardia', async () => {
  const fixture = api(userA);render(<App />);fireEvent.click(screen.getByRole('button', { name: 'Simulados' }));
  fireEvent.click(await screen.findByRole('checkbox', { name: 'Usar variações numéricas quando disponíveis' }));await start();fireEvent.click(screen.getAllByRole('radio')[0]);
  await waitFor(() => expect(fixture.fetchMock.mock.calls.some(([u]) => u.includes('/answers/'))).toBe(true));fixture.change(userB);
  fireEvent.click(screen.getByRole('button', { name: 'Atualizar sessão' }));await screen.findByRole('button', { name: 'Iniciar simulado' });
  expect(screen.getByRole('checkbox', { name: 'Usar variações numéricas quando disponíveis' })).not.toBeChecked();
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
it('retorno de página restaurada pelo navegador revalida a conta antes de exibir o contexto privado', async () => {
  const fixture = api(userA);render(<App />);await start();fixture.change(userB);
  const restored = new Event('pageshow');Object.defineProperty(restored, 'persisted', { value: true });fireEvent(window, restored);
  await screen.findByRole('button', { name: 'Iniciar simulado' });expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument();
});

it('navegação preserva escolha pendente e retry idempotente sem criar outra tentativa', async () => {
  const fixture = api(userA);render(<App />);await start();
  fixture.fetchMock.mockImplementationOnce(async () => response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' })).mockImplementationOnce(async () => { throw new TypeError('network'); });
  fireEvent.click(screen.getAllByRole('radio')[0]);await screen.findByRole('button', { name: 'Repetir salvamento' });
  fireEvent.click(screen.getByRole('button', { name: 'Início' }));
  expect(screen.queryByRole('radio')).not.toBeInTheDocument();expect(screen.getByRole('heading', { name: /Mais clareza/ })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Simulados' }));
  expect(screen.getAllByRole('radio')[0]).toBeChecked();fireEvent.click(screen.getByRole('button', { name: 'Repetir salvamento' }));
  await waitFor(() => expect(fixture.fetchMock.mock.calls.filter(([url]) => url.includes('/answers/'))).toHaveLength(2));
  const writes = fixture.fetchMock.mock.calls.filter(([url]) => url.includes('/answers/'));expect(writes[0][1]?.body).toEqual(writes[1][1]?.body);
  await act(async () => fixture.finishWrite());expect(await screen.findByText('Respostas confirmadas.')).toBeInTheDocument();
  expect(fixture.fetchMock.mock.calls.filter(([url, options]) => url.endsWith('/attempts') && options?.method === 'POST')).toHaveLength(1);
});
it('troca de conta durante outra área descarta a tentativa oculta e ignora resposta tardia', async () => {
  const fixture = api(userA);render(<App />);await start();fireEvent.click(screen.getAllByRole('radio')[0]);
  await waitFor(() => expect(fixture.fetchMock.mock.calls.some(([url]) => url.includes('/answers/'))).toBe(true));
  fireEvent.click(screen.getByRole('button', { name: 'Aulas' }));fixture.change(userB);
  fireEvent.click(screen.getByRole('button', { name: 'Atualizar sessão' }));await screen.findByText('Você está conectado.');
  await act(async () => fixture.finishWrite());fireEvent.click(screen.getByRole('button', { name: 'Simulados' }));
  expect(await screen.findByRole('button', { name: 'Iniciar simulado' })).toBeInTheDocument();expect(screen.queryByText('PRIVATE_CONTEXT_STATEMENT')).not.toBeInTheDocument();
});
