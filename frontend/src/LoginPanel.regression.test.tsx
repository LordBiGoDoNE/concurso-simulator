import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import App from './App';

const id = '12345678-1234-1234-1234-123456789abc';
const response = (body: unknown, status = 200) => ({ ok: status >= 200 && status < 300, status, json: async () => body });
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });

it.each(['/auth/config', '/me', '/csrf'])('UI recupera de JSON pendente em %s e mantém aulas/retry', async stalled => {
  vi.useFakeTimers();
  let stall = stalled !== '/csrf';
  let signal!: AbortSignal;
  vi.stubGlobal('fetch', vi.fn(async (url: string, options: RequestInit) => {
    if (stall && url.endsWith(stalled)) {
      signal = options.signal as AbortSignal;
      return { ok: true, status: 200, json: () => new Promise(() => {}) };
    }
    if (url.endsWith('/status')) return response({ status: 'UP' });
    if (url.endsWith('/auth/config')) return response({ googleEnabled: true });
    if (url.endsWith('/me')) return response({ id });
    if (url.endsWith('/csrf')) return response({ token: 'fresh-csrf', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/auth/logout')) return response(undefined, 204);
    throw new Error('unexpected path');
  }));
  render(<App />);
  await act(async () => { await vi.advanceTimersByTimeAsync(0); });
  if (stalled === '/csrf') {
    stall = true;
    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));
    expect(screen.getByRole('button', { name: 'Saindo…' })).toBeDisabled();
  }
  await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
  expect(signal.aborted).toBe(true);
  expect(vi.getTimerCount()).toBe(0);
  expect(screen.getByRole('link', { name: 'Acessar material de estudo' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Atualizar sessão' })).toBeEnabled();
  stall = false;
  if (stalled === '/csrf') {
    expect(screen.getByRole('alert')).toHaveTextContent(/sessão pode continuar ativa/);
    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));
    await act(async () => { await vi.advanceTimersByTimeAsync(0); });
    expect(screen.getByText('Você está estudando como visitante.')).toBeInTheDocument();
  } else {
    expect(screen.getByText(/Consulta de sessão indisponível/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar sessão' }));
    await act(async () => { await vi.advanceTimersByTimeAsync(0); });
    expect(screen.getByText('Você está conectado.')).toBeInTheDocument();
  }
});

it.each([204, 401, 403, 500].flatMap(status => [true, false].map(lateSuccess => ({ status, lateSuccess }))))('consulta por foco tardia ($lateSuccess) não sobrescreve logout $status; atualização/retry continuam válidos', async ({ status, lateSuccess }) => {
  let completeLogout!: (value: unknown) => void;
  let completeRefresh!: (value: unknown) => void;
  const logout = new Promise(resolve => { completeLogout = resolve; });
  const refresh = new Promise(resolve => { completeRefresh = resolve; });
  let meCalls = 0;
  let logoutCalls = 0;
  let csrfCalls = 0;
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible');
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    if (url.endsWith('/status')) return response({ status: 'UP' });
    if (url.endsWith('/auth/config')) return response({ googleEnabled: true });
    if (url.endsWith('/me')) {
      meCalls += 1;
      return meCalls === 1 ? response({ id }) : meCalls === 2 ? refresh : response(undefined, 401);
    }
    if (url.endsWith('/csrf')) return response({ token: `csrf-${++csrfCalls}`, headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/auth/logout')) return ++logoutCalls === 1 ? logout : response(undefined, 204);
    throw new Error('unexpected path');
  }));
  render(<App />);
  fireEvent.click(await screen.findByRole('button', { name: 'Sair' }));
  await act(async () => { await Promise.resolve(); });
  expect(logoutCalls).toBe(1);
  fireEvent(window, new Event('focus'));
  await act(async () => { await Promise.resolve(); });
  expect(meCalls).toBe(2);
  await act(async () => { completeLogout(response(undefined, status)); });
  const success = status === 204 || status === 401;
  expect(screen.getByRole('alert')).toHaveTextContent(success ? /Você está sem sessão/ : status === 403 ? /renovar a proteção/ : /sessão pode continuar ativa/);
  await act(async () => { completeRefresh(lateSuccess ? response({ id }) : response(undefined, 500)); });
  expect(screen.getByText(success ? 'Você está estudando como visitante.' : 'Você está conectado.')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Atualizar sessão' })).toBeEnabled();
  if (success) {
    expect(screen.queryByRole('button', { name: 'Sair' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Entrar com Google' })).toBeInTheDocument();
  } else {
    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));
    expect(await screen.findByText('Você está estudando como visitante.')).toBeInTheDocument();
    expect(csrfCalls).toBe(2);
  }
  fireEvent.click(screen.getByRole('button', { name: 'Atualizar sessão' }));
  expect(await screen.findByText('Você está estudando como visitante.')).toBeInTheDocument();
  expect(meCalls).toBe(3);
  expect(screen.getByRole('link', { name: 'Acessar material de estudo' })).toBeInTheDocument();
});

it.each([null, { headerName: 'Wrong', token: 'token' }, { headerName: 'X-CSRF-TOKEN', token: '' }])('CSRF inválido %j não envia logout nem bloqueia retry', async token => {
  const fetchMock = vi.fn(async (url: string) => {
    if (url.endsWith('/status')) return response({ status: 'UP' });
    if (url.endsWith('/auth/config')) return response({ googleEnabled: true });
    if (url.endsWith('/me')) return response({ id });
    if (url.endsWith('/csrf')) return response(token);
    throw new Error('unexpected path');
  });
  vi.stubGlobal('fetch', fetchMock);
  render(<App />);
  fireEvent.click(await screen.findByRole('button', { name: 'Sair' }));
  expect(await screen.findByRole('alert')).toHaveTextContent(/sessão pode continuar ativa/);
  expect(screen.getByRole('button', { name: 'Sair' })).toBeEnabled();
  expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/auth/logout'))).toBe(false);
  expect(screen.getByRole('link', { name: 'Acessar material de estudo' })).toBeInTheDocument();
});
