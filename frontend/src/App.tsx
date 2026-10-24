import { useCallback, useEffect, useRef, useState } from 'react';
import LoginPanel from './LoginPanel';
import QuestionBrowser from './QuestionBrowser';
import AttemptWorkspace from './AttemptWorkspace';
import LessonPracticeWorkspace, { type LessonPracticeHandle } from './LessonPracticeWorkspace';
import StudyHome from './StudyHome';
import LessonLibrary from './LessonLibrary';
import type { DirectedPractice } from './directed-practice';
import type { CurrentIdentity } from './identity-api';

const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
import { materialUrl } from './lesson-links';
const configuredTimeout = Number(import.meta.env.VITE_STATUS_TIMEOUT_MS || 5000);
const timeoutMs = Number.isFinite(configuredTimeout) && configuredTimeout > 0 ? configuredTimeout : 5000;
type Area = 'home' | 'attempts' | 'lessons' | 'practice' | 'questions';
const areaNames: Record<Area, string> = { home: 'Início', attempts: 'Simulados', lessons: 'Aulas', practice: 'Práticas', questions: 'Banco de questões' };

export default function App() {
  const [area, setArea] = useState<Area>('home');
  const [runActive, setRunActive] = useState(false);
  const [lessonsOpened, setLessonsOpened] = useState(false);
  const [lessonDiscipline, setLessonDiscipline] = useState(''); const [practiceNotice, setPracticeNotice] = useState('');
  const practiceWorkspace = useRef<LessonPracticeHandle>(null);
  const [lessonTarget,setLessonTarget] = useState<{id:string;version:number}|null>(null);
  const pageHeading = useRef<HTMLHeadingElement>(null); const navigated = useRef(false);
  function navigate(next: Area) { if (next === area) return; if (next === 'lessons') setLessonsOpened(true); navigated.current = true; setArea(next); }
  useEffect(() => { if (navigated.current) { navigated.current = false; pageHeading.current?.focus(); } }, [area]);
  function openLessons(discipline = '') { setLessonDiscipline(discipline); navigate('lessons'); }
  function practiceLesson(selection: DirectedPractice) {
    if (checking || !practiceWorkspace.current?.start(selection)) { setPracticeNotice('Prática não iniciada. A prática atual foi preservada; confira sua sessão ou aguarde uma operação em andamento.'); return; }
    setPracticeNotice(''); navigate('practice');
  }
  const [attempt, setAttempt] = useState(0);
  const [status, setStatus] = useState<'loading' | 'up' | 'down'>('loading');
  const [identity, setIdentity] = useState<CurrentIdentity | null>(null); const [checking, setChecking] = useState(true);
  const guestActive = useRef({attempt:false,practice:false}); const [privateNotice, setPrivateNotice] = useState('');
  const setGuestActive = useCallback((value: boolean) => { guestActive.current.attempt = value; }, []);
  const setPracticeGuestActive = useCallback((value: boolean) => { guestActive.current.practice = value; }, []);
  const backToLesson = useCallback((id:string) => { setLessonTarget(previous=>({id,version:(previous?.version??0)+1}));setLessonsOpened(true);navigate('lessons'); }, [area]);
  const changedIdentity = useCallback((value: CurrentIdentity | null) => { setIdentity(value); setChecking(false); }, []);
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
      <a className="skip-link" href="#study-content">Pular para o conteúdo</a>
      <aside className="sidebar"><div className="brand"><span className="brand-symbol" aria-hidden="true">CS</span><span>Concurso Simulator<small>Seu espaço de estudo</small></span></div>
        <details className="study-menu" open={!(area === 'attempts' && runActive && !checking)}><summary>Ir para outra área</summary><nav aria-label="Navegação principal">{(Object.keys(areaNames) as Area[]).map(item => <button key={item} aria-current={area === item ? 'page' : undefined} onClick={() => navigate(item)}>{areaNames[item]}</button>)}</nav></details>
        <div className="sidebar-foot"><p>Aprenda no seu ritmo.<br /><small>Aulas, prática e revisão.</small></p></div>
      </aside>
      <div className="workspace"><header className="topbar"><span>Seu espaço de estudo <span aria-hidden="true">/</span> <strong>{areaNames[area]}</strong></span><span className="identity-pill">{checking ? 'Conferindo sessão' : identity?.connected ? 'Conta conectada' : 'Acesso visitante'}</span></header>
      <main id="study-content" tabIndex={-1}>
      <h2 ref={pageHeading} tabIndex={-1} className="sr-only">{areaNames[area]}</h2>
      <div hidden={area !== 'home'}><StudyHome onCompose={() => navigate('attempts')} onLessons={openLessons} /></div>
      <div className="identity-panel">
      <LoginPanel onIdentity={changedIdentity} onChecking={checkingIdentity} onBeforeLogin={beforeLogin} />
      </div>
      {privateNotice && <p role="status">{privateNotice}</p>}
      {checking && area === 'attempts' && <p role="status">Conferindo acesso aos simulados…</p>}
      <div hidden={checking || area !== 'attempts'}>{identity && <AttemptWorkspace key={identity.connected ? identity.id : 'visitor'} identity={identity} contextActive={!checking} reviewActive={!checking && area === 'attempts'} onUnavailable={privateUnavailable} onGuestActive={setGuestActive} onRunActiveChange={setRunActive} />}</div>
      <div hidden={area !== 'lessons'}>{practiceNotice && <p role="status">{practiceNotice}</p>}{lessonsOpened && <LessonLibrary initialDiscipline={lessonDiscipline} requestedLesson={lessonTarget} onPractice={practiceLesson} />}</div>
      <div hidden={checking || area !== 'practice'}>{identity && <LessonPracticeWorkspace ref={practiceWorkspace} key={identity.connected ? identity.id : 'visitor'} identity={identity} contextActive={!checking} onUnavailable={privateUnavailable} onGuestActive={setPracticeGuestActive} onChooseLesson={()=>openLessons()} onBackToLesson={backToLesson}/>}</div>
      <div hidden={area !== 'questions'}><QuestionBrowser /></div>
      <footer className="availability">
        <a href={materialUrl}>Acessar material de estudo</a>
        <p role="status" aria-live="polite">
          {status === 'loading' ? 'Verificando disponibilidade da API…' : status === 'up' ? 'API disponível.' : 'API indisponível. Você pode continuar acessando as aulas.'}
        </p>
        <button disabled={status === 'loading'} onClick={() => setAttempt(value => value + 1)}>Verificar novamente</button>
      </footer>
    </main>
    </div></div>
  );
}
