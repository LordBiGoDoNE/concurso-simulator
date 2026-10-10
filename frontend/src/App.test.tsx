import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });

describe('entrada da aplicação', () => {
  it('mostra disponibilidade e permite nova tentativa', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ status: 'UP' }) });
    vi.stubGlobal('fetch', fetchMock);
    render(<App />);
    expect(await screen.findByText('API disponível.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Verificar novamente' }));
    await waitFor(() => expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/status'))).toHaveLength(2));
  });

  it.each(['503', 'network', 'invalid'])('mantém aulas acessíveis quando ocorre %s', async failure => {
    vi.stubGlobal('fetch', failure === 'network'
      ? vi.fn().mockRejectedValue(new TypeError('offline'))
      : vi.fn().mockResolvedValue({ ok: failure !== '503', json: async () => ({ status: failure === '503' ? 'DOWN' : 'UNKNOWN' }) }));
    render(<App />);
    expect(await screen.findByText(/API indisponível/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Acessar material de estudo' })).toHaveAttribute('href', 'https://lordbigodone.github.io/concurso-simulator/');
    expect(screen.getByRole('button', { name: 'Verificar novamente' })).toBeEnabled();
  });

  it('interrompe a consulta no timeout e informa indisponibilidade', async () => {
    vi.useFakeTimers();
    vi.stubGlobal('fetch', vi.fn((_url, options: RequestInit) => new Promise((_resolve, reject) => {
      options.signal?.addEventListener('abort', () => reject(new DOMException('timeout', 'AbortError')));
    })));
    render(<App />);
    await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
    expect(screen.getByText(/API indisponível/)).toBeInTheDocument();
  });
});
