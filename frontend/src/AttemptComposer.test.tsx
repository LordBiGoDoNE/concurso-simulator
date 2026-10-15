import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import AttemptComposer from './AttemptComposer';
import { catalog } from '../test/question-fixtures';
import { view } from '../test/attempt-fixtures';
import { AttemptApiError } from './attempt-api';
vi.mock('./question-api', () => ({ questionCatalog: vi.fn() }));
vi.mock('./attempt-api', async importOriginal => ({ ...await importOriginal<typeof import('./attempt-api')>(), createAttempt: vi.fn() }));
import { questionCatalog } from './question-api';
import { createAttempt } from './attempt-api';
afterEach(() => { vi.clearAllMocks(); });
it('valida 1–100, mostra limites livres e impede duplo clique enquanto cria', async () => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog); let finish!: (v: typeof view) => void;
  vi.mocked(createAttempt).mockImplementation(() => new Promise(resolve => { finish = resolve; }));
  const created = vi.fn(); render(<AttemptComposer onCreated={created} onUnavailable={vi.fn()} />);
  const quantity = await screen.findByLabelText('Quantidade de Português');
  fireEvent.change(quantity, { target: { value: '101' } }); expect(screen.getByRole('button', { name: 'Iniciar simulado' })).toBeDisabled();
  fireEvent.change(quantity, { target: { value: '2' } }); const start = screen.getByRole('button', { name: 'Iniciar simulado' });
  fireEvent.click(start); fireEvent.click(start); expect(createAttempt).toHaveBeenCalledTimes(1);
  expect(screen.getByText(/não são regras oficiais/)).toBeInTheDocument(); finish(view); await waitFor(() => expect(created).toHaveBeenCalledWith(view));
});
it('insuficiência mantém operação no retry e critérios alterados geram outra operação', async () => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog); vi.mocked(createAttempt).mockRejectedValue(new AttemptApiError(409, [{ disciplineId: 'portugues', available: 1 }]));
  render(<AttemptComposer onCreated={vi.fn()} onUnavailable={vi.fn()} />); fireEvent.click(await screen.findByRole('button', { name: 'Iniciar simulado' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Português: 1 disponíveis');
  fireEvent.click(screen.getByRole('button', { name: 'Tentar criar novamente' })); await waitFor(() => expect(createAttempt).toHaveBeenCalledTimes(2));
  expect(vi.mocked(createAttempt).mock.calls[0][1]).toBe(vi.mocked(createAttempt).mock.calls[1][1]);
  await waitFor(() => expect(screen.getByRole('button', { name: 'Tentar criar novamente' })).toBeEnabled());
  fireEvent.change(screen.getByLabelText('Quantidade de Português'), { target: { value: '1' } }); fireEvent.click(screen.getByRole('button', { name: 'Tentar criar novamente' }));
  await waitFor(() => expect(createAttempt).toHaveBeenCalledTimes(3)); expect(vi.mocked(createAttempt).mock.calls[2][1]).not.toBe(vi.mocked(createAttempt).mock.calls[0][1]);
});
it('desmontagem aborta criação e ignora resposta tardia', async () => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog); let finish!: (v: typeof view) => void; let signal!: AbortSignal;
  vi.mocked(createAttempt).mockImplementation((_c, _id, s) => { signal = s; return new Promise(resolve => { finish = resolve; }); });
  const created = vi.fn(); const { unmount } = render(<AttemptComposer onCreated={created} onUnavailable={vi.fn()} />);
  fireEvent.click(await screen.findByRole('button', { name: 'Iniciar simulado' })); unmount(); expect(signal.aborted).toBe(true); finish(view); await Promise.resolve(); expect(created).not.toHaveBeenCalled();
});
