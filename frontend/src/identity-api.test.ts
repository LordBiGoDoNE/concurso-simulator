import { afterEach, expect, it, vi } from 'vitest';
import { currentIdentity, identityRequest } from './identity-api';

const id = '12345678-1234-1234-1234-123456789abc';
const response = (body: unknown, status = 200) => ({ ok: status >= 200 && status < 300, status, json: vi.fn(async () => body) });
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });

it.each(['/auth/config', '/me', '/csrf'])('aborta %s após headers imediatos e JSON pendente', async stalled => {
  vi.useFakeTimers();
  let signal!: AbortSignal;
  let finishBody!: (body: unknown) => void;
  const json = vi.fn(() => new Promise(resolve => { finishBody = resolve; }));
  vi.stubGlobal('fetch', vi.fn(async (url: string, options: RequestInit) => {
    if (url.endsWith(stalled)) {
      signal = options.signal as AbortSignal;
      return { ok: true, status: 200, json };
    }
    return url.endsWith('/auth/config') ? response({ googleEnabled: true }) : response({ id });
  }));
  const request = stalled === '/csrf' ? identityRequest('/api/v1/csrf') : currentIdentity();
  const rejected = expect(request).rejects.toMatchObject({ name: 'AbortError' });
  await vi.advanceTimersByTimeAsync(4999);
  expect(json).toHaveBeenCalledOnce();
  expect(signal.aborted).toBe(false);
  await vi.advanceTimersByTimeAsync(1);
  await rejected;
  expect(signal.aborted).toBe(true);
  expect(vi.getTimerCount()).toBe(0);
  // Mesmo um leitor que ignore abort não pode manter a chamada pendente nem revertê-la.
  finishBody(stalled === '/auth/config' ? { googleEnabled: true } : { id });
  await Promise.resolve();
});

it('usa um único prazo para headers e JSON, sem reiniciar ao receber headers', async () => {
  vi.useFakeTimers();
  let headers!: (value: unknown) => void;
  let signal!: AbortSignal;
  const json = vi.fn(() => new Promise(() => {}));
  vi.stubGlobal('fetch', vi.fn((_url, options: RequestInit) => {
    signal = options.signal as AbortSignal;
    return new Promise(resolve => { headers = resolve; });
  }));
  const rejected = expect(identityRequest('/api/v1/csrf')).rejects.toMatchObject({ name: 'AbortError' });
  await vi.advanceTimersByTimeAsync(3000);
  headers({ ok: true, status: 200, json });
  await vi.advanceTimersByTimeAsync(0);
  expect(json).toHaveBeenCalledOnce();
  await vi.advanceTimersByTimeAsync(2000);
  await rejected;
  expect(signal.aborted).toBe(true);
  expect(vi.getTimerCount()).toBe(0);
});

it.each(['headers', 'body', 'already-aborted'])('respeita abort externo em %s e libera listeners/timer', async phase => {
  vi.useFakeTimers();
  const controller = new AbortController();
  const remove = vi.spyOn(controller.signal, 'removeEventListener');
  let signal: AbortSignal | undefined;
  const json = vi.fn(() => new Promise(() => {}));
  const fetchMock = vi.fn((_url, options: RequestInit) => {
    signal = options.signal as AbortSignal;
    return phase === 'headers' ? new Promise(() => {}) : Promise.resolve({ ok: true, status: 200, json });
  });
  vi.stubGlobal('fetch', fetchMock);
  if (phase === 'already-aborted') controller.abort();
  const rejected = expect(identityRequest('/api/v1/csrf', { signal: controller.signal })).rejects.toMatchObject({ name: 'AbortError' });
  await vi.advanceTimersByTimeAsync(0);
  controller.abort();
  await rejected;
  if (phase === 'already-aborted') expect(fetchMock).not.toHaveBeenCalled();
  else expect(signal?.aborted).toBe(true);
  if (phase === 'body') expect(json).toHaveBeenCalledOnce();
  expect(remove).toHaveBeenCalledWith('abort', expect.any(Function));
  expect(vi.getTimerCount()).toBe(0);
});

it.each(['success', 'network', 'json'])('libera prazo/listeners após %s e mantém opções seguras', async outcome => {
  vi.useFakeTimers();
  const controller = new AbortController();
  const remove = vi.spyOn(controller.signal, 'removeEventListener');
  const fetchMock = vi.fn(async () => {
    if (outcome === 'network') throw new TypeError('network');
    return { ok: true, status: 200, json: async () => {
      if (outcome === 'json') throw new SyntaxError('invalid JSON');
      return { googleEnabled: false };
    } };
  });
  vi.stubGlobal('fetch', fetchMock);
  const request = identityRequest('/api/v1/auth/config', { signal: controller.signal, credentials: 'omit', cache: 'force-cache' });
  if (outcome === 'success') await expect(request).resolves.toMatchObject({ body: { googleEnabled: false } });
  else await expect(request).rejects.toThrow();
  expect(fetchMock).toHaveBeenCalledWith(expect.any(String), expect.objectContaining({ credentials: 'include', cache: 'no-store' }));
  expect(remove).toHaveBeenCalledWith('abort', expect.any(Function));
  expect(vi.getTimerCount()).toBe(0);
});

it('me 401 sem corpo retorna visitante sem tentar ler JSON', async () => {
  const me = response(undefined, 401);
  vi.stubGlobal('fetch', vi.fn(async (url: string) => url.endsWith('/me') ? me : response({ googleEnabled: false })));
  await expect(currentIdentity()).resolves.toEqual({ connected: false, googleEnabled: false });
  expect(me.json).not.toHaveBeenCalled();
});

it.each([204, 401, 403, 500])('logout %s usa status sem tentar ler JSON', async status => {
  const logout = response(undefined, status);
  vi.stubGlobal('fetch', vi.fn(async () => logout));
  await expect(identityRequest('/api/v1/auth/logout', { method: 'POST' }, false)).resolves.toMatchObject({ status });
  expect(logout.json).not.toHaveBeenCalled();
});

it.each([
  [{ googleEnabled: 'true' }, { id }],
  [null, { id }],
  [{ googleEnabled: true }, { id: 'not-a-uuid' }],
  [{ googleEnabled: true }, null],
])('rejeita contratos inválidos de config/me (%j, %j)', async (config, me) => {
  vi.stubGlobal('fetch', vi.fn(async (url: string) => response(url.endsWith('/me') ? me : config)));
  await expect(currentIdentity()).rejects.toThrow(/invalid/);
});
