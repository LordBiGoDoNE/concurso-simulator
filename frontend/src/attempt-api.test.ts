import { afterEach, expect, it, vi } from 'vitest';
import { attemptHistory, createAttempt, finishAttempt, readAttempt, validateAttempt } from './attempt-api';
import { view, result } from '../test/attempt-fixtures';
import { response } from '../test/question-fixtures';
const signal = () => new AbortController().signal;
const composition = { roleId: 'assistente-administrativo', disciplines: [{ disciplineId: 'portugues', quantity: 1, topicIds: [] }] };
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });
it('envia CSRF/credenciais/no-store e preserva payload e commandId no retry', async () => {
  const fetchMock = vi.fn(async (url: string) => url.endsWith('/csrf') ? response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' }) : response(view));
  vi.stubGlobal('fetch', fetchMock);
  await createAttempt(composition, 'operation', signal()); await createAttempt(composition, 'operation', signal());
  const writes = fetchMock.mock.calls.filter(([url]) => !url.endsWith('/csrf')) as unknown as [string, RequestInit][];
  expect(writes).toHaveLength(2); expect(writes[0][1].body).toEqual(writes[1][1].body);
  expect(writes[0][1]).toMatchObject({ credentials: 'include', cache: 'no-store', method: 'POST', headers: { 'X-CSRF-TOKEN': 'csrf-test', 'Content-Type': 'application/json' } });
});
it.each(['csrf-body', 'mutation-body', 'headers'])('prazo único encerra %s sem depender de fetch obedecer abort', async stalled => {
  vi.useFakeTimers(); let finish!: (v: unknown) => void; let captured!: AbortSignal;
  vi.stubGlobal('fetch', vi.fn(async (url: string, options: RequestInit) => {
    captured = options.signal as AbortSignal;
    if (stalled === 'headers') return new Promise(() => {});
    if (url.endsWith('/csrf') && stalled !== 'csrf-body') return response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' });
    return { ok: true, status: 200, json: () => new Promise(resolve => { finish = resolve; }) };
  }));
  const rejected = expect(createAttempt(composition, 'operation', signal())).rejects.toMatchObject({ name: 'AbortError' });
  await vi.advanceTimersByTimeAsync(5000); await rejected; expect(captured.aborted).toBe(true); expect(vi.getTimerCount()).toBe(0);
  finish?.(view); await Promise.resolve();
});
it('nega propriedades corretivas em qualquer item e detecção negativa não depende do DOM', () => {
  expect(() => validateAttempt(view)).not.toThrow();
  expect(() => validateAttempt({ ...view, correctAlternativeId: 'leak' })).toThrow();
  expect(() => validateAttempt({ ...view, items: [{ ...view.items[0], resolution: result.items[0].resolution }] })).toThrow();
  expect(() => validateAttempt({ ...view, items: [view.items[0], view.items[0]] })).toThrow();
});
it('aceita correção apenas no contrato fechado e recusa nota parcial no histórico', async () => {
  vi.stubGlobal('fetch', vi.fn(async (url: string) => url.endsWith('/csrf') ? response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' }) : response({ attempt: view, result })));
  await expect(finishAttempt(view.id, 'operation', 0, signal())).rejects.toThrow();
  vi.stubGlobal('fetch', vi.fn(async () => response({ items: [{ id: view.id, roleId: view.roleId, createdAt: view.createdAt, finishedAt: null, total: 1, percentage: 100 }], page: 0, size: 20 })));
  await expect(attemptHistory(0, signal())).rejects.toThrow();
});
it('abort externo recusa resposta tardia e libera listener', async () => {
  const c = new AbortController(); let finish!: (v: unknown) => void;
  vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, status: 200, json: () => new Promise(resolve => { finish = resolve; }) })));
  const pending = readAttempt(view.id, c.signal); const rejected = expect(pending).rejects.toMatchObject({ name: 'AbortError' });
  await Promise.resolve(); await Promise.resolve(); c.abort(); await rejected; finish(view); await Promise.resolve();
});
