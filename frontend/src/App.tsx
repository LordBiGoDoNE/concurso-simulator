import { useCallback, useEffect, useRef, useState } from 'react';
import LoginPanel from './LoginPanel';
import QuestionBrowser from './QuestionBrowser';
import AttemptWorkspace from './AttemptWorkspace';
import LessonPracticeWorkspace from './LessonPracticeWorkspace';
import StudyHome from './StudyHome';
import LessonLibrary, { LessonPagination } from './LessonLibrary';
import { lessons } from './study-catalog';
import type { CurrentIdentity } from './identity-api';
import { useStudyNavigation, locationForRole, type Area } from './study-navigation';

const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const configuredTimeout = Number(import.meta.env.VITE_STATUS_TIMEOUT_MS || 5000);
const timeoutMs = Number.isFinite(configuredTimeout) && configuredTimeout > 0 ? configuredTimeout : 5000;
const areaNames: Record<Area, string> = { home: 'Início', attempts: 'Simulados', lessons: 'Aulas', practice: 'Práticas', questions: 'Banco de questões' };

export default function App() {
  const { route, notice: navigationNotice, navigate: updateRoute } = useStudyNavigation();
  const area = route.area;
  const [runActive, setRunActive] = useState(false);
  const [lessonsOpened, setLessonsOpened] = useState(area === 'lessons');
  const lesson = area === 'lessons' ? lessons.find(topic => topic.id === route.topicId) : undefined;
  const pageHeading = useRef<HTMLHeadingElement>(null); const previousArea = useRef(area);
  function navigate(next: Area) { updateRoute({ area: next }); }
  useEffect(() => {
    if (area === 'lessons') setLessonsOpened(true);
    if (previousArea.current !== area) pageHeading.current?.focus();
    previousArea.current = area;
  }, [area]);
  function openLessons(discipline?: string, roleId?: string) {
    updateRoute({ ...(roleId === undefined ? {} : locationForRole(route, roleId)), area: 'lessons', ...(discipline === undefined ? {} : { disciplineId: discipline, topicId: '', search: '' }) });
  }
  const [attempt, setAttempt] = useState(0);
  const [status, setStatus] = useState<'loading' | 'up' | 'down'>('loading');
  const [identity, setIdentity] = useState<CurrentIdentity | null>(null); const [checking, setChecking] = useState(true);
  const guestActive = useRef({attempt:false,practice:false}); const [privateNotice, setPrivateNotice] = useState('');
  const setGuestActive = useCallback((value: boolean) => { guestActive.current.attempt = value; }, []);
  const setPracticeGuestActive = useCallback((value: boolean) => { guestActive.current.practice = value; }, []);
  const backToLesson = useCallback((id:string, roleId:string) => { updateRoute({ area: 'lessons', topicId: id, roleId }); }, [updateRoute]);
  const lastAccountId = useRef<string | null>(null);
  const changedIdentity = useCallback((value: CurrentIdentity | null) => {
    if (value?.connected) {
      if (lastAccountId.current && lastAccountId.current !== value.id) setPrivateNotice('');
      lastAccountId.current = value.id;
    }
    setIdentity(value); setChecking(false);
  }, []);
  const checkingIdentity = useCallback(() => setChecking(true), []);
  const privateUnavailable = useCallback(() => { setIdentity(null); setChecking(true); setPrivateNotice('Tentativa ou sessão indisponível. O conteúdo privado foi limpo; confira sua sessão e o histórico da conta.'); window.dispatchEvent(new Event('identity-recheck')); }, []);
  const beforeLogin = useCallback(() => {
    if (!identity?.connected && (guestActive.current.attempt || guestActive.current.practice) && !window.confirm('As realizações visitantes não serão transferidas para a conta. Entrar agora encerra o simulado e a prática visitantes na interface. Deseja continuar?')) return false;
    setIdentity(null); return true;
  }, [identity]);

  useEffect(() => {
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), timeoutMs);
    let active = true;
    setStatus('loading');
    fetch(`${apiUrl.replace(/\/$/, '')}/api/v1/status`, { signal: controller.signal })
      .then(async response => {
        const body = await response.json();
        if (active) setStatus(response.ok && body.status === 'UP' ? 'up' : 'down');
      })
      .catch(() => { if (active) setStatus('down'); })
      .finally(() => window.clearTimeout(timer));
    return () => { active = false; window.clearTimeout(timer); controller.abort(); };
  }, [attempt]);

  return (
    <div className={`app-layout${area === 'attempts' && runActive && !checking ? ' exam-mode' : ''}`}>
      <a className="skip-link" href="#study-content" onClick={event => { event.preventDefault(); document.getElementById('study-content')?.focus(); }}>Pular para o conteúdo</a>
      <aside className="sidebar"><div className="brand"><span className="brand-symbol" aria-hidden="true">CS</span><span>Concurso Simulator<small>Seu espaço de estudo</small></span></div>
        <details className="study-menu" open={!(area === 'attempts' && runActive && !checking)}><summary>Ir para outra área</summary><nav aria-label="Navegação principal">{(Object.keys(areaNames) as Area[]).map(item => <button key={item} aria-current={area === item ? 'page' : undefined} onClick={() => navigate(item)}>{areaNames[item]}</button>)}</nav></details>
        <div className="sidebar-foot"><p>Aprenda no seu ritmo.<br /><small>Aulas, prática e revisão.</small></p></div>
      </aside>
      <div className="workspace"><header className="topbar"><span>Seu espaço de estudo <span aria-hidden="true">/</span> <strong>{areaNames[area]}</strong></span><span className="identity-pill">{checking ? 'Conferindo sessão' : identity?.connected ? 'Conta conectada' : 'Acesso visitante'}</span></header>
      <main id="study-content" tabIndex={-1}>
      <h2 ref={pageHeading} tabIndex={-1} className="sr-only">{areaNames[area]}</h2>
      {navigationNotice && <p role="status">{navigationNotice}</p>}
      <div hidden={area !== 'home'}><StudyHome roleId={route.roleId} onRoleChange={roleId => updateRoute(locationForRole(route, roleId))} onCompose={() => navigate('attempts')} onQuestions={() => navigate('questions')} onLessons={openLessons} /></div>
      <div className="identity-panel">
      <LoginPanel onIdentity={changedIdentity} onChecking={checkingIdentity} onBeforeLogin={beforeLogin} />
      </div>
      {privateNotice && <p role="status">{privateNotice}</p>}
      {checking && area === 'attempts' && <p role="status">Conferindo acesso aos simulados…</p>}
      <div hidden={checking || area !== 'attempts'}>{identity && <AttemptWorkspace key={identity.connected ? identity.id : 'visitor'} identity={identity} contextActive={!checking} reviewActive={!checking && area === 'attempts'} onUnavailable={privateUnavailable} onGuestActive={setGuestActive} onRunActiveChange={setRunActive} />}</div>
      <div hidden={area !== 'lessons'}>{(lessonsOpened || area === 'lessons') && <LessonLibrary location={route} onLocationChange={(value, replace) => updateRoute({ ...value, area: 'lessons' }, replace)} showPagination={false} />}</div>
      <div hidden={lesson ? false : checking || area !== 'practice'}><LessonPracticeWorkspace key={identity ? identity.connected ? identity.id : 'visitor' : 'no-identity'} identity={identity} contextActive={!checking && identity !== null} inlineLesson={lesson ? { topic: lesson, roleId: route.roleId, onRoleChange: roleId => updateRoute(locationForRole(route, roleId)) } : undefined} onUnavailable={privateUnavailable} onGuestActive={setPracticeGuestActive} onChooseLesson={()=>openLessons()} onBackToLesson={backToLesson}/></div>
      {lesson && <LessonPagination topicId={lesson.id} roleId={route.roleId} onSelect={topicId => updateRoute({ topicId })} />}
      <div hidden={area !== 'questions'}><QuestionBrowser /></div>
      <footer className="availability">
        <a href="#/aulas" onClick={event => { event.preventDefault(); updateRoute({ area: 'lessons', topicId: '', disciplineId: '', search: '' }); }}>Acessar material de estudo</a>
        <p role="status" aria-live="polite">
          {status === 'loading' ? 'Verificando disponibilidade da API…' : status === 'up' ? 'API disponível.' : 'API indisponível. Você pode continuar acessando as aulas.'}
        </p>
        <button disabled={status === 'loading'} onClick={() => setAttempt(value => value + 1)}>Verificar novamente</button>
      </footer>
    </main>
    </div></div>
  );
}
