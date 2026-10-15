import { detail } from './question-fixtures';
import type { AttemptView, Result } from '../src/attempt-api';
export const attemptId = '70000000-0000-0000-0000-000000000001';
export const itemId = '80000000-0000-0000-0000-000000000001';
export const view: AttemptView = { id: attemptId, roleId: 'assistente-administrativo', createdAt: '2026-10-14T12:00:00Z', finishedAt: null, version: 0,
  items: [{ ...detail, id: itemId, selectedAlternativeId: null }] };
export const result: Result = { total: 1, points: 0, correct: 0, wrong: 0, blank: 1, percentage: 0,
  items: [{ item: view.items[0], correctAnswer: detail.alternatives[0].content, explanation: { text: 'PRIVATE_ATTEMPT_RESOLUTION_SENTINEL', resources: [] } }] };
