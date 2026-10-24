import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { questionCatalog, type Catalog } from './question-api';
import { AttemptApiError, createAttempt, type AttemptView, type Composition } from './attempt-api';

import type { DirectedPractice } from './directed-practice';

export default function AttemptComposer({ onCreated, onUnavailable, prefill, active = true, onBusy }: { onCreated: (v: AttemptView) => void; onUnavailable: () => void; prefill?: DirectedPractice | null; active?: boolean; onBusy?: (value: boolean) => void }) {
  const [catalog, setCatalog] = useState<Catalog | null>(null); const [catalogFailed, setCatalogFailed] = useState(false); const [retry, setRetry] = useState(0);
  const [role, setRole] = useState(''); const [quantities, setQuantities] = useState<Record<string, string>>({}); const [topics, setTopics] = useState<Record<string, string[]>>({});
  const [useParameterizedQuestions, setUseParameterizedQuestions] = useState(false);
  const [busy, setBusy] = useState(false); const [message, setMessage] = useState(''); const pending = useRef(false);
  const heading = useRef<HTMLHeadingElement>(null); const focus = useRef(false);
  const operation = useRef<{ key: string; id: string } | null>(null); const lifetime = useRef<AbortController | null>(null);
  useEffect(() => { lifetime.current = new AbortController(); return () => { lifetime.current?.abort(); onBusy?.(false); }; }, [onBusy]);
  useEffect(() => {
    const controller = new AbortController(); let active = true; setCatalogFailed(false);
    questionCatalog(controller.signal).then(c => { if (active) {
      if (prefill && (!c.roles.some(r => r.id === prefill.roleId) || !c.disciplines.some(d => d.id === prefill.disciplineId) || !c.topics.some(t => t.id === prefill.topicId && t.disciplineId === prefill.disciplineId))) throw new Error('Seleção fora do catálogo');
      setCatalog(c); setRole(prefill?.roleId || c.roles[0]?.id || '');
      setQuantities(prefill ? Object.fromEntries(c.disciplines.map(d => [d.id, d.id === prefill.disciplineId ? '1' : '0'])) : { [c.disciplines[0]?.id || '']: '5' });
      setTopics(prefill ? { [prefill.disciplineId]: [prefill.topicId] } : {}); setUseParameterizedQuestions(false); focus.current = !!prefill;
    } }).catch(() => { if (active) setCatalogFailed(true); });
    return () => { active = false; controller.abort(); };
  }, [retry, prefill]);
  useLayoutEffect(() => { if (catalog && focus.current && active) { focus.current = false; heading.current?.focus(); } }, [catalog, active]);
  function changeCriteria(change: () => void) { operation.current = null; setMessage(''); change(); }
  const portions = (catalog?.disciplines || []).map(d => ({ disciplineId: d.id, quantity: Number(quantities[d.id] || 0), topicIds: topics[d.id] || [] })).filter(d => d.quantity !== 0);
  const total = portions.reduce((n, d) => n + d.quantity, 0); const valid = !!role && Number.isInteger(total) && total >= 1 && total <= 100 && portions.every(p => Number.isInteger(p.quantity) && p.quantity > 0 && p.quantity <= 100);
  async function submit() {
    if (!active || pending.current || !valid || !lifetime.current) return;
    const controller = lifetime.current;
    pending.current = true; setBusy(true); onBusy?.(true); setMessage('');
    const composition: Composition = { roleId: role, disciplines: portions, useParameterizedQuestions }; const key = JSON.stringify(composition);
    if (operation.current?.key !== key) operation.current = { key, id: crypto.randomUUID() };
    try { const value = await createAttempt(composition, operation.current.id, controller.signal); if (!controller.signal.aborted) onCreated(value); }
    catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof AttemptApiError && [401, 403].includes(error.status)) { onUnavailable(); return; }
      setMessage(error instanceof AttemptApiError && error.available.length ? `Acervo insuficiente: ${error.available.map(a => `${catalog?.disciplines.find(d => d.id === a.disciplineId)?.title || a.disciplineId}: ${a.available} disponíveis`).join('; ')}.` : 'Criação não confirmada. Tente novamente com os mesmos critérios para recuperar a tentativa, se ela foi gravada.');
    } finally { pending.current = false; if (!controller.signal.aborted) { onBusy?.(false); setBusy(false); } }
  }
  return <section className="composer" aria-labelledby="compose-attempt-title">
    <header className="page-intro"><p className="eyebrow">Um treino do seu jeito</p>
    <h1 id="compose-attempt-title" ref={heading} tabIndex={-1}>Compor simulado</h1>
    <p>Escolha de 1 a 100 questões. Distribuição livre e um ponto por acerto; não são regras oficiais do edital.</p></header>
    {!catalog && !catalogFailed && <p role="status">Carregando composição…</p>}
    {catalogFailed && <><p role="alert">Composição indisponível. As aulas continuam acessíveis.</p><button onClick={() => setRetry(v => v + 1)}>Tentar composição novamente</button></>}
    {prefill && <p>Prática direcionada de {prefill.topicId}. Confira cargo, assunto e quantidade antes de iniciar. Não há garantia de questão nova.</p>}
    {catalog && <form className="compose-layout" onSubmit={e => { e.preventDefault(); void submit(); }}>
      <div className="settings-card"><div className="setting-block"><h2><span className="step-number" aria-hidden="true">01</span> Para qual cargo?</h2>
      <label htmlFor="attempt-role">Cargo do simulado</label><select id="attempt-role" value={role} disabled={busy || !active} onChange={e => changeCriteria(() => setRole(e.target.value))}>{catalog.roles.map(r => <option key={r.id} value={r.id}>{r.title}</option>)}</select>
      </div><div className="setting-block"><h2><span className="step-number" aria-hidden="true">02</span> O que você quer praticar?</h2><p className="muted">Distribua as questões. Deixe zero para não incluir uma disciplina.</p>
      <div className="attempt-portions">{catalog.disciplines.map(d => <fieldset key={d.id} disabled={busy || !active}>
        <legend>{d.title}</legend><label htmlFor={`quantity-${d.id}`}>Quantidade de {d.title}</label>
        <input id={`quantity-${d.id}`} type="number" min="0" max="100" step="1" value={quantities[d.id] || ''} onChange={e => changeCriteria(() => setQuantities(v => ({ ...v, [d.id]: e.target.value })))} />
        <details><summary>Assuntos de {d.title} (opcional)</summary>{catalog.topics.filter(t => t.disciplineId === d.id).map(t => <label className="attempt-topic" key={t.id}><input type="checkbox" checked={(topics[d.id] || []).includes(t.id)} onChange={e => changeCriteria(() => setTopics(v => ({ ...v, [d.id]: e.target.checked ? [...(v[d.id] || []), t.id] : (v[d.id] || []).filter(id => id !== t.id) })))} />{t.title}</label>)}</details>
      </fieldset>)}</div></div>
      <div className="setting-block"><h2><span className="step-number" aria-hidden="true">03</span> Varie seu treino</h2>
      <label htmlFor="attempt-variations"><input id="attempt-variations" type="checkbox" checked={useParameterizedQuestions} disabled={busy || !active}
        aria-describedby="attempt-variations-help" onChange={e => changeCriteria(() => setUseParameterizedQuestions(e.target.checked))} />Usar variações numéricas quando disponíveis</label>
      <p id="attempt-variations-help">Pode misturar questões fixas e variações de regra de três direta, porcentagem e juros simples. Os valores podem se repetir entre simulados; não há garantia de questão nova.</p>
      </div></div>
      <aside className="summary-card" aria-labelledby="composition-summary-title"><div className="summary-head"><h2 id="composition-summary-title" className="eyebrow">Seu simulado</h2><p className="summary-total"><strong>{Number.isFinite(total) ? total : '—'}</strong> questões</p></div><div className="summary-body"><p className="summary-role">{catalog.roles.find(r => r.id === role)?.title}</p><ul>{portions.map(p => <li key={p.disciplineId}><span>{catalog.disciplines.find(d => d.id === p.disciplineId)?.title}</span><strong>{p.quantity}</strong></li>)}</ul>
      <p role="status">Total: {total} questões. {!valid && 'Informe quantidades inteiras que somem de 1 a 100.'}</p>
      {message && <p role="alert">{message}</p>}
      <div className="compose-start"><span aria-hidden="true">{Number.isFinite(total) ? total : '—'} questões</span><button disabled={!valid || busy || !active} type="submit">{busy ? 'Criando simulado…' : message ? 'Tentar criar novamente' : 'Iniciar simulado'}</button></div>
      <p className="muted">Confira os critérios antes de iniciar. Assuntos sem seleção incluem toda a disciplina.</p></div></aside>
    </form>}
  </section>;
}
