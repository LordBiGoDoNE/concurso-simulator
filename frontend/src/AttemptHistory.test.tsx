import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import AttemptHistory from './AttemptHistory';
import { attemptHistory, type HistoryPage } from './attempt-api';
import { view } from '../test/attempt-fixtures';
vi.mock('./attempt-api', async original => ({ ...await original<typeof import('./attempt-api')>(), attemptHistory: vi.fn() }));
afterEach(() => vi.clearAllMocks());
const entry = { id: view.id, roleId: view.roleId, createdAt: view.createdAt, finishedAt: null, total: 1, percentage: null };
it('lista andamento sem nota, final com nota e páginas vazias sem repetir dados da página anterior', async () => {
  vi.mocked(attemptHistory).mockResolvedValueOnce({ page: 0, size: 20, items: Array.from({ length: 20 }, (_, i) => ({ ...entry, id: `${i}`, ...(i === 0 ? { finishedAt: view.createdAt, percentage: 60 } : {}) })) })
    .mockResolvedValueOnce({ page: 1, size: 20, items: [] });
  const open = vi.fn(); render(<AttemptHistory refresh={0} onOpen={open} onUnavailable={vi.fn()} />);
  expect(await screen.findByText('Concluído — 60%.')).toBeInTheDocument(); expect(screen.getAllByText('Em andamento — sem nota parcial.')).toHaveLength(19);
  fireEvent.click(screen.getAllByRole('button', { name: /Abrir simulado/ })[0]); expect(open).toHaveBeenCalledWith('0');
  fireEvent.click(screen.getByRole('button', { name: 'Próxima página do histórico' })); expect(await screen.findByText('Nenhuma tentativa nesta página.')).toBeInTheDocument();
  expect(screen.queryByText('Concluído — 60%.')).not.toBeInTheDocument(); expect(screen.getByRole('button', { name: 'Página anterior do histórico' })).toBeEnabled();
});
it('descarta página tardia e aborta o conteúdo privado na desmontagem', async () => {
  let done!: (v: HistoryPage) => void;let signal!: AbortSignal;
  vi.mocked(attemptHistory).mockImplementationOnce((_page, s) => { signal = s; return new Promise(resolve => { done = resolve; }); }).mockResolvedValue({ page: 0, size: 20, items: [] });
  const { rerender, unmount } = render(<AttemptHistory refresh={0} onOpen={vi.fn()} onUnavailable={vi.fn()} />);
  rerender(<AttemptHistory refresh={1} onOpen={vi.fn()} onUnavailable={vi.fn()} />); await screen.findByText('Nenhuma tentativa nesta página.'); expect(signal.aborted).toBe(true);
  done({ page: 0, size: 20, items: [entry] });await waitFor(() => expect(screen.queryByRole('button', { name: /Abrir simulado/ })).not.toBeInTheDocument()); unmount();
});
