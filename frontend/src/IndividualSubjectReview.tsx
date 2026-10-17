import { useState } from 'react';
import { individualReview } from './subject-review-api';
import { useSubjectReview } from './useSubjectReview';
import SubjectReviewSummary from './SubjectReviewSummary';

import type { DirectedPractice } from './directed-practice';

export default function IndividualSubjectReview({ attemptId, roleId, active, onUnavailable, onPractice }: { attemptId: string; roleId: string; active: boolean; onUnavailable: () => void; onPractice?: (selection: DirectedPractice) => void }) {
  const [retry, setRetry] = useState(0);
  const state = useSubjectReview(`${attemptId}:${roleId}`, active, retry, signal => individualReview(attemptId, roleId, signal), onUnavailable);
  return <section aria-labelledby="individual-subject-review-title"><h3 id="individual-subject-review-title">Revisão por assunto deste simulado</h3>
    {state.phase === 'loading' && <p role="status">Consultando assuntos deste resultado…</p>}
    {state.phase === 'error' && <><p role="alert">{state.status === 409 ? 'A conclusão ainda não foi confirmada para esta revisão.' : 'Revisão por assunto indisponível. O resultado continua concluído; não foi calculada uma nota local.'}</p><button onClick={() => setRetry(v => v + 1)}>Tentar revisão deste simulado novamente</button></>}
    {state.phase === 'ready' && <SubjectReviewSummary summary={state.value} individual onPractice={onPractice} practiceEnabled={active} />}
  </section>;
}
