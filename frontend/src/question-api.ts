export interface Named { id: string; title: string }
export interface Topic extends Named { disciplineId: string }
export interface Catalog { contestId: string; disciplines: Named[]; topics: Topic[]; roles: Named[] }
export interface Classification { disciplineId: string; topicId: string; roleIds: string[] }
export interface Resource { path: string; mediaType: string; sha256: string; description: string }
export interface Content { text: string; resources: Resource[] }
export interface Summary { questionId: string; revisionId: string; classification: Classification; statementStart: string }
export interface QuestionPage { items: Summary[]; page: number; size: number; total: number }
export interface Detail { questionId: string; revisionId: string; classification: Classification; statement: Content; alternatives: { id: string; content: Content }[]; references: { name: string; url: string }[] }
export interface Filters { disciplineId: string; topicId: string; roleId: string; page: number }

const api = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');
const configured = Number(import.meta.env.VITE_QUESTION_TIMEOUT_MS || 5000);
const timeout = Number.isFinite(configured) && configured > 0 ? configured : 5000;
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
type Row = Record<string, unknown>;
function row(value: unknown, keys: string[]): Row {
  if (!value || typeof value !== 'object' || Array.isArray(value) || Object.keys(value).length !== keys.length || keys.some(k => !(k in value))) throw new Error('Contrato de questões inválido');
  return value as Row;
}
function string(value: unknown): asserts value is string { if (typeof value !== 'string' || !value.trim()) throw new Error('Texto inválido'); }
function list(value: unknown): asserts value is unknown[] { if (!Array.isArray(value)) throw new Error('Lista inválida'); }
function id(value: unknown) { string(value); if (!uuid.test(value)) throw new Error('UUID inválido'); }
function named(value: unknown) { const r = row(value, ['id', 'title']); string(r.id); string(r.title); }
function classification(value: unknown) { const r = row(value, ['disciplineId', 'topicId', 'roleIds']); string(r.disciplineId); string(r.topicId); list(r.roleIds); r.roleIds.forEach(string); }
function content(value: unknown) {
  const r = row(value, ['text', 'resources']); string(r.text); list(r.resources);
  r.resources.forEach(v => { const a = row(v, ['path', 'mediaType', 'sha256', 'description']); Object.values(a).forEach(string); });
}
function summary(value: unknown) { const r = row(value, ['questionId', 'revisionId', 'classification', 'statementStart']); id(r.questionId); id(r.revisionId); classification(r.classification); string(r.statementStart); }

async function request<T>(path: string, signal: AbortSignal, validate: (body: unknown) => void): Promise<T> {
  const controller = new AbortController();
  const abort = () => controller.abort();
  signal.addEventListener('abort', abort, { once: true });
  if (signal.aborted) controller.abort();
  const timer = window.setTimeout(abort, timeout);
  let rejectAbort!: (error: unknown) => void;
  const aborted = new Promise<never>((_resolve, reject) => {
    rejectAbort = () => reject(new DOMException('Consulta interrompida', 'AbortError'));
    controller.signal.addEventListener('abort', rejectAbort, { once: true });
    if (controller.signal.aborted) rejectAbort(undefined);
  });
  try {
    return await Promise.race([aborted, (async () => {
      controller.signal.throwIfAborted();
      const response = await fetch(`${api}/api/v1/question-bank${path}`, { method: 'GET', credentials: 'omit', cache: 'no-store', signal: controller.signal });
      if (!response.ok) throw new Error('Consulta de questões indisponível');
      const body: unknown = await response.json();
      controller.signal.throwIfAborted(); validate(body); return body as T;
    })()]);
  } finally {
    window.clearTimeout(timer); signal.removeEventListener('abort', abort); controller.signal.removeEventListener('abort', rejectAbort);
  }
}
export function questionCatalog(signal: AbortSignal) {
  return request<Catalog>('/catalog', signal, body => {
    const r = row(body, ['contestId', 'disciplines', 'topics', 'roles']); string(r.contestId);
    list(r.disciplines); r.disciplines.forEach(named); list(r.roles); r.roles.forEach(named); list(r.topics);
    r.topics.forEach(t => { const v = row(t, ['id', 'title', 'disciplineId']); string(v.id); string(v.title); string(v.disciplineId); });
  });
}
export function questionList(filters: Filters, signal: AbortSignal) {
  const query = new URLSearchParams({ page: String(filters.page), size: '20' });
  for (const key of ['disciplineId', 'topicId', 'roleId'] as const) if (filters[key]) query.set(key, filters[key]);
  return request<QuestionPage>(`/questions?${query}`, signal, body => {
    const r = row(body, ['items', 'page', 'size', 'total']); list(r.items); r.items.forEach(summary);
    if (![r.page, r.size, r.total].every(v => Number.isSafeInteger(v) && Number(v) >= 0) || r.size !== 20 || r.page !== filters.page) throw new Error('Página inválida');
  });
}
export function questionDetail(questionId: string, signal: AbortSignal) {
  return request<Detail>(`/questions/${encodeURIComponent(questionId)}`, signal, body => {
    const r = row(body, ['questionId', 'revisionId', 'classification', 'statement', 'alternatives', 'references']); id(r.questionId); id(r.revisionId);
    if (r.questionId !== questionId) throw new Error('Questão divergente');
    classification(r.classification); content(r.statement); list(r.alternatives); list(r.references);
    if (r.alternatives.length < 2 || !r.references.length) throw new Error('Questão incompleta');
    r.alternatives.forEach(o => { const v = row(o, ['id', 'content']); id(v.id); content(v.content); });
    r.references.forEach(ref => {
      const v = row(ref, ['name', 'url']); string(v.name); string(v.url); const url = new URL(v.url);
      if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) throw new Error('URL inválida');
    });
  });
}
