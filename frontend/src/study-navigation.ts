import { useCallback, useEffect, useRef, useState } from 'react';
import { lessons, lessonSupportsRole, studyCatalog } from './study-catalog';

const areaPaths = { home: 'inicio', lessons: 'aulas', attempts: 'simulados', practice: 'praticas', questions: 'questoes' } as const;
export type Area = keyof typeof areaPaths;
export interface LessonLocation { roleId: string; disciplineId: string; search: string; topicId: string }
export interface StudyRoute extends LessonLocation { area: Area }
export const emptyLessonLocation: LessonLocation = { roleId: '', disciplineId: '', search: '', topicId: '' };
const initialRoute: StudyRoute = { area: 'home', ...emptyLessonLocation };
const invalidNotice = 'O endereço tinha opções desconhecidas ou incompatíveis. Ajustamos a navegação; escolha o cargo ou a aula novamente.';

export function locationForRole(location: LessonLocation, roleId: string): LessonLocation {
  const topic = lessons.find(t => t.id === location.topicId);
  return {
    ...location, roleId,
    disciplineId: lessons.some(t => t.disciplineId === location.disciplineId && lessonSupportsRole(t, roleId)) ? location.disciplineId : '',
    topicId: topic && lessonSupportsRole(topic, roleId) ? topic.id : '',
  };
}

export function routeHash(route: StudyRoute) {
  const params = new URLSearchParams();
  if (route.roleId) params.set('cargo', route.roleId);
  if (route.disciplineId) params.set('disciplina', route.disciplineId);
  if (route.search) params.set('busca', route.search);
  if (route.topicId && route.area !== 'lessons') params.set('aula', route.topicId);
  const path = areaPaths[route.area] + (route.area === 'lessons' && route.topicId ? '/' + route.topicId : '');
  return '#/' + path + (params.size ? '?' + params.toString() : '');
}

export function parseStudyRoute(hash: string): { route: StudyRoute; notice: string } {
  if (!hash || hash === '#') return { route: { ...initialRoute }, notice: '' };
  const match = /^#\/([^/?]+)(?:\/([^/?]+))?(?:\?([^#]*))?$/.exec(hash);
  if (!match) return { route: { ...initialRoute }, notice: invalidNotice };
  const area = (Object.keys(areaPaths) as Area[]).find(key => areaPaths[key] === match[1]);
  if (!area) return { route: { ...initialRoute }, notice: invalidNotice };
  const params = new URLSearchParams(match[3]);
  let invalid = false;
  const allowed = ['cargo', 'disciplina', 'busca', ...(area === 'lessons' ? [] : ['aula'])];
  params.forEach((_, key) => { if (!allowed.includes(key) || params.getAll(key).length !== 1) invalid = true; });
  const read = (key: string) => params.getAll(key).length === 1 ? params.get(key)! : '';
  const route: StudyRoute = { area, ...emptyLessonLocation };
  const role = read('cargo'), discipline = read('disciplina');
  if (role && !studyCatalog.roles.some(r => r.id === role)) invalid = true; else route.roleId = role;
  if (discipline && !studyCatalog.disciplines.some(d => d.id === discipline)) invalid = true; else route.disciplineId = discipline;
  const topic = area === 'lessons' ? match[2] ?? '' : read('aula');
  if (match[2] && area !== 'lessons') invalid = true;
  if (topic && !lessons.some(t => t.id === topic)) invalid = true; else route.topicId = topic;
  const search = read('busca');
  route.search = search.replace(/[\u0000-\u001f\u007f]/g, '').slice(0, 160);
  if (search !== route.search) invalid = true;
  const compatible = locationForRole(route, route.roleId);
  if (compatible.disciplineId !== route.disciplineId || compatible.topicId !== route.topicId) invalid = true;
  return { route: { ...route, ...compatible }, notice: invalid ? invalidNotice : '' };
}

export function useStudyNavigation() {
  const [state, setState] = useState(() => parseStudyRoute(window.location.hash));
  const current = useRef(state.route);
  current.current = state.route;
  useEffect(() => {
    const restore = () => {
      const next = parseStudyRoute(window.location.hash);
      current.current = next.route;
      if (window.location.hash !== routeHash(next.route)) window.history.replaceState(null, '', routeHash(next.route));
      setState(previous => routeHash(previous.route) === routeHash(next.route) && !next.notice ? previous : next);
    };
    if (window.location.hash !== routeHash(current.current)) window.history.replaceState(null, '', routeHash(current.current));
    window.addEventListener('hashchange', restore);
    window.addEventListener('popstate', restore);
    return () => { window.removeEventListener('hashchange', restore); window.removeEventListener('popstate', restore); };
  }, []);
  const navigate = useCallback((patch: Partial<StudyRoute>, replace = false) => {
    const next = parseStudyRoute(routeHash({ ...current.current, ...patch }));
    const hash = routeHash(next.route);
    if (window.location.hash !== hash) window.history[replace ? 'replaceState' : 'pushState'](null, '', hash);
    current.current = next.route;
    setState(next);
  }, []);
  return { ...state, navigate };
}
