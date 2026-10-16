import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import AttemptWorkspace from './AttemptWorkspace';
import { questionCatalog } from './question-api';
import { createAttempt, saveAnswer, attemptHistory } from './attempt-api';
import { reviewOverview } from './subject-review-api';
import { overview } from '../test/subject-review-fixtures';
import { catalog } from '../test/question-fixtures';
import { view } from '../test/attempt-fixtures';
vi.mock('./question-api', () => ({ questionCatalog: vi.fn() }));
vi.mock('./attempt-api', async original => ({ ...await original<typeof import('./attempt-api')>(), createAttempt: vi.fn(), saveAnswer: vi.fn(), attemptHistory: vi.fn() }));
vi.mock('./subject-review-api', async original => ({ ...await original<typeof import('./subject-review-api')>(), reviewOverview: vi.fn() }));
const props = { identity: { connected: true as const, googleEnabled: true, id: '90000000-0000-0000-0000-000000000001' }, onUnavailable: vi.fn(), onGuestActive: vi.fn() };
beforeEach(() => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog);vi.mocked(reviewOverview).mockImplementation(async role => overview(true, role));
  vi.mocked(attemptHistory).mockResolvedValue({ items: [], page: 0, size: 20 });vi.mocked(createAttempt).mockResolvedValue(view);
});
afterEach(() => { vi.restoreAllMocks();vi.resetAllMocks(); });
async function panel() {
  fireEvent.change(await screen.findByLabelText('Cargo para revisão'), { target: { value: view.roleId } });
  return within(await screen.findByRole('region', { name: 'Tabela de desempenho por assunto' }));
}
it('atalho substitui critérios anteriores, zera demais disciplinas, desliga variações e não cria automaticamente', async () => {
  render(<AttemptWorkspace {...props} />);await screen.findByLabelText('Quantidade de Português');
  fireEvent.change(screen.getByLabelText('Quantidade de Português'), { target: { value: '8' } });fireEvent.click(screen.getByLabelText('Interpretação'));
  fireEvent.click(screen.getByLabelText('Usar variações numéricas quando disponíveis'));fireEvent.click((await panel()).getByRole('button', { name: 'Praticar M01' }));
  await waitFor(() => expect(screen.getByLabelText('Quantidade de Matemática')).toHaveValue(1));expect(screen.getByLabelText('Quantidade de Português')).toHaveValue(0);
  expect(screen.getByLabelText('Interpretação')).not.toBeChecked();expect(screen.getByLabelText('Operações')).toBeChecked();expect(screen.getByLabelText('Usar variações numéricas quando disponíveis')).not.toBeChecked();expect(createAttempt).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: 'Iniciar simulado' }));await screen.findByRole('radio', { name: /Opção A/ });
  expect(vi.mocked(createAttempt).mock.calls[0][0]).toEqual({ roleId: view.roleId, disciplines: [{ disciplineId: 'matematica-raciocinio', quantity: 1, topicIds: ['M01'] }], useParameterizedQuestions: false });
});
it('cancelar abandono preserva resposta local e comando; confirmar só abre composição', async () => {
  vi.mocked(saveAnswer).mockRejectedValue(new TypeError('network'));const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
  render(<AttemptWorkspace {...props} />);fireEvent.click(await screen.findByRole('button', { name: 'Iniciar simulado' }));fireEvent.click((await screen.findAllByRole('radio'))[0]);await screen.findByRole('button', { name: 'Repetir salvamento' });
  const practice = (await panel()).getByRole('button', { name: 'Praticar M01' });fireEvent.click(practice);
  expect(confirm).toHaveBeenCalledTimes(1);expect(screen.getAllByRole('radio')[0]).toBeChecked();expect(screen.queryByText(/Prática direcionada de/)).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Repetir salvamento' }));await waitFor(() => expect(saveAnswer).toHaveBeenCalledTimes(2));
  expect(vi.mocked(saveAnswer).mock.calls[0][2]).toEqual(vi.mocked(saveAnswer).mock.calls[1][2]);
  confirm.mockReturnValue(true);fireEvent.click(practice);await screen.findByText(/Prática direcionada de M01/);expect(screen.queryByRole('radio')).not.toBeInTheDocument();expect(createAttempt).toHaveBeenCalledTimes(1);
});
it('troca de cargo cancela preenchimento pendente e impede catálogo tardio de restaurar seleção antiga', async () => {
  render(<AttemptWorkspace {...props} />);const area = await panel();let resolve!: (value: typeof catalog) => void;let signal!: AbortSignal;
  vi.mocked(questionCatalog).mockImplementationOnce(s => { signal = s;return new Promise(done => { resolve = done; }); });
  fireEvent.click(area.getByRole('button', { name: 'Praticar M01' }));expect(screen.getByText(/Prática direcionada de M01/)).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText('Cargo para revisão'), { target: { value: 'secretario-de-escola' } });expect(signal.aborted).toBe(true);
  await act(async () => resolve(catalog));await screen.findByLabelText('Quantidade de Português');expect(screen.queryByText(/Prática direcionada/)).not.toBeInTheDocument();expect(screen.getByLabelText('Operações')).not.toBeChecked();expect(createAttempt).not.toHaveBeenCalled();
});
it('criação em voo bloqueia atalhos e desmontagem aborta sem aplicar confirmação tardia', async () => {
  let resolve!: (v: typeof view) => void;let signal!: AbortSignal;vi.mocked(createAttempt).mockImplementation((_c, _id, s) => { signal = s;return new Promise(done => { resolve = done; }); });
  const ui = render(<AttemptWorkspace {...props} />);const area = await panel();fireEvent.click(screen.getByRole('button', { name: 'Iniciar simulado' }));expect(area.getByRole('button', { name: 'Praticar M01' })).toBeDisabled();
  ui.unmount();expect(signal.aborted).toBe(true);await act(async () => resolve(view));expect(screen.queryByRole('radio')).not.toBeInTheDocument();
});
