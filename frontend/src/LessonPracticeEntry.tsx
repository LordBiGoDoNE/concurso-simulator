import { useEffect, useState } from 'react';
import { lessonPracticeAvailability, type PracticeAvailability } from './lesson-practice-api';
import { studyCatalog, lessonSupportsRole, type Lesson } from './study-catalog';
import type { DirectedPractice } from './directed-practice';

type Availability = { key: string; phase: 'loading' | 'error' } | { key: string; phase: 'ready'; value: PracticeAvailability };
export default function LessonPracticeEntry({ topic, roleId, onRoleChange, onPractice, active, startEnabled = true }: {
  topic: Lesson; roleId: string; onRoleChange: (roleId: string) => void; onPractice: (selection: DirectedPractice) => void; active: boolean; startEnabled?: boolean;
}) {
  const roles = studyCatalog.roles.filter(role => lessonSupportsRole(topic, role.id));
  const chosenRole = roles.some(role => role.id === roleId) ? roleId : roles[0].id;
  const [retry, setRetry] = useState(0);
  const key = `${topic.id}:${chosenRole}:${retry}`;
  const [state, setState] = useState<Availability>({ key: '', phase: 'loading' });
  useEffect(() => {
    if (!active) { setState({ key, phase: 'loading' }); return; }
    const controller = new AbortController();
    setState({ key, phase: 'loading' });
    lessonPracticeAvailability(topic.id, chosenRole, controller.signal)
      .then(value => { if (!controller.signal.aborted) setState({ key, phase: 'ready', value }); })
      .catch(() => { if (!controller.signal.aborted) setState({ key, phase: 'error' }); });
    return () => controller.abort();
  }, [key, topic.id, chosenRole, active, retry]);
  const current = active && state.key === key ? state : { phase: 'loading' as const };
  const canStart = startEnabled && current.phase === 'ready' && current.value.available > 0;
  return <div className="lesson-practice">
    <h2>Pratique a aula selecionada</h2>
    <p>Até cinco questões próprias deste assunto, separadas dos simulados. A correção aparece ao encerrar a prática.</p>
    <p id="lesson-practice-subject"><strong>Assunto da prática: {topic.id} — {topic.title}</strong></p>
    <p>Os links dentro do material não alteram essa seleção. Para mudar o assunto da prática, escolha outra aula nesta página.</p>
    <label htmlFor="lesson-role">Cargo para praticar esta aula</label>
    <select id="lesson-role" value={chosenRole} onChange={event => onRoleChange(event.target.value)}>{roles.map(role => <option key={role.id} value={role.id}>{role.title}</option>)}</select>
    {current.phase === 'loading' && <p role="status">Consultando questões de prática…</p>}
    {current.phase === 'error' && <><p role="alert">Não foi possível consultar a prática. Isso não significa ausência de questões. A aula continua disponível.</p><button className="secondary" disabled={!active} onClick={() => setRetry(value => value + 1)}>Consultar prática novamente</button></>}
    {current.phase === 'ready' && <p role="status">{current.value.available === 0 ? 'Esta aula ainda não tem questões próprias de prática para o cargo escolhido. Você pode continuar estudando a aula.' : `Prática disponível: até ${Math.min(current.value.available, current.value.maximum)} questões. Comece quando estiver pronto.`}</p>}
    {!startEnabled && current.phase === 'ready' && current.value.available > 0 && <p role="status">Confira sua sessão antes de iniciar. A aula continua disponível sem login.</p>}
    <button disabled={!canStart} aria-describedby="lesson-practice-subject" onClick={() => {
      if (canStart) onPractice({ roleId: chosenRole, disciplineId: topic.disciplineId, topicId: topic.id });
    }}>Praticar {topic.id}</button>
  </div>;
}
