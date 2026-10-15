import { identityApiUrl } from './identity-api';
import type { Classification, Content, Detail } from './question-api';

export interface AttemptItem extends Detail { id: string; selectedAlternativeId: string | null }
export interface AttemptView { id: string; roleId: string; createdAt: string; finishedAt: string | null; version: number; items: AttemptItem[] }
export interface Composition { roleId: string; disciplines: { disciplineId: string; quantity: number; topicIds: string[] }[] }
export interface Answer { commandId: string; expectedVersion: number; alternativeId: string | null }
export interface Result { total: number; points: number; correct: number; wrong: number; blank: number; percentage: number; items: { item: AttemptItem; correctAlternativeId: string; resolution: Content }[] }
export interface Finished { attempt: AttemptView; result: Result }
export interface HistoryItem { id: string; roleId: string; createdAt: string; finishedAt: string | null; total: number; percentage: number | null }
export interface HistoryPage { items: HistoryItem[]; page: number; size: number }
export class AttemptApiError extends Error {
  constructor(public status: number, public available: { disciplineId: string; available: number }[] = []) { super('Não foi possível confirmar a operação.'); }
}
type Row = Record<string, unknown>;
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
function ensure(ok: unknown): asserts ok { if (!ok) throw new Error('Contrato de tentativa inválido'); }
function row(value: unknown, keys: string[]): Row {
  ensure(value && typeof value === 'object' && !Array.isArray(value)); const r = value as Row;
  ensure(Object.keys(r).length === keys.length && keys.every(k => k in r)); return r;
}
function text(v: unknown): asserts v is string { ensure(typeof v === 'string' && !!v.trim()); }
function id(v: unknown) { text(v); ensure(uuid.test(v)); }
function date(v: unknown) { text(v); ensure(Number.isFinite(Date.parse(v))); }
function number(v: unknown) { ensure(Number.isSafeInteger(v) && Number(v) >= 0); }
function list(v: unknown): asserts v is unknown[] { ensure(Array.isArray(v)); }
function content(v: unknown) {
  const r = row(v, ['text', 'resources']); text(r.text); list(r.resources);
  r.resources.forEach(resource => { const a = row(resource, ['path', 'mediaType', 'sha256', 'description']); Object.values(a).forEach(text); ensure(String(a.path).startsWith('assets/')); });
}
function item(v: unknown) {
  const r = row(v, ['id', 'questionId', 'revisionId', 'classification', 'statement', 'alternatives', 'references', 'selectedAlternativeId']);
  id(r.id); id(r.questionId); id(r.revisionId); content(r.statement);
  const c = row(r.classification, ['disciplineId', 'topicId', 'roleIds']); text(c.disciplineId); text(c.topicId); list(c.roleIds); c.roleIds.forEach(text);
  list(r.alternatives); ensure(r.alternatives.length >= 2); const ids: unknown[] = [];
  r.alternatives.forEach(o => { const a = row(o, ['id', 'content']); id(a.id); content(a.content); ids.push(a.id); });
  ensure(new Set(ids).size === ids.length && (r.selectedAlternativeId === null || ids.includes(r.selectedAlternativeId)));
  list(r.references); r.references.forEach(v => { const a = row(v, ['name', 'url']); text(a.name); text(a.url); const u = new URL(a.url); ensure(['http:', 'https:'].includes(u.protocol) && !u.username && !u.password); });
}
export function validateAttempt(v: unknown): asserts v is AttemptView {
  const r = row(v, ['id', 'roleId', 'createdAt', 'finishedAt', 'version', 'items']); id(r.id); text(r.roleId); date(r.createdAt); if (r.finishedAt !== null) date(r.finishedAt); number(r.version);
  list(r.items); ensure(r.items.length >= 1 && r.items.length <= 100); r.items.forEach(item);
  ensure(new Set(r.items.map(i => (i as AttemptItem).id)).size === r.items.length && new Set(r.items.map(i => (i as AttemptItem).questionId)).size === r.items.length);
}
function result(v: unknown): asserts v is Result {
  const r = row(v, ['total', 'points', 'correct', 'wrong', 'blank', 'percentage', 'items']);
  ['total', 'points', 'correct', 'wrong', 'blank'].forEach(k => number(r[k])); ensure(typeof r.percentage === 'number' && r.percentage >= 0 && r.percentage <= 100);
  ensure(Number(r.total) === Number(r.correct) + Number(r.wrong) + Number(r.blank) && r.points === r.correct);
  list(r.items); ensure(r.items.length === r.total); r.items.forEach(v => {
    const a = row(v, ['item', 'correctAlternativeId', 'resolution']); item(a.item); id(a.correctAlternativeId); content(a.resolution);
    ensure((a.item as AttemptItem).alternatives.some(o => o.id === a.correctAlternativeId));
  });
}
function history(v: unknown): asserts v is HistoryPage {
  const r = row(v, ['items', 'page', 'size']); number(r.page); number(r.size); ensure(Number(r.size) >= 1 && Number(r.size) <= 100); list(r.items);
  r.items.forEach(v => { const e = row(v, ['id', 'roleId', 'createdAt', 'finishedAt', 'total', 'percentage']); id(e.id); text(e.roleId); date(e.createdAt); number(e.total);
    if (e.finishedAt === null) ensure(e.percentage === null); else { date(e.finishedAt); ensure(typeof e.percentage === 'number' && e.percentage >= 0 && e.percentage <= 100); }
  });
}
async function request<T>(path: string, signal: AbortSignal, validate: (v: unknown) => void, method = 'GET', payload?: unknown): Promise<T> {
  const controller = new AbortController(); const abort = () => controller.abort(); signal.addEventListener('abort', abort, { once: true }); if (signal.aborted) abort();
  const timer = window.setTimeout(abort, 5000); let rejectAbort!: () => void;
  const aborted = new Promise<never>((_, reject) => { rejectAbort = () => reject(new DOMException('Operação interrompida', 'AbortError')); controller.signal.addEventListener('abort', rejectAbort, { once: true }); if (controller.signal.aborted) rejectAbort(); });
  const fetchJson = async (url: string, options: RequestInit = {}) => {
    controller.signal.throwIfAborted();
    const response = await fetch(identityApiUrl + url, { ...options, credentials: 'include', cache: 'no-store', signal: controller.signal });
    const body: unknown = await response.json(); controller.signal.throwIfAborted();
    if (!response.ok) {
      const available = typeof body === 'object' && body && 'available' in body && Array.isArray(body.available) ? body.available.filter((a): a is { disciplineId: string; available: number } => !!a && typeof a.disciplineId === 'string' && Number.isSafeInteger(a.available) && a.available >= 0) : [];
      throw new AttemptApiError(response.status, available);
    }
    return body;
  };
  try {
    return await Promise.race([aborted, (async () => {
      const headers: Record<string, string> = {};
      if (method !== 'GET') { const csrf = row(await fetchJson('/api/v1/csrf'), ['token', 'headerName']); text(csrf.token); ensure(csrf.headerName === 'X-CSRF-TOKEN'); headers[csrf.headerName] = csrf.token; headers['Content-Type'] = 'application/json'; }
      const body = await fetchJson(path, { method, headers, ...(payload === undefined ? {} : { body: JSON.stringify(payload) }) });
      validate(body); return body as T;
    })()]);
  } finally { window.clearTimeout(timer); signal.removeEventListener('abort', abort); controller.signal.removeEventListener('abort', rejectAbort); }
}
const attemptPath = (id: string) => { ensure(uuid.test(id)); return `/api/v1/attempts/${id}`; };
export const createAttempt = (composition: Composition, commandId: string, signal: AbortSignal) => request<AttemptView>('/api/v1/attempts', signal, validateAttempt, 'POST', { ...composition, commandId });
export const readAttempt = (id: string, signal: AbortSignal) => request<AttemptView>(attemptPath(id), signal, v => { validateAttempt(v); ensure(v.id === id); });
export const saveAnswer = (id: string, itemId: string, answer: Answer, signal: AbortSignal) => request<AttemptView>(`${attemptPath(id)}/answers/${encodeURIComponent(itemId)}`, signal, v => { validateAttempt(v); ensure(v.id === id && v.version === answer.expectedVersion + 1 && v.finishedAt === null && v.items.some(i => i.id === itemId && i.selectedAlternativeId === answer.alternativeId)); }, 'PUT', answer);
export const finishAttempt = (id: string, commandId: string, expectedVersion: number, signal: AbortSignal) => request<Finished>(`${attemptPath(id)}/finish`, signal, v => { const r = row(v, ['attempt', 'result']); validateAttempt(r.attempt); ensure(r.attempt.id === id && r.attempt.finishedAt !== null && r.attempt.version === expectedVersion + 1); result(r.result); const attempt = r.attempt; ensure(r.result.total === attempt.items.length && r.result.items.every((c, i) => c.item.id === attempt.items[i].id)); }, 'POST', { commandId, expectedVersion });
export const attemptResult = (id: string, signal: AbortSignal) => request<Result>(`${attemptPath(id)}/result`, signal, result);
export const attemptHistory = (page: number, signal: AbortSignal) => request<HistoryPage>(`/api/v1/me/attempts?page=${page}&size=20`, signal, v => { history(v); ensure(v.page === page && v.size === 20); });
