import { afterEach, expect, it, vi } from 'vitest';
import { questionCatalog, questionDetail, questionList } from './question-api';
import { catalog, detail, page, questionId, response } from '../test/question-fixtures';
const filters = { disciplineId: 'portugues', topicId: 'P01', roleId: 'secretario-de-escola', page: 0 };
afterEach(() => { vi.useRealTimers(); vi.unstubAllGlobals(); vi.restoreAllMocks(); });

it.each(['headers', 'json'])('o prazo total cobre %s mesmo se transporte ignorar abort', async phase => {
  vi.useFakeTimers(); let signal!: AbortSignal;
  let finish!: (value: unknown) => void;
  vi.stubGlobal('fetch', vi.fn((_url: string, options: RequestInit) => {
    signal = options.signal!;
    return phase === 'headers' ? new Promise(resolve => { finish = resolve; }) : Promise.resolve({ ok: true, json: () => new Promise(resolve => { finish = resolve; }) });
  }));
  const promise = questionCatalog(new AbortController().signal);
  const rejected = expect(promise).rejects.toMatchObject({ name: 'AbortError' });
  await vi.advanceTimersByTimeAsync(4999); expect(signal.aborted).toBe(false);
  await vi.advanceTimersByTimeAsync(1); await rejected; expect(signal.aborted).toBe(true); expect(vi.getTimerCount()).toBe(0);
  finish(phase === 'headers' ? response(catalog) : catalog); await Promise.resolve();
});
it('headers tardios não reiniciam o prazo até terminar JSON', async () => {
  vi.useFakeTimers(); let headers!: (value: unknown) => void;
  vi.stubGlobal('fetch', vi.fn(() => new Promise(resolve => { headers = resolve; })));
  const rejected = expect(questionCatalog(new AbortController().signal)).rejects.toMatchObject({ name: 'AbortError' });
  await vi.advanceTimersByTimeAsync(3000); headers({ ok: true, json: () => new Promise(() => {}) });
  await vi.advanceTimersByTimeAsync(2000); await rejected; expect(vi.getTimerCount()).toBe(0);
});
it.each([false, true])('abort externo/pré-abortado=%s libera listener e prazo', async pre => {
  vi.useFakeTimers(); const controller = new AbortController(); const remove = vi.spyOn(controller.signal, 'removeEventListener');
  const fetch = vi.fn(() => new Promise(() => {})); vi.stubGlobal('fetch', fetch);
  if (pre) controller.abort(); const rejected = expect(questionCatalog(controller.signal)).rejects.toMatchObject({ name: 'AbortError' });
  controller.abort(); await rejected; if (pre) expect(fetch).not.toHaveBeenCalled();
  expect(remove).toHaveBeenCalledWith('abort', expect.any(Function)); expect(vi.getTimerCount()).toBe(0);
});
it('filtros AND são codificados e consulta usa GET/omit/no-store, sem storage', async () => {
  const fetch = vi.fn(async () => response(page)); vi.stubGlobal('fetch', fetch);
  await expect(questionList(filters, new AbortController().signal)).resolves.toEqual(page);
  expect(fetch).toHaveBeenCalledWith(expect.stringContaining('disciplineId=portugues&topicId=P01&roleId=secretario-de-escola'), expect.objectContaining({ method: 'GET', credentials: 'omit', cache: 'no-store' }));
  expect(localStorage.length).toBe(0); expect(sessionStorage.length).toBe(0);
});
it.each([400, 404, 503])('erro HTTP %s não vira acervo vazio', async status => {
  const reply = response(undefined, status); const json = vi.spyOn(reply, 'json');vi.stubGlobal('fetch', vi.fn(async () => reply));
  await expect(questionDetail(questionId, new AbortController().signal)).rejects.toThrow(); expect(json).not.toHaveBeenCalled();
});
it.each([{ ...detail, correctAlternativeId: questionId }, { ...detail, resolution: 'privado' }, { ...detail, references: [{ name: 'Insegura', url: 'javascript:alert(1)' }] }])('rejeita contrato extra/corretivo/URL insegura', async body => {
  vi.stubGlobal('fetch', vi.fn(async () => response(body)));
  await expect(questionDetail(questionId, new AbortController().signal)).rejects.toThrow();
});
