import { afterEach, expect, it, vi } from 'vitest';
import { individualReview, reviewOverview, ReviewApiError } from './subject-review-api';
import { overview, individual } from '../test/subject-review-fixtures';
import { view } from '../test/attempt-fixtures';
const response = (body: unknown, status = 200) => ({ ok: status === 200, status, json: async () => body });
afterEach(() => { vi.unstubAllGlobals();vi.useRealTimers(); });
it('consulta por cargo e tentativa em GET sem CSRF, com credenciais/no-store e contexto exato', async () => {
  const fetch = vi.fn(async (url: string) => response(url.includes('/me/') ? overview() : individual()));vi.stubGlobal('fetch', fetch);
  expect(await reviewOverview(view.roleId, new AbortController().signal)).toEqual(overview());expect(await individualReview(view.id, view.roleId, new AbortController().signal)).toEqual(individual());
  expect(fetch).toHaveBeenCalledTimes(2);for (const [, options] of fetch.mock.calls as unknown as [string, RequestInit][]) { expect(options.credentials).toBe('include');expect(options.cache).toBe('no-store');expect(options.signal).toBeInstanceOf(AbortSignal);expect(options.body).toBeUndefined(); }
});
it('rejeita adulteração de estrutura, contexto, amostra e campos privados em todos os níveis', async () => {
  const mutations = [
    (v: any) => { v.owner = 'private'; }, (v: any) => { v.subjects[0].parameters = {}; }, (v: any) => { v.window.digest = 'private'; },
    (v: any) => { v.subjects.pop(); }, (v: any) => { v.subjects[1].topicId = v.subjects[0].topicId; }, (v: any) => { v.subjects[0].topicId = 'P99'; },
    (v: any) => { v.subjects[0].assessed = 2; }, (v: any) => { v.subjects[0].percentage = null; }, (v: any) => { v.subjects[1].percentage = 0; },
    (v: any) => { v.roleId = 'secretario-de-escola'; }, (v: any) => { v.window.hasOlder = true; }, (v: any) => { v.window.attemptCount = 101; },
    (v: any) => { v.priorityTopicIds = ['P02']; }, (v: any) => { v.window.oldestFinishedAt = '2030-01-01T00:00:00Z'; }, (v: any) => { v.subjects[0].blank = '3'; }, (v: any) => { v.subjects[0].percentage = 33.333; },
  ];
  for (const mutate of mutations) { const body = overview();mutate(body);vi.stubGlobal('fetch', vi.fn(async () => response(body)));await expect(reviewOverview(view.roleId, new AbortController().signal)).rejects.toThrow('Contrato'); }
  vi.stubGlobal('fetch', vi.fn(async () => response({ ...individual(), attemptId: crypto.randomUUID() })));await expect(individualReview(view.id, view.roleId, new AbortController().signal)).rejects.toThrow('Contrato');
});
it.each([401, 403, 404, 409, 500, 503])('preserva status %s mesmo com corpo de erro truncado ou pendente', async status => {
  const json = vi.fn(() => new Promise(() => {}));vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false, status, json })));
  await expect(reviewOverview(view.roleId, new AbortController().signal)).rejects.toEqual(new ReviewApiError(status));expect(json).not.toHaveBeenCalled();
});
it('limita JSON pendente e cancela mesmo se fetch ignorar abort', async () => {
  vi.useFakeTimers();vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, status: 200, json: () => new Promise(() => {}) })));
  const pending = reviewOverview(view.roleId, new AbortController().signal);const check = expect(pending).rejects.toThrow();await vi.advanceTimersByTimeAsync(5001);await check;
  const controller = new AbortController();const aborted = reviewOverview(view.roleId, controller.signal);const rejected = expect(aborted).rejects.toThrow();controller.abort();await rejected;
});
