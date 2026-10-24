import { useLayoutEffect, useRef, useState } from 'react';
import { reviewOverview, reviewRoles } from './subject-review-api';
import { useSubjectReview } from './useSubjectReview';
import SubjectReviewSummary from './SubjectReviewSummary';

import type { DirectedPractice } from './directed-practice';

export default function SubjectReviewPanel({ active, refresh, onUnavailable, onPractice, practiceEnabled = true, onRoleChange }: { active: boolean; refresh: number; onUnavailable: () => void; onPractice?: (selection: DirectedPractice) => void; practiceEnabled?: boolean; onRoleChange?: () => void }) {
  const [role, setRole] = useState('');const [retry, setRetry] = useState(0);const heading = useRef<HTMLHeadingElement>(null);const focus = useRef<{ role: string; refresh: number; retry: number } | null>(null);
  const state = useSubjectReview(role, active && role !== '', refresh + retry, signal => reviewOverview(role, signal), onUnavailable);
  useLayoutEffect(() => {
    const intent = focus.current;if (!intent) return;
    if (!active || intent.role !== role || intent.refresh !== refresh || intent.retry !== retry || state.phase === 'error' || state.phase === 'idle') { focus.current = null;return; }
    if (state.phase === 'ready') { focus.current = null;heading.current?.focus(); }
  }, [state, active, role, refresh, retry]);
  return <section aria-labelledby="subject-review-title"><h3 id="subject-review-title" ref={heading} tabIndex={-1}>Revisão por assunto</h3>
    <label htmlFor="subject-review-role">Cargo para revisão</label><select id="subject-review-role" value={role} disabled={!active || !practiceEnabled} onChange={e => { onRoleChange?.(); focus.current = { role: e.target.value, refresh, retry };setRole(e.target.value); }}>
      <option value="">Escolha um cargo</option>{reviewRoles.map(r => <option key={r.id} value={r.id}>{r.title}</option>)}
    </select>
    {!role && <p>Escolha o cargo para consultar seu desempenho privado.</p>}
    {role && <button disabled={!active || state.phase === 'loading'} onClick={() => { focus.current = { role, refresh, retry: retry + 1 };setRetry(retry + 1); }}>Atualizar revisão</button>}
    {state.phase === 'loading' && <p role="status">Consultando revisão por assunto…</p>}
    {state.phase === 'error' && <p role="alert">Revisão indisponível. Não significa ausência de dados ou nota zero. Use Atualizar revisão para tentar novamente.</p>}
    {state.phase === 'ready' && <><p>Janela: {state.value.window.attemptCount} de até {state.value.window.limit} conclusões deste cargo. {state.value.window.hasOlder ? 'Há conclusões anteriores fora da janela.' : 'Não há conclusões anteriores fora da janela.'}</p>
      {state.value.window.latestFinishedAt !== null && <p>De <time dateTime={state.value.window.oldestFinishedAt!}>{new Date(state.value.window.oldestFinishedAt!).toLocaleString('pt-BR')}</time> até <time dateTime={state.value.window.latestFinishedAt}>{new Date(state.value.window.latestFinishedAt).toLocaleString('pt-BR')}</time>.</p>}
      <SubjectReviewSummary summary={state.value} onPractice={onPractice} practiceEnabled={active && practiceEnabled} /></>}
  </section>;
}
