import { useEffect, useState } from 'react';
import { lessonPracticeAvailability, type PracticeAvailability } from './lesson-practice-api';
import { type Lesson } from './study-catalog';
import type { LessonPracticeSelection } from './lesson-practice-api';

type Availability = { key: string; phase: 'loading' | 'error' } | { key: string; phase: 'ready'; value: PracticeAvailability };
export default function LessonPracticeEntry({ topic, onPractice, active, startEnabled = true, startState = 'ready' }: {
  topic: Lesson; onPractice: (selection: LessonPracticeSelection) => void; active: boolean; startEnabled?: boolean;
  startState?: 'ready' | 'loading' | 'running' | 'finished' | 'pending';
}) {
  const [retry, setRetry] = useState(0);
  const key = `${topic.id}:${retry}`;
  const [state, setState] = useState<Availability>({ key: '', phase: 'loading' });
  useEffect(() => {
    if (!active) { setState({ key, phase: 'loading' }); return; }
    const controller = new AbortController();
    setState({ key, phase: 'loading' });
    lessonPracticeAvailability(topic.id, controller.signal)
      .then(value => { if (!controller.signal.aborted) setState({ key, phase: 'ready', value }); })
      .catch(() => { if (!controller.signal.aborted) setState({ key, phase: 'error' }); });
    return () => controller.abort();
  }, [key, topic.id, active, retry]);
  const current = active && state.key === key ? state : { phase: 'loading' as const };
  const canStart = startEnabled && ['ready', 'finished'].includes(startState) && current.phase === 'ready' && current.value.available > 0;
  return <div className="lesson-practice">
    <p>Até cinco questões próprias deste assunto, separadas dos simulados. A correção aparece ao encerrar a prática.</p>
    <p id="lesson-practice-subject"><strong>Assunto da prática: {topic.id} — {topic.title}</strong></p>
    <p>Para mudar o assunto da prática, escolha outra aula no índice acima.</p>
    {current.phase === 'loading' && <p role="status">Consultando questões de prática…</p>}
    {current.phase === 'error' && <><p role="alert">Não foi possível consultar a prática. Isso não significa ausência de questões. A aula continua disponível.</p><button className="secondary" disabled={!active} onClick={() => setRetry(value => value + 1)}>Consultar prática novamente</button></>}
    {current.phase === 'ready' && <p role="status">{current.value.available === 0 ? 'Esta aula ainda não tem questões próprias de prática. Você pode continuar estudando a aula.' : `Prática disponível: até ${Math.min(current.value.available, current.value.maximum)} questões.`}</p>}
    {!startEnabled && current.phase === 'ready' && current.value.available > 0 && <p role="status">Confira sua sessão antes de iniciar. A aula continua disponível sem login.</p>}
    <button disabled={!canStart} aria-describedby="lesson-practice-subject" onClick={() => {
      if (canStart) onPractice({ topicId: topic.id });
    }}>{startState === 'loading' ? 'Carregando questões…' : startState === 'running' ? 'Prática em andamento' : startState === 'pending' ? 'Início pendente' : startState === 'finished' ? 'Começar nova prática' : 'Começar prática'}</button>
  </div>;
}
