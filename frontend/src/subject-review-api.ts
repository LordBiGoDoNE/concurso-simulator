import { identityRequest } from './identity-api';

export interface SubjectMetric { topicId: string; title: string; disciplineId: string; disciplineTitle: string; correct: number; wrong: number; blank: number; assessed: number; exposures: number; percentage: number | null; lastFinishedAt: string | null }
export interface ReviewSummary { policy: 'subject-review/v1'; roleId: string; minimumBases: 3; subjects: SubjectMetric[]; priorityTopicIds: string[] }
export interface ReviewOverview extends ReviewSummary { window: { limit: 100; attemptCount: number; hasOlder: boolean; latestFinishedAt: string | null; oldestFinishedAt: string | null } }
export interface IndividualReview extends ReviewSummary { attemptId: string }
export class ReviewApiError extends Error { constructor(public status: number) { super('Consulta de revisão indisponível.'); } }
export const reviewRoles = [{ id: 'assistente-administrativo', title: 'Assistente Administrativo' }, { id: 'secretario-de-escola', title: 'Secretário de Escola' }];
const topicIds = ['P:10', 'M:12', 'I:10', 'A:8', 'S:11'].flatMap(group => { const [prefix, count] = group.split(':');return Array.from({ length: Number(count) }, (_, i) => prefix + String(i + 1).padStart(2, '0')); });
function ensure(ok: unknown): asserts ok { if (!ok) throw new Error('Contrato de revisão inválido'); }
function row(v: unknown, keys: string[]): Record<string, unknown> { ensure(v && typeof v === 'object' && !Array.isArray(v));ensure(Object.keys(v).length === keys.length && keys.every(k => Object.hasOwn(v, k)));return v as Record<string, unknown>; }
function text(v: unknown): asserts v is string { ensure(typeof v === 'string' && v.trim().length > 0); }
function count(v: unknown) { ensure(Number.isSafeInteger(v) && Number(v) >= 0 && Number(v) <= 10000); }
function date(v: unknown) { ensure(typeof v === 'string' && /^\d{4}-\d\d-\d\dT/.test(v) && Number.isFinite(Date.parse(v))); }
function summary(v: unknown, context: 'window' | 'attemptId', roleId: string) {
  const r = row(v, ['policy', 'roleId', 'minimumBases', context, 'subjects', 'priorityTopicIds']);
  ensure(r.policy === 'subject-review/v1' && r.roleId === roleId && r.minimumBases === 3);
  ensure(Array.isArray(r.subjects) && r.subjects.length === 51);const ids = new Set<string>();
  for (const value of r.subjects) {
    const m = row(value, ['topicId', 'title', 'disciplineId', 'disciplineTitle', 'correct', 'wrong', 'blank', 'assessed', 'exposures', 'percentage', 'lastFinishedAt']);
    text(m.topicId);ensure(topicIds.includes(m.topicId) && !ids.has(m.topicId));ids.add(m.topicId);text(m.title);text(m.disciplineTitle);
    ensure(['portugues', 'matematica-raciocinio', 'informatica', 'conhecimentos-administrativos', 'conhecimentos-escolares'].includes(String(m.disciplineId)));
    ['correct', 'wrong', 'blank', 'assessed', 'exposures'].forEach(k => count(m[k]));
    ensure(m.assessed === Number(m.correct) + Number(m.wrong) + Number(m.blank) && Number(m.exposures) >= Number(m.assessed));
    if (m.assessed === 0) ensure(m.percentage === null && m.lastFinishedAt === null);
    else { ensure(typeof m.percentage === 'number' && Number.isFinite(m.percentage) && m.percentage >= 0 && m.percentage <= 100 && Math.abs(m.percentage * 100 - Math.round(m.percentage * 100)) < 1e-8);date(m.lastFinishedAt); }
  }
  ensure(Array.isArray(r.priorityTopicIds) && new Set(r.priorityTopicIds).size === r.priorityTopicIds.length);
  const metrics = r.subjects as SubjectMetric[];
  for (const id of r.priorityTopicIds) { const m = metrics.find(m => m.topicId === id);ensure(m && m.assessed >= 3 && m.wrong + m.blank > 0); }
  return r;
}
async function read(path: string, signal: AbortSignal) {
  const response = await identityRequest(path, { signal });if (!response.ok) throw new ReviewApiError(response.status);return response.body as unknown;
}
export async function reviewOverview(roleId: string, signal: AbortSignal): Promise<ReviewOverview> {
  ensure(reviewRoles.some(r => r.id === roleId));const v = await read(`/api/v1/me/subject-review?roleId=${encodeURIComponent(roleId)}`, signal);
  const r = summary(v, 'window', roleId);const w = row(r.window, ['limit', 'attemptCount', 'hasOlder', 'latestFinishedAt', 'oldestFinishedAt']);
  count(w.attemptCount);ensure(w.limit === 100 && Number(w.attemptCount) <= 100 && typeof w.hasOlder === 'boolean' && (!w.hasOlder || w.attemptCount === 100));
  if (w.attemptCount === 0) ensure(w.latestFinishedAt === null && w.oldestFinishedAt === null);
  else { date(w.latestFinishedAt);date(w.oldestFinishedAt);ensure(Date.parse(String(w.latestFinishedAt)) >= Date.parse(String(w.oldestFinishedAt))); }
  const exposures = (r.subjects as SubjectMetric[]).reduce((total, m) => total + m.exposures, 0);
  ensure(exposures >= Number(w.attemptCount) && exposures <= Number(w.attemptCount) * 100);
  return v as ReviewOverview;
}
export async function individualReview(attemptId: string, roleId: string, signal: AbortSignal): Promise<IndividualReview> {
  ensure(/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(attemptId) && reviewRoles.some(r => r.id === roleId));
  const v = await read(`/api/v1/attempts/${attemptId}/subject-review`, signal);const r = summary(v, 'attemptId', roleId);ensure(r.attemptId === attemptId);
  const metrics = r.subjects as SubjectMetric[];const total = metrics.reduce((sum, m) => sum + m.exposures, 0);
  ensure(total >= 1 && total <= 100 && metrics.every(m => m.assessed === m.exposures));return v as IndividualReview;
}
