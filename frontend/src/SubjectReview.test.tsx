import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import SubjectReviewPanel from './SubjectReviewPanel';
import IndividualSubjectReview from './IndividualSubjectReview';
import { individualReview, reviewOverview, ReviewApiError } from './subject-review-api';
import { overview, individual } from '../test/subject-review-fixtures';
import { view } from '../test/attempt-fixtures';
vi.mock('./subject-review-api', async original => ({ ...await original<typeof import('./subject-review-api')>(), reviewOverview: vi.fn(), individualReview: vi.fn() }));
afterEach(() => vi.resetAllMocks());
const choose = (value = view.roleId) => fireEvent.change(screen.getByLabelText('Cargo para revisão'), { target: { value } });
it('exige cargo explícito e distingue sem dados, poucos dados, prioridade e atualização sem nota inventada', async () => {
  vi.mocked(reviewOverview).mockResolvedValueOnce(overview(true));render(<SubjectReviewPanel active refresh={0} onUnavailable={vi.fn()} />);expect(reviewOverview).not.toHaveBeenCalled();choose();
  await screen.findByText('Sem dados para este cargo. Nenhuma dificuldade foi inferida.');expect(screen.getAllByRole('row')).toHaveLength(52);expect(screen.getByText('Nenhuma prioridade nesta amostra.')).toBeInTheDocument();
  const data = overview();data.subjects[1] = { ...data.subjects[1], wrong: 1, assessed: 1, exposures: 1, percentage: 0, lastFinishedAt: data.window.latestFinishedAt };
  vi.mocked(reviewOverview).mockResolvedValueOnce(data);fireEvent.click(screen.getByRole('button', { name: 'Atualizar revisão' }));
  await screen.findByText('Assunto P1: 3 erros ou brancos em 3 bases.');expect(screen.getByText('Poucos dados')).toBeInTheDocument();expect(screen.getByRole('heading', { name: 'Revisão por assunto' })).toHaveFocus();
});
it('falha não vira vazio e retry mantém cargo; nova conclusão confirmada atualiza a janela', async () => {
  vi.mocked(reviewOverview).mockRejectedValueOnce(new Error('offline'));const props = { active: true, refresh: 0, onUnavailable: vi.fn() };const ui = render(<SubjectReviewPanel {...props} />);choose();
  await screen.findByRole('alert');expect(screen.queryByRole('table')).not.toBeInTheDocument();expect(screen.queryByText(/Sem dados para/)).not.toBeInTheDocument();
  vi.mocked(reviewOverview).mockResolvedValue(overview());fireEvent.click(screen.getByRole('button', { name: 'Atualizar revisão' }));await screen.findByRole('table');ui.rerender(<SubjectReviewPanel {...props} refresh={1} />);
  await waitFor(() => expect(reviewOverview).toHaveBeenCalledTimes(3));expect(screen.getByLabelText('Cargo para revisão')).toHaveValue(view.roleId);
});
it('troca de cargo cancela e ignora resposta antiga, inclusive servidor que ignora abort', async () => {
  let finish!: (value: ReturnType<typeof overview>) => void;let signal!: AbortSignal;
  vi.mocked(reviewOverview).mockImplementationOnce((_role, s) => { signal = s;return new Promise(resolve => { finish = resolve; }); }).mockResolvedValueOnce(overview(true, 'secretario-de-escola'));
  render(<SubjectReviewPanel active refresh={0} onUnavailable={vi.fn()} />);choose();choose('secretario-de-escola');await screen.findByText('Sem dados para este cargo. Nenhuma dificuldade foi inferida.');expect(signal.aborted).toBe(true);
  await act(async () => finish(overview()));expect(screen.queryByText('Assunto P1: 3 erros ou brancos em 3 bases.')).not.toBeInTheDocument();
});
it('verificação de sessão limpa, cancela e impede resposta tardia; reativação lê novamente', async () => {
  let finish!: (v: ReturnType<typeof overview>) => void;let signal!: AbortSignal;vi.mocked(reviewOverview).mockImplementationOnce((_r, s) => { signal = s;return new Promise(resolve => { finish = resolve; }); });
  const props = { active: true, refresh: 0, onUnavailable: vi.fn() };const ui = render(<SubjectReviewPanel {...props} />);choose();ui.rerender(<SubjectReviewPanel {...props} active={false} />);expect(signal.aborted).toBe(true);
  await act(async () => finish(overview()));expect(screen.queryByRole('table')).not.toBeInTheDocument();vi.mocked(reviewOverview).mockResolvedValue(overview(true));ui.rerender(<SubjectReviewPanel {...props} />);await screen.findByRole('table');expect(reviewOverview).toHaveBeenCalledTimes(2);
});
it.each([401, 403, 404])('rejeição %s limpa a revisão individual e notifica o contexto', async status => {
  const unavailable = vi.fn();vi.mocked(individualReview).mockRejectedValue(new ReviewApiError(status));render(<IndividualSubjectReview active attemptId={view.id} roleId={view.roleId} onUnavailable={unavailable} />);
  await waitFor(() => expect(unavailable).toHaveBeenCalledTimes(1));expect(screen.queryByRole('table')).not.toBeInTheDocument();expect(reviewOverview).not.toHaveBeenCalled();
});
it('individual consulta só o ID concluído, 409/falha distintos e retry sem agregar histórico', async () => {
  vi.mocked(individualReview).mockRejectedValueOnce(new ReviewApiError(409)).mockResolvedValueOnce(individual());render(<IndividualSubjectReview active attemptId={view.id} roleId={view.roleId} onUnavailable={vi.fn()} />);
  await screen.findByText('A conclusão ainda não foi confirmada para esta revisão.');fireEvent.click(screen.getByRole('button', { name: 'Tentar revisão deste simulado novamente' }));await screen.findByRole('table');
  expect(within(screen.getByRole('region', { name: 'Revisão por assunto deste simulado' })).getByText('Somente esta tentativa concluída; não reúne outras tentativas.')).toBeInTheDocument();expect(reviewOverview).not.toHaveBeenCalled();
});
