import { view as attempt, result } from './attempt-fixtures';
import type { PracticeView } from '../src/lesson-practice-api';
const { roleId: _role, ...run } = attempt;
export const view: PracticeView = { ...run, topicId: 'P01' };
export { result };
