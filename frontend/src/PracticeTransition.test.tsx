import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { createRef } from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import AttemptWorkspace, { type AttemptWorkspaceHandle } from './AttemptWorkspace';
import { questionCatalog } from './question-api';
import { AttemptApiError, createAttempt, finishAttempt, readAttempt, saveAnswer, type AttemptView } from './attempt-api';
import { catalog } from '../test/question-fixtures';
import { view, result } from '../test/attempt-fixtures';

vi.mock('./question-api', () => ({ questionCatalog: vi.fn() }));
vi.mock('./attempt-api', async original => ({ ...await original<typeof import('./attempt-api')>(), createAttempt: vi.fn(), saveAnswer: vi.fn(), readAttempt: vi.fn(), finishAttempt: vi.fn() }));
const selection = { roleId: view.roleId, disciplineId: 'matematica-raciocinio', topicId: 'M01' };
beforeEach(() => vi.mocked(questionCatalog).mockResolvedValue(catalog));
afterEach(() => { vi.restoreAllMocks();vi.resetAllMocks(); });
async function start(initial = view) {
  vi.mocked(createAttempt).mockResolvedValue(initial);
  const handle = createRef<AttemptWorkspaceHandle>();
  render(<AttemptWorkspace ref={handle} identity={{ connected: false, googleEnabled: false }} onUnavailable={vi.fn()} onGuestActive={vi.fn()} />);
  fireEvent.click(await screen.findByRole('button', { name: 'Iniciar simulado' }));await screen.findByRole('heading', { name: `Questão 1 de ${initial.items.length}` });
  return handle;
}
it('prática da aula não desmonta gravação suspensa nem descarta o segundo trabalho da fila', async () => {
  const initial = { ...view, items: [view.items[0], { ...view.items[0], id: '80000000-0000-0000-0000-000000000002', questionId: '10000000-0000-0000-0000-000000000002' }] };
  const completions: ((value: AttemptView) => void)[] = [];
  vi.mocked(saveAnswer).mockImplementation(() => new Promise(resolve => completions.push(resolve)));
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);const handle = await start(initial);
  fireEvent.click(screen.getAllByRole('radio')[0]);fireEvent.click(screen.getByRole('button', { name: 'Próxima questão' }));fireEvent.click(screen.getAllByRole('radio')[0]);
  await waitFor(() => expect(saveAnswer).toHaveBeenCalledTimes(1));
  let accepted!: boolean;act(() => { accepted = handle.current!.practice(selection); });expect(accepted).toBe(false);expect(confirm).not.toHaveBeenCalled();
  expect(screen.getByRole('heading', { name: 'Questão 2 de 2' })).toBeInTheDocument();expect(screen.getAllByRole('radio')[0]).toBeChecked();
  expect(vi.mocked(saveAnswer).mock.calls[0][3].aborted).toBe(false);
  const firstSaved = { ...initial, version: 1, items: [{ ...initial.items[0], selectedAlternativeId: initial.items[0].alternatives[0].id }, initial.items[1]] };
  await act(async () => completions[0](firstSaved));await waitFor(() => expect(saveAnswer).toHaveBeenCalledTimes(2));
  act(() => { accepted = handle.current!.practice(selection); });expect(accepted).toBe(false);
  expect(vi.mocked(saveAnswer).mock.calls[1][2]).toMatchObject({ expectedVersion: 1, alternativeId: initial.items[1].alternatives[0].id });
  expect(vi.mocked(saveAnswer).mock.calls[1][3].aborted).toBe(false);
  await act(async () => completions[1]({ ...firstSaved, version: 2, items: [firstSaved.items[0], { ...initial.items[1], selectedAlternativeId: initial.items[1].alternatives[0].id }] }));
  await screen.findByText('Respostas confirmadas.');act(() => { accepted = handle.current!.practice(selection); });expect(accepted).toBe(true);
  await screen.findByText(/Prática direcionada de M01/);expect(createAttempt).toHaveBeenCalledTimes(1);expect(saveAnswer).toHaveBeenCalledTimes(2);
});
it('conclusão suspensa bloqueia prática e composição alternativa até confirmar o resultado', async () => {
  let complete!: (value: { attempt: AttemptView; result: typeof result }) => void;
  vi.mocked(finishAttempt).mockImplementation(() => new Promise(resolve => { complete = resolve; }));
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);const handle = await start();
  fireEvent.click(screen.getByRole('button', { name: 'Concluir simulado' }));fireEvent.click(screen.getByRole('button', { name: 'Confirmar conclusão' }));
  await waitFor(() => expect(finishAttempt).toHaveBeenCalledTimes(1));
  let accepted!: boolean;act(() => { accepted = handle.current!.practice(selection); });expect(accepted).toBe(false);
  fireEvent.click(screen.getByRole('button', { name: 'Compor outro simulado' }));expect(confirm).not.toHaveBeenCalled();
  expect(vi.mocked(finishAttempt).mock.calls[0][3].aborted).toBe(false);
  await act(async () => complete({ attempt: { ...view, version: 1, finishedAt: '2026-10-23T16:00:00Z' }, result }));
  await screen.findByRole('heading', { name: 'Resultado do simulado' });act(() => { accepted = handle.current!.practice(selection); });expect(accepted).toBe(true);expect(finishAttempt).toHaveBeenCalledTimes(1);
});
it('recarga suspensa preserva a escolha e recusa substituição até receber a versão atual', async () => {
  vi.mocked(saveAnswer).mockRejectedValue(new AttemptApiError(409));let complete!: (value: AttemptView) => void;
  vi.mocked(readAttempt).mockImplementation(() => new Promise(resolve => { complete = resolve; }));
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);const handle = await start();
  fireEvent.click(screen.getAllByRole('radio')[0]);fireEvent.click(await screen.findByRole('button', { name: 'Recarregar tentativa' }));
  await waitFor(() => expect(readAttempt).toHaveBeenCalledTimes(1));let accepted!: boolean;act(() => { accepted = handle.current!.practice(selection); });
  expect(accepted).toBe(false);expect(confirm).not.toHaveBeenCalled();expect(vi.mocked(readAttempt).mock.calls[0][1].aborted).toBe(false);
  await act(async () => complete({ ...view, version: 2 }));await screen.findByRole('button', { name: 'Salvar escolhas preservadas' });expect(screen.getAllByRole('radio')[0]).toBeChecked();
  act(() => { accepted = handle.current!.practice(selection); });expect(accepted).toBe(true);expect(createAttempt).toHaveBeenCalledTimes(1);
});
