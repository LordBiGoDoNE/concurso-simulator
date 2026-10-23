import { afterEach, expect, it, vi } from 'vitest';
import { attemptHistory, attemptResult, createAttempt, finishAttempt, readAttempt, validateAttempt } from './attempt-api';
import { view, result } from '../test/attempt-fixtures';
import { response } from '../test/question-fixtures';
const signal = () => new AbortController().signal;
const composition = { roleId: 'assistente-administrativo', disciplines: [{ disciplineId: 'portugues', quantity: 1, topicIds: [] }] };

it('valida contrato corretivo tardio e projeta apresentação sem recalcular a nota', async () => {
  const wire = { ...result, items: [{ item: view.items[0], correctAlternativeId: view.items[0].alternatives[0].id, resolution: result.items[0].explanation }] };
  const closed = { ...view, finishedAt: '2026-10-14T12:05:00Z', version: 1 };
  vi.stubGlobal('fetch', vi.fn(async (url: string) => url.endsWith('/csrf') ? response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' }) : response(url.endsWith('/finish') ? { attempt: closed, result: wire } : wire)));
  expect(await attemptResult(view.id, signal())).toEqual(result);
  expect(await finishAttempt(view.id, 'operation', 0, signal())).toEqual({ attempt: closed, result });
});

it.each([401, 403, 404])('invalida acesso %s pelos headers sem consumir corpo truncado ou pendente', async status => {
  for (const json of [vi.fn().mockRejectedValue(new SyntaxError('truncated')), vi.fn(() => new Promise(() => {}))]) {
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false, status, json })));
    await expect(readAttempt(view.id, signal())).rejects.toMatchObject({ status });
    expect(json).not.toHaveBeenCalled();
  }
});
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });
it('envia CSRF/credenciais/no-store e preserva payload e commandId no retry', async () => {
  const fetchMock = vi.fn(async (url: string) => url.endsWith('/csrf') ? response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' }) : response(view));
  vi.stubGlobal('fetch', fetchMock);
  await createAttempt(composition, 'operation', signal()); await createAttempt(composition, 'operation', signal());
  const writes = fetchMock.mock.calls.filter(([url]) => !url.endsWith('/csrf')) as unknown as [string, RequestInit][];
  expect(writes).toHaveLength(2); expect(writes[0][1].body).toEqual(writes[1][1].body);
  expect(writes[0][1]).toMatchObject({ credentials: 'include', cache: 'no-store', method: 'POST', headers: { 'X-CSRF-TOKEN': 'csrf-test', 'Content-Type': 'application/json' } });
});
it('envia somente a opção pública e rejeita metadados privados em qualquer item', async () => {
  const fetchMock = vi.fn(async (url: string) => url.endsWith('/csrf') ? response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' }) : response(view));
  vi.stubGlobal('fetch', fetchMock);await createAttempt({ ...composition, useParameterizedQuestions: true }, 'operation', signal());
  const request = fetchMock.mock.calls.find(([url]) => url.endsWith('/attempts')) as unknown as [string, RequestInit];
  expect(JSON.parse(String(request[1].body))).toEqual({ ...composition, useParameterizedQuestions: true, commandId: 'operation' });
  for (const key of ['source', 'templateId', 'templateVersion', 'parameters', 'definitionDigest', 'manifestDigest', 'outputDigest', 'generatorVersion', 'seed']) {
    expect(() => validateAttempt({ ...view, items: [{ ...view.items[0], [key]: 'PRIVATE_PARAMETERIZED_LEAK' }] })).toThrow();
  }
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
  expect(() => validateAttempt({ ...view, items: [{ ...view.items[0], resolution: result.items[0].explanation }] })).toThrow();
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
