import type { ReviewOverview, IndividualReview, SubjectMetric } from '../src/subject-review-api';
import { view } from './attempt-fixtures';
export const reviewDate = '2026-10-16T12:00:00Z';
export function subjects(): SubjectMetric[] {
  return [['P', 10, 'portugues'], ['M', 12, 'matematica-raciocinio'], ['I', 10, 'informatica'], ['A', 8, 'conhecimentos-administrativos'], ['S', 11, 'conhecimentos-escolares']].flatMap(([prefix, count, discipline]) =>
    Array.from({ length: Number(count) }, (_, i) => ({ topicId: `${prefix}${String(i + 1).padStart(2, '0')}`, title: `Assunto ${prefix}${i + 1}`, disciplineId: String(discipline), disciplineTitle: `Disciplina ${prefix}`, correct: 0, wrong: 0, blank: 0, assessed: 0, exposures: 0, percentage: null, lastFinishedAt: null })));
}
export function overview(empty = false, roleId = view.roleId): ReviewOverview {
  const metrics = subjects();if (!empty) metrics[0] = { ...metrics[0], blank: 3, assessed: 3, exposures: 3, percentage: 0, lastFinishedAt: reviewDate };
  return { policy: 'subject-review/v1', roleId, minimumBases: 3, window: { limit: 100, attemptCount: empty ? 0 : 1, hasOlder: false, latestFinishedAt: empty ? null : reviewDate, oldestFinishedAt: empty ? null : reviewDate }, subjects: metrics, priorityTopicIds: empty ? [] : ['P01'] };
}
export function individual(): IndividualReview { const { window: _window, ...summary } = overview();return { ...summary, attemptId: view.id }; }
