import { attemptRequest, validateRun, ensure, row, id, number, list, type RunView, type RunTransport, type Answer, type Result } from './attempt-api';
import catalog from './lesson-catalog.json';

export interface LessonPracticeSelection { topicId: string }
export interface PracticeView extends RunView { topicId: string }
function view(value: unknown): asserts value is PracticeView {
  validateRun(value, 'topicId');
  const r = value as PracticeView;
  ensure(r.items.length <= 5);
  const topic = catalog.topics.find(t => t.id === r.topicId);
  ensure(topic && r.items.every(i => i.classification.topicId === topic.id && i.classification.disciplineId === topic.disciplineId));
}
const base = '/api/v1/lesson-practices';
export interface PracticeAvailability { topicId: string; available: number; maximum: 5 }
export function lessonPracticeAvailability(topicId: string, signal: AbortSignal) {
  ensure(catalog.topics.some(topic => topic.id === topicId));
  return attemptRequest<PracticeAvailability>(`${base}/availability?${new URLSearchParams({ topicId })}`, signal, value => {
    const result = row(value, ['topicId', 'available', 'maximum']);
    number(result.available); ensure(result.topicId === topicId && result.maximum === 5);
  });
}
const route = (value: string) => { id(value); return `${base}/${value}`; };
export const createLessonPractice = (selection: LessonPracticeSelection, commandId: string, signal: AbortSignal) => attemptRequest<PracticeView>(base, signal, v => {
  view(v); ensure(v.topicId === selection.topicId);
}, 'POST', { commandId, topicId: selection.topicId });
export interface PracticeFinished { attempt: PracticeView; result: Result }
export interface PracticeTransport extends RunTransport {
  read: (value: string, signal: AbortSignal) => Promise<PracticeView>;
  save: (value: string, itemId: string, answer: Answer, signal: AbortSignal) => Promise<PracticeView>;
  finish: (value: string, commandId: string, expectedVersion: number, signal: AbortSignal) => Promise<PracticeFinished>;
}
export const lessonPracticeTransport: PracticeTransport = {
  read: (value, signal) => attemptRequest<PracticeView>(route(value), signal, v => { view(v); ensure(v.id === value); }),
  save: (value, itemId, answer, signal) => attemptRequest<PracticeView>(`${route(value)}/answers/${encodeURIComponent(itemId)}`, signal, v => {
    view(v); ensure(v.id === value && v.version === answer.expectedVersion + 1 && v.finishedAt === null && v.items.some(i => i.id === itemId && i.selectedAlternativeId === answer.alternativeId));
  }, 'PUT', answer),
  finish: (value, commandId, expectedVersion, signal) => attemptRequest<PracticeFinished>(`${route(value)}/finish`, signal, async v => {
    const r = row(v, ['attempt', 'result']); view(r.attempt);
    ensure(r.attempt.id === value && r.attempt.version === expectedVersion + 1 && r.attempt.finishedAt !== null);
    const validator = await import('./attempt-result-api'); const result = validator.result(r.result);
    const attempt = r.attempt;
    ensure(result.total === attempt.items.length && result.items.every((c, i) => c.item.id === attempt.items[i].id));
    return { attempt, result };
  }, 'POST', { commandId, expectedVersion }),
  result: (value, signal) => attemptRequest<Result>(`${route(value)}/result`, signal, async v => {
    const validator = await import('./attempt-result-api'); return validator.result(v);
  }),
};
export interface PracticeHistoryItem { id: string; topicId: string; createdAt: string; finishedAt: string | null; total: number; percentage: number | null }
export interface PracticeHistoryPage { items: PracticeHistoryItem[]; page: number; size: number }
export const lessonPracticeHistory = (page: number, signal: AbortSignal) => attemptRequest<PracticeHistoryPage>(`/api/v1/me/lesson-practices?page=${page}&size=20`, signal, v => {
  const r = row(v, ['items', 'page', 'size']); ensure(r.page === page && r.size === 20); list(r.items);
  r.items.forEach(value => {
    const e = row(value, ['id', 'topicId', 'createdAt', 'finishedAt', 'total', 'percentage']); id(e.id); number(e.total); ensure(Number(e.total) >= 1 && Number(e.total) <= 5);
    ensure(catalog.topics.some(t => t.id === e.topicId));
    ensure(typeof e.createdAt === 'string' && Number.isFinite(Date.parse(e.createdAt)));
    if (e.finishedAt === null) ensure(e.percentage === null);
    else ensure(typeof e.finishedAt === 'string' && Number.isFinite(Date.parse(e.finishedAt)) && typeof e.percentage === 'number' && e.percentage >= 0 && e.percentage <= 100);
  });
});
