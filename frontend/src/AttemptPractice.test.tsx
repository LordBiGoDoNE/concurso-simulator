import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import AttemptPractice from './AttemptPractice';
import { AttemptApiError, readAttempt, saveAnswer } from './attempt-api';
import { view } from '../test/attempt-fixtures';
import type { AttemptView } from './attempt-api';
vi.mock('./attempt-api', async original => ({ ...await original<typeof import('./attempt-api')>(), readAttempt: vi.fn(), saveAnswer: vi.fn() }));
afterEach(() => vi.clearAllMocks());
const first = view.items[0].alternatives[0].id; const second = view.items[0].alternatives[1].id;
it('serializa alterações rápidas, usa a versão confirmada e permite limpar', async () => {
  let done!: (v: AttemptView) => void;
  vi.mocked(saveAnswer).mockImplementationOnce(() => new Promise(resolve => { done = resolve; })).mockResolvedValueOnce({ ...view, version: 2, items: [{ ...view.items[0], selectedAlternativeId: second }] });
  render(<AttemptPractice initial={view} onUnavailable={vi.fn()} />);
  fireEvent.click(screen.getAllByRole('radio')[0]); fireEvent.click(screen.getAllByRole('radio')[1]);
  expect(saveAnswer).toHaveBeenCalledTimes(1); expect(screen.getAllByRole('radio')[1]).toBeChecked(); expect(screen.getByText('Salvando fila…')).toBeInTheDocument();
  done({ ...view, version: 1, items: [{ ...view.items[0], selectedAlternativeId: first }] });
  await waitFor(() => expect(saveAnswer).toHaveBeenCalledTimes(2)); expect(vi.mocked(saveAnswer).mock.calls[1][2]).toMatchObject({ expectedVersion: 1, alternativeId: second });
  await screen.findByText('Respostas confirmadas.');
  vi.mocked(saveAnswer).mockResolvedValue({ ...view, version: 3 });
  fireEvent.click(screen.getByRole('button', { name: 'Limpar resposta' }));
  await waitFor(() => expect(saveAnswer).toHaveBeenCalledTimes(3));
  expect(vi.mocked(saveAnswer).mock.calls[2][2]).toMatchObject({ expectedVersion: 2, alternativeId: null });
  await screen.findByText('Respostas confirmadas.'); expect(screen.getAllByRole('radio').every(r => !(r as HTMLInputElement).checked)).toBe(true);
});
it('retry mantém ID/payload da gravação cuja confirmação falhou', async () => {
  vi.mocked(saveAnswer).mockRejectedValueOnce(new TypeError('timeout')).mockResolvedValue({ ...view, version: 1, items: [{ ...view.items[0], selectedAlternativeId: first }] });
  render(<AttemptPractice initial={view} onUnavailable={vi.fn()} />); fireEvent.click(screen.getAllByRole('radio')[0]);
  fireEvent.click(await screen.findByRole('button', { name: 'Repetir salvamento' })); await screen.findByText('Respostas confirmadas.');
  expect(vi.mocked(saveAnswer).mock.calls[0][2]).toEqual(vi.mocked(saveAnswer).mock.calls[1][2]); expect(screen.getAllByRole('radio')[0]).toBeChecked();
});
it('conflito recarrega versão atual e preserva escolha sem reenviar até ação explícita', async () => {
  vi.mocked(saveAnswer).mockRejectedValueOnce(new AttemptApiError(409)).mockResolvedValue({ ...view, version: 3, items: [{ ...view.items[0], selectedAlternativeId: first }] });
  vi.mocked(readAttempt).mockResolvedValue({ ...view, version: 2, items: [{ ...view.items[0], selectedAlternativeId: second }] });
  render(<AttemptPractice initial={view} onUnavailable={vi.fn()} />); fireEvent.click(screen.getAllByRole('radio')[0]);
  fireEvent.click(await screen.findByRole('button', { name: 'Recarregar tentativa' }));
  expect(await screen.findByText(/Escolhas locais preservadas/)).toBeInTheDocument(); expect(screen.getAllByRole('radio')[0]).toBeChecked(); expect(saveAnswer).toHaveBeenCalledTimes(1);
  fireEvent.click(screen.getByRole('button', { name: 'Salvar escolhas preservadas' })); await screen.findByText('Respostas confirmadas.');
  expect(vi.mocked(saveAnswer).mock.calls[1][2]).toMatchObject({ expectedVersion: 2, alternativeId: first });
  expect(vi.mocked(saveAnswer).mock.calls[0][2].commandId).not.toBe(vi.mocked(saveAnswer).mock.calls[1][2].commandId);
});
it('navegação mantém IDs/ordens e desmontagem ignora gravação tardia', async () => {
  const more = { ...view, items: [...view.items, { ...view.items[0], id: '80000000-0000-0000-0000-000000000002', questionId: '10000000-0000-0000-0000-000000000002' }] };
  let done!: (v: AttemptView) => void; let signal!: AbortSignal;
  vi.mocked(saveAnswer).mockImplementation((_id, _item, _body, s) => { signal = s; return new Promise(resolve => { done = resolve; }); });
  const { unmount } = render(<AttemptPractice initial={more} onUnavailable={vi.fn()} />); fireEvent.click(screen.getAllByRole('radio')[1]);
  fireEvent.click(screen.getByRole('button', { name: 'Próxima questão' })); expect(screen.getByRole('heading', { name: 'Questão 2 de 2' })).toHaveFocus();
  fireEvent.click(screen.getByRole('button', { name: 'Questão anterior' })); expect(screen.getAllByRole('radio')[1]).toBeChecked();
  unmount(); expect(signal.aborted).toBe(true); done(more); await Promise.resolve();
});
