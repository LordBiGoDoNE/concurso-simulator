import catalog from './lesson-catalog.json';
import paths from './lesson-paths.json';

export const studyCatalog = catalog;
export const lessons = catalog.topics.filter(topic => Object.hasOwn(paths, topic.id));
export type Lesson = (typeof lessons)[number];
export function lessonSupportsRole(topic: Lesson, roleId: string) {
  if (!roleId) return true;
  if (topic.disciplineId === 'conhecimentos-escolares') return roleId === 'secretario-de-escola';
  if (topic.disciplineId === 'conhecimentos-administrativos') return roleId === 'assistente-administrativo';
  return catalog.roles.some(role => role.id === roleId);
}
