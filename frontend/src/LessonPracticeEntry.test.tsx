import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import LessonPracticeEntry from './LessonPracticeEntry';
import { lessons } from './study-catalog';

const topic = lessons.find(t => t.id === 'P01')!;
const response = (available = 5, patch = {}) => ({ ok: true, status: 200, json: async () => ({ topicId: topic.id, available, maximum: 5, ...patch }) });
const props = () => ({ topic, onPractice: vi.fn(), active: true });
afterEach(() => vi.unstubAllGlobals());

it('só permite início após disponibilidade confirmada e clique explícito', async () => {
  let complete!: (value: unknown) => void;
  const fetchMock = vi.fn(() => new Promise(resolve => { complete = resolve; }));
  vi.stubGlobal('fetch', fetchMock);
  const value = props();render(<LessonPracticeEntry {...value} />);
  expect(screen.getByRole('button', { name: 'Começar prática' })).toBeDisabled();
  await act(async () => complete(response(8)));
  expect(screen.getByRole('status')).toHaveTextContent('até 5 questões');
  expect(value.onPractice).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: 'Começar prática' }));
  expect(value.onPractice).toHaveBeenCalledWith({ topicId: 'P01' });
  expect(fetchMock).toHaveBeenCalledTimes(1);
  expect(fetchMock.mock.calls[0]).toEqual([expect.stringContaining('/availability?topicId=P01'), expect.objectContaining({ method: 'GET' })]);
});
it('acervo vazio é distinto de erro e não cria prática', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => response(0)));
  const value = props();render(<LessonPracticeEntry {...value} />);
  expect(await screen.findByText(/ainda não tem questões/)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Começar prática' })).toBeDisabled();
  expect(value.onPractice).not.toHaveBeenCalled();
});
it.each([{ roleId: 'outro' }, { topicId: 'M01' }, { available: -1 }, { available: 1.5 }, { maximum: 10 }, { extra: true }])('contrato incompatível é erro, com nova consulta: %j', async patch => {
  const fetchMock = vi.fn().mockResolvedValueOnce(response(5, patch)).mockResolvedValue(response(2));
  vi.stubGlobal('fetch', fetchMock);
  const value = props();render(<LessonPracticeEntry {...value} />);
  expect(await screen.findByRole('alert')).toHaveTextContent('não significa ausência');
  expect(screen.getByRole('button', { name: 'Começar prática' })).toBeDisabled();
  fireEvent.click(screen.getByRole('button', { name: 'Consultar prática novamente' }));
  await waitFor(() => expect(screen.getByRole('button', { name: 'Começar prática' })).toBeEnabled());
  expect(value.onPractice).not.toHaveBeenCalled();
});
it('ignora consulta tardia de outro assunto e não inicia ao trocar aula', async () => {
  let complete!: (value: unknown) => void;
  vi.stubGlobal('fetch', vi.fn().mockImplementationOnce(() => new Promise(resolve => { complete = resolve; })).mockResolvedValue(response(0, { topicId: 'M01' })));
  const value = props();const mounted = render(<LessonPracticeEntry {...value} />);
  mounted.rerender(<LessonPracticeEntry {...value} topic={lessons.find(t => t.id === 'M01')!} />);
  await screen.findByText(/ainda não tem questões/);
  await act(async () => complete(response(5)));
  expect(screen.getByRole('button', { name: 'Começar prática' })).toBeDisabled();
  expect(value.onPractice).not.toHaveBeenCalled();
});
it('consulta confirmada não libera início enquanto a identidade está sendo conferida', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => response()));
  const value = props();render(<LessonPracticeEntry {...value} startEnabled={false} />);
  await screen.findByText(/Confira sua sessão antes de iniciar/);
  expect(screen.getByRole('button', { name: 'Começar prática' })).toBeDisabled();
  expect(value.onPractice).not.toHaveBeenCalled();
});
it('sair da área aborta a consulta e voltar exige uma confirmação nova', async () => {
  const fetchMock = vi.fn().mockResolvedValueOnce(response()).mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal('fetch', fetchMock);
  const value = props();const mounted = render(<LessonPracticeEntry {...value} />);
  await waitFor(() => expect(screen.getByRole('button', { name: 'Começar prática' })).toBeEnabled());
  mounted.rerender(<LessonPracticeEntry {...value} active={false} />);
  mounted.rerender(<LessonPracticeEntry {...value} />);
  expect(screen.getByRole('button', { name: 'Começar prática' })).toBeDisabled();
  expect(fetchMock).toHaveBeenCalledTimes(2);
  mounted.unmount();
  expect(fetchMock.mock.calls[1][1].signal.aborted).toBe(true);
});
