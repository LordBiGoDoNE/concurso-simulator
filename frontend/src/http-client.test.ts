import { afterEach, expect, it, vi } from 'vitest';
import { positiveTimeout, withAbortScope } from './http-client';

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
  vi.resetModules();
});

const never = () => new Promise<never>(() => undefined);

it('aceita só timeout numérico positivo e usa o padrão nos demais casos', () => {
  expect(positiveTimeout('2500', 5000)).toBe(2500);
  expect(positiveTimeout(undefined, 5000)).toBe(5000);
  expect(positiveTimeout('', 5000)).toBe(5000);
  expect(positiveTimeout('0', 5000)).toBe(5000);
  expect(positiveTimeout('-1', 5000)).toBe(5000);
  expect(positiveTimeout('abc', 5000)).toBe(5000);
});

it('resolve a URL base uma vez, sem barra final', async () => {
  vi.stubEnv('VITE_API_URL', 'http://api.example.test/');
  const client = await import('./http-client');
  expect(client.apiBaseUrl).toBe('http://api.example.test');
  const fetchMock = vi.fn(async () => new Response('{}'));
  vi.stubGlobal('fetch', fetchMock);
  await client.apiFetch('/api/v1/status', {});
  expect(fetchMock).toHaveBeenCalledWith('http://api.example.test/api/v1/status', {});
});

it('rejeita com AbortError no timeout mesmo que o trabalho ignore o sinal, e libera o timer', async () => {
  vi.useFakeTimers();
  const pending = withAbortScope(undefined, 1000, 'Tempo esgotado', never);
  const outcome = expect(pending).rejects.toMatchObject({ name: 'AbortError', message: 'Tempo esgotado' });
  await vi.advanceTimersByTimeAsync(1000);
  await outcome;
  expect(vi.getTimerCount()).toBe(0);
});

it('não inicia o trabalho quando o sinal externo já foi abortado', async () => {
  const external = new AbortController(); external.abort();
  const work = vi.fn(async () => 'never');
  await expect(withAbortScope(external.signal, 1000, 'Interrompida', work)).rejects.toMatchObject({ name: 'AbortError' });
  expect(work).not.toHaveBeenCalled();
});

it('propaga o abort externo para o sinal do trabalho durante a requisição', async () => {
  const external = new AbortController();
  let inner: AbortSignal | undefined;
  const pending = withAbortScope(external.signal, 10000, 'Interrompida', signal => { inner = signal; return never(); });
  await Promise.resolve();
  external.abort();
  await expect(pending).rejects.toMatchObject({ name: 'AbortError', message: 'Interrompida' });
  expect(inner?.aborted).toBe(true);
});

it('não entrega dados quando o abort chega depois do corpo e antes da validação', async () => {
  const external = new AbortController();
  const validate = vi.fn();
  const pending = withAbortScope(external.signal, 10000, 'Interrompida', async signal => {
    const body = await Promise.resolve({ ok: true });
    external.abort();
    signal.throwIfAborted();
    validate(body);
    return body;
  });
  await expect(pending).rejects.toMatchObject({ name: 'AbortError' });
  expect(validate).not.toHaveBeenCalled();
});

it('remove o ouvinte do sinal externo e o timer ao terminar', async () => {
  vi.useFakeTimers();
  const external = new AbortController();
  const remove = vi.spyOn(external.signal, 'removeEventListener');
  await expect(withAbortScope(external.signal, 1000, 'Interrompida', async () => 'ok')).resolves.toBe('ok');
  expect(remove).toHaveBeenCalledWith('abort', expect.any(Function));
  expect(vi.getTimerCount()).toBe(0);
});
