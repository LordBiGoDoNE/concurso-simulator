import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import App from './App';
import { individualReview, reviewOverview } from './subject-review-api';
import { overview, individual } from '../test/subject-review-fixtures';
import { view, result } from '../test/attempt-fixtures';
import { catalog, emptyPage, response } from '../test/question-fixtures';
vi.mock('./subject-review-api', async original => ({ ...await original<typeof import('./subject-review-api')>(), reviewOverview: vi.fn(), individualReview: vi.fn() }));
const userA = '90000000-0000-0000-0000-000000000001', userB = '90000000-0000-0000-0000-000000000002';
afterEach(() => { vi.restoreAllMocks();vi.unstubAllGlobals();vi.resetAllMocks(); });
function api(initial: string | null) {
  let user = initial;
  vi.stubGlobal('fetch', vi.fn(async (url: string, options?: RequestInit) => {
    if (url.endsWith('/status')) return response({ status: 'UP' });if (url.endsWith('/auth/config')) return response({ googleEnabled: true });
    if (url.endsWith('/me')) return user ? response({ id: user }) : response(null, 401);
    if (url.endsWith('/csrf')) return response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/auth/logout')) { user = null;return response(null, 204); }
    if (url.endsWith('/catalog')) return response(catalog);if (url.includes('/question-bank/questions')) return response(emptyPage);
    if (url.includes('/me/attempts')) return response({ items: [], page: 0, size: 20 });
    if (url.endsWith('/attempts') && options?.method === 'POST') return response(view, 201);
    if (url.endsWith('/finish')) return response({ attempt: { ...view, version: 1, finishedAt: '2026-10-16T12:00:00Z' }, result: { ...result, items: result.items.map(i => ({ item: i.item, correctAlternativeId: i.item.alternatives[0].id, resolution: i.explanation })) } });
    throw new Error('unexpected request');
  }));return { change: (next: string | null) => { user = next; } };
}
it('logout desmonta painel imediatamente e resposta tardia não reaparece nem vai para storage', async () => {
  api(userA);let finish!: (v: ReturnType<typeof overview>) => void;let signal!: AbortSignal;
  vi.mocked(reviewOverview).mockImplementation((_r, s) => { signal = s;return new Promise(resolve => { finish = resolve; }); });render(<App />);
  fireEvent.change(await screen.findByLabelText('Cargo para revisão'), { target: { value: view.roleId } });await screen.findByText('Consultando revisão por assunto…');
  fireEvent.click(screen.getByRole('button', { name: 'Sair' }));expect(screen.queryByRole('heading', { name: 'Revisão por assunto' })).not.toBeInTheDocument();expect(signal.aborted).toBe(true);
  await act(async () => finish(overview()));await screen.findByText('Você está estudando como visitante.');expect(screen.queryByRole('table')).not.toBeInTheDocument();expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
});
it('troca de conta limpa cargo/métricas e cancela consulta anterior sem adotar visitante', async () => {
  const fixture = api(userA);let finish!: (v: ReturnType<typeof overview>) => void;let signal!: AbortSignal;
  vi.mocked(reviewOverview).mockImplementationOnce((_r, s) => { signal = s;return new Promise(resolve => { finish = resolve; }); }).mockResolvedValueOnce(overview(true));render(<App />);
  fireEvent.change(await screen.findByLabelText('Cargo para revisão'), { target: { value: view.roleId } });fixture.change(userB);fireEvent.click(screen.getByRole('button', { name: 'Atualizar sessão' }));
  await waitFor(() => expect(screen.getByLabelText('Cargo para revisão')).toHaveValue(''));expect(signal.aborted).toBe(true);await act(async () => finish(overview()));expect(screen.queryByRole('table')).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText('Cargo para revisão'), { target: { value: view.roleId } });await screen.findByText('Sem dados para este cargo. Nenhuma dificuldade foi inferida.');
});
it.each([null, userA])('conclusão confirmada atualiza painel somente da conta; individual não pede histórico implícito (%s)', async user => {
  api(user);vi.mocked(reviewOverview).mockResolvedValue(overview(true));vi.mocked(individualReview).mockResolvedValue(individual());render(<App />);
  if (user) { fireEvent.change(await screen.findByLabelText('Cargo para revisão'), { target: { value: view.roleId } });await screen.findByText('Sem dados para este cargo. Nenhuma dificuldade foi inferida.'); }
  fireEvent.click(await screen.findByRole('button', { name: 'Iniciar simulado' }));await screen.findByRole('heading', { name: 'Questão 1 de 1' });
  fireEvent.click(screen.getByRole('button', { name: 'Concluir simulado' }));fireEvent.click(screen.getByRole('button', { name: 'Confirmar conclusão' }));await screen.findByRole('heading', { name: 'Resultado do simulado' });
  await waitFor(() => expect(individualReview).toHaveBeenCalledWith(view.id, view.roleId, expect.any(AbortSignal)));
  if (user) await waitFor(() => expect(vi.mocked(reviewOverview).mock.calls.length).toBeGreaterThan(1));else expect(reviewOverview).not.toHaveBeenCalled();
});

it('consulta explícita falha e conclusão posterior não move foco do resultado na atualização automática', async () => {
  api(userA);vi.mocked(reviewOverview).mockRejectedValueOnce(new Error('offline')).mockResolvedValue(overview());vi.mocked(individualReview).mockResolvedValue(individual());render(<App />);
  fireEvent.click(await screen.findByRole('button', { name: 'Iniciar simulado' }));await screen.findByRole('heading', { name: 'Questão 1 de 1' });
  fireEvent.change(screen.getByLabelText('Cargo para revisão'), { target: { value: view.roleId } });await screen.findByRole('alert');
  fireEvent.click(screen.getByRole('button', { name: 'Concluir simulado' }));fireEvent.click(screen.getByRole('button', { name: 'Confirmar conclusão' }));
  await screen.findByText('Assunto P1: 3 erros ou brancos em 3 bases.', { selector: '#subject-review-title ~ ol li' });
  expect(screen.getByRole('heading', { name: 'Resultado do simulado' })).toHaveFocus();
});
