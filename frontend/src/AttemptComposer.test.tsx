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
  const variations = screen.getByRole('checkbox', { name: 'Usar variações numéricas quando disponíveis' });
  expect(variations).not.toBeChecked();expect(variations).toHaveAccessibleDescription(expect.stringContaining('não há garantia de questão nova'));
  fireEvent.change(quantity, { target: { value: '101' } }); expect(screen.getByRole('button', { name: 'Iniciar simulado' })).toBeDisabled();
  fireEvent.change(quantity, { target: { value: '2' } }); const start = screen.getByRole('button', { name: 'Iniciar simulado' });
  fireEvent.click(start); fireEvent.click(start); expect(createAttempt).toHaveBeenCalledTimes(1);
  expect(variations).toBeDisabled();expect(vi.mocked(createAttempt).mock.calls[0][0].useParameterizedQuestions).toBe(false);
  expect(screen.getByText(/não são regras oficiais/)).toBeInTheDocument(); finish(view); await waitFor(() => expect(created).toHaveBeenCalledWith(view));
});
it('mantém opção e comando após confirmação perdida e muda operação ao mudar a opção', async () => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog);vi.mocked(createAttempt).mockRejectedValue(new TypeError('network'));
  render(<AttemptComposer onCreated={vi.fn()} onUnavailable={vi.fn()} />);
  const option = await screen.findByRole('checkbox', { name: 'Usar variações numéricas quando disponíveis' });
  fireEvent.click(option);fireEvent.click(screen.getByRole('button', { name: 'Iniciar simulado' }));
  const retry = await screen.findByRole('button', { name: 'Tentar criar novamente' });expect(option).toBeChecked();
  await waitFor(() => expect(retry).toBeEnabled());fireEvent.click(retry);await waitFor(() => expect(createAttempt).toHaveBeenCalledTimes(2));
  const calls = vi.mocked(createAttempt).mock.calls;expect(calls[0][0]).toEqual(calls[1][0]);expect(calls[0][1]).toBe(calls[1][1]);expect(calls[0][0].useParameterizedQuestions).toBe(true);
  await waitFor(() => expect(retry).toBeEnabled());fireEvent.click(option);fireEvent.click(retry);await waitFor(() => expect(createAttempt).toHaveBeenCalledTimes(3));
  expect(calls[2][1]).not.toBe(calls[0][1]);expect(calls[2][0].useParameterizedQuestions).toBe(false);
});
it('insuficiência mantém operação no retry e critérios alterados geram outra operação', async () => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog); vi.mocked(createAttempt).mockRejectedValue(new AttemptApiError(409, [{ disciplineId: 'portugues', available: 1 }]));
  render(<AttemptComposer onCreated={vi.fn()} onUnavailable={vi.fn()} />); fireEvent.click(await screen.findByRole('button', { name: 'Iniciar simulado' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Português: 1 disponíveis');
  fireEvent.click(screen.getByRole('button', { name: 'Tentar criar novamente' })); await waitFor(() => expect(createAttempt).toHaveBeenCalledTimes(2));
  expect(vi.mocked(createAttempt).mock.calls[0][1]).toBe(vi.mocked(createAttempt).mock.calls[1][1]);
  await waitFor(() => expect(screen.getByRole('button', { name: 'Tentar criar novamente' })).toBeEnabled());
  fireEvent.change(screen.getByLabelText('Quantidade de Português'), { target: { value: '1' } }); fireEvent.click(screen.getByRole('button', { name: 'Iniciar simulado' }));
  await waitFor(() => expect(createAttempt).toHaveBeenCalledTimes(3)); expect(vi.mocked(createAttempt).mock.calls[2][1]).not.toBe(vi.mocked(createAttempt).mock.calls[0][1]);
});
it('desmontagem aborta criação e ignora resposta tardia', async () => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog); let finish!: (v: typeof view) => void; let signal!: AbortSignal;
  vi.mocked(createAttempt).mockImplementation((_c, _id, s) => { signal = s; return new Promise(resolve => { finish = resolve; }); });
  const created = vi.fn(); const { unmount } = render(<AttemptComposer onCreated={created} onUnavailable={vi.fn()} />);
  fireEvent.click(await screen.findByRole('button', { name: 'Iniciar simulado' })); unmount(); expect(signal.aborted).toBe(true); finish(view); await Promise.resolve(); expect(created).not.toHaveBeenCalled();
});

it('preenche somente o assunto solicitado, sem POST, e permite mudar critérios e opção explicitamente', async () => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog);vi.mocked(createAttempt).mockRejectedValue(new TypeError('network'));
  render(<AttemptComposer prefill={{ roleId: 'secretario-de-escola', disciplineId: 'matematica-raciocinio', topicId: 'M01' }} onCreated={vi.fn()} onUnavailable={vi.fn()} />);
  await waitFor(() => expect(screen.getByLabelText('Quantidade de Matemática')).toHaveValue(1));
  expect(screen.getByLabelText('Quantidade de Português')).toHaveValue(0);expect(screen.getByLabelText('Cargo do simulado')).toHaveValue('secretario-de-escola');
  expect(screen.getByLabelText('Operações')).toBeChecked();expect(screen.getByLabelText('Interpretação')).not.toBeChecked();
  const option = screen.getByLabelText('Usar variações numéricas quando disponíveis');expect(option).not.toBeChecked();expect(createAttempt).not.toHaveBeenCalled();
  expect(screen.getByRole('heading', { name: 'Compor simulado' })).toHaveFocus();
  fireEvent.change(screen.getByLabelText('Quantidade de Matemática'), { target: { value: '2' } });fireEvent.click(option);fireEvent.click(screen.getByRole('button', { name: 'Iniciar simulado' }));
  await screen.findByRole('alert');expect(vi.mocked(createAttempt).mock.calls[0][0]).toEqual({ roleId: 'secretario-de-escola', disciplines: [{ disciplineId: 'matematica-raciocinio', quantity: 2, topicIds: ['M01'] }], useParameterizedQuestions: true });
});
it('mudança de critérios descarta comando mesmo ao voltar aos valores anteriores', async () => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog);vi.mocked(createAttempt).mockRejectedValue(new TypeError('network'));
  render(<AttemptComposer onCreated={vi.fn()} onUnavailable={vi.fn()} />);fireEvent.click(await screen.findByRole('button', { name: 'Iniciar simulado' }));await screen.findByRole('alert');
  const quantity = screen.getByLabelText('Quantidade de Português');fireEvent.change(quantity, { target: { value: '1' } });fireEvent.change(quantity, { target: { value: '5' } });
  fireEvent.click(screen.getByRole('button', { name: 'Iniciar simulado' }));await screen.findByRole('alert');const calls = vi.mocked(createAttempt).mock.calls;
  expect(calls[0][0]).toEqual(calls[1][0]);expect(calls[0][1]).not.toBe(calls[1][1]);
});
it('seleção fora do catálogo falha sem fallback nem criação', async () => {
  vi.mocked(questionCatalog).mockResolvedValue(catalog);
  render(<AttemptComposer prefill={{ roleId: view.roleId, disciplineId: 'portugues', topicId: 'M01' }} onCreated={vi.fn()} onUnavailable={vi.fn()} />);
  await screen.findByRole('alert');expect(screen.queryByRole('button', { name: 'Iniciar simulado' })).not.toBeInTheDocument();expect(createAttempt).not.toHaveBeenCalled();
});
