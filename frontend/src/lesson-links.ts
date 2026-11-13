import { emptyLessonLocation, routeHash } from './study-navigation';
import { lessons, lessonSupportsRole, studyCatalog } from './study-catalog';

export function nativeLessonUrl(topicId: string, roleId = ''): string | null {
  const topic = lessons.find(topic => topic.id === topicId);
  if (!topic || roleId && !studyCatalog.roles.some(role => role.id === roleId)) return null;
  // Review lists all 51 subjects. Other-role lessons remain freely readable in
  // the new tab without changing the role of the current attempt or its filters.
  return routeHash({ ...emptyLessonLocation, area: 'lessons', topicId, roleId: lessonSupportsRole(topic, roleId) ? roleId : '' });
}
