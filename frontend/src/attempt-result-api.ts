import { ensure, row, number, list, item, id, content, validateAttempt, type Result, type Finished, type AttemptItem } from './attempt-api';
export function validateResult(v: unknown): asserts v is Result {
  const r = row(v, ['total', 'points', 'correct', 'wrong', 'blank', 'percentage', 'items']);
  ['total', 'points', 'correct', 'wrong', 'blank'].forEach(k => number(r[k])); ensure(typeof r.percentage === 'number' && r.percentage >= 0 && r.percentage <= 100);
  ensure(Number(r.total) === Number(r.correct) + Number(r.wrong) + Number(r.blank) && r.points === r.correct);
  list(r.items); ensure(r.items.length === r.total); r.items.forEach(v => {
    const a = row(v, ['item', 'correctAlternativeId', 'resolution']); item(a.item); id(a.correctAlternativeId); content(a.resolution);
    ensure((a.item as AttemptItem).alternatives.some(o => o.id === a.correctAlternativeId));
  });
}
export function validateFinished(v: unknown, expectedId: string, expectedVersion: number): asserts v is Finished {
  const r = row(v, ['attempt', 'result']); validateAttempt(r.attempt);
  ensure(r.attempt.id === expectedId && r.attempt.finishedAt !== null && r.attempt.version === expectedVersion + 1);
  validateResult(r.result); const attempt = r.attempt;
  ensure(r.result.total === attempt.items.length && r.result.items.every((c, i) => c.item.id === attempt.items[i].id));
}
