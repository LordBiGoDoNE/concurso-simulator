import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';

const id = '12345678-1234-1234-1234-123456789abc';
const response = (body: unknown, status = 200) => ({ ok: status >= 200 && status < 300, status, json: async () => body });

function api({ enabled = true, connected = false, logout = 204, failure = '' } = {}) {
  let active = connected;
  const fetchMock = vi.fn(async (url: string, options?: RequestInit) => {
    if (url.endsWith('/status')) return response({ status: 'UP' });
    if (url.endsWith(failure) && failure) throw new TypeError('network');
    if (url.endsWith('/auth/config')) return response({ googleEnabled: enabled });
    if (url.endsWith('/me')) return active ? response({ id }) : response({ error: 'unauthenticated' }, 401);
    if (url.endsWith('/csrf')) return response({ token: 'fresh-csrf', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/auth/logout') && options?.method === 'POST') {
      if (logout === 204 || logout === 401) active = false;
      return response(null, logout);
    }
    throw new Error('unexpected path');
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => { vi.unstubAllGlobals(); window.history.replaceState(null, '', '/'); });

describe('login opcional', () => {
  it.each([true, false])('visitante com Google habilitado=%s não inicia login automaticamente', async enabled => {
    const fetchMock = api({ enabled });
    render(<App />);
    expect(await screen.findByText('Você está estudando como visitante.')).toBeInTheDocument();
    if (enabled) expect(screen.getByRole('link', { name: 'Entrar com Google' })).toHaveAttribute('href', 'http://localhost:8080/oauth2/authorization/google');
    else expect(screen.queryByRole('link', { name: 'Entrar com Google' })).not.toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url]) => url.includes('/oauth2/'))).toBe(false);
    expect(screen.getByRole('link', { name: 'Acessar material de estudo' })).toBeInTheDocument();
    for (const [url, options] of fetchMock.mock.calls) if (!url.endsWith('/status')) expect(options?.credentials).toBe('include');
  });

  it.each(['/me', '/auth/config'])('falha de rede em %s não bloqueia aulas e permite nova tentativa', async failure => {
    api({ failure }); render(<App />);
    expect(await screen.findByText(/Consulta de sessão indisponível/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Acessar material de estudo' })).toBeInTheDocument();
    api(); fireEvent.click(screen.getByRole('button', { name: 'Atualizar sessão' }));
    expect(await screen.findByText('Você está estudando como visitante.')).toBeInTheDocument();
  });

  it('saída consulta CSRF novo e envia POST com credenciais', async () => {
    const fetchMock = api({ connected: true }); render(<App />);
    expect(await screen.findByText('Você está conectado.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));
    expect(await screen.findByText('Você está estudando como visitante.')).toBeInTheDocument();
    const call = fetchMock.mock.calls.find(([url]) => url.endsWith('/auth/logout'));
    expect(call?.[1]).toMatchObject({ method: 'POST', credentials: 'include', headers: { 'X-CSRF-TOKEN': 'fresh-csrf' } });
    expect(localStorage.length).toBe(0); expect(sessionStorage.length).toBe(0);
  });

  it.each([403, 500])('saída %s não anuncia sucesso e preserva aulas', async logout => {
    api({ connected: true, logout }); render(<App />);
    fireEvent.click(await screen.findByRole('button', { name: 'Sair' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(logout === 403 ? /renovar a proteção/ : /sessão pode continuar ativa/);
    expect(screen.getByText('Você está conectado.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Acessar material de estudo' })).toBeInTheDocument();
  });

  it('sessão expirada volta a visitante ao atualizar e saída 401 é tolerada', async () => {
    api({ connected: true, logout: 401 }); render(<App />);
    fireEvent.click(await screen.findByRole('button', { name: 'Sair' }));
    expect(await screen.findByText('Você está estudando como visitante.')).toBeInTheDocument();
    api({ connected: true }); fireEvent.click(screen.getByRole('button', { name: 'Atualizar sessão' }));
    expect(await screen.findByText('Você está conectado.')).toBeInTheDocument();
    api(); fireEvent(window, new Event('focus'));
    await waitFor(() => expect(screen.getByText('Você está estudando como visitante.')).toBeInTheDocument());
  });

  it('consome falha genérica, remove só marcador da URL e permite tentar login', async () => {
    window.history.replaceState(null, '', '/?auth=failed&other=value#section');
    api(); render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent(/Não foi possível entrar com Google/);
    expect(window.location.search).toBe('?other=value'); expect(window.location.hash).toBe('#section');
    expect(await screen.findByRole('link', { name: 'Entrar com Google' })).toBeInTheDocument();
  });
});
