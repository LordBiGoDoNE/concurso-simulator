import { useEffect, useRef, useState } from 'react';
import { questionCatalog, type Catalog } from './question-api';
import { AttemptApiError, createAttempt, type AttemptView, type Composition } from './attempt-api';

export default function AttemptComposer({ onCreated, onUnavailable }: { onCreated: (v: AttemptView) => void; onUnavailable: () => void }) {
  const [catalog, setCatalog] = useState<Catalog | null>(null); const [catalogFailed, setCatalogFailed] = useState(false); const [retry, setRetry] = useState(0);
  const [role, setRole] = useState(''); const [quantities, setQuantities] = useState<Record<string, string>>({}); const [topics, setTopics] = useState<Record<string, string[]>>({});
  const [useParameterizedQuestions, setUseParameterizedQuestions] = useState(false);
  const [busy, setBusy] = useState(false); const [message, setMessage] = useState(''); const pending = useRef(false);
  const operation = useRef<{ key: string; id: string } | null>(null); const lifetime = useRef<AbortController | null>(null);
  useEffect(() => { lifetime.current = new AbortController(); return () => lifetime.current?.abort(); }, []);
  useEffect(() => {
    const controller = new AbortController(); let active = true; setCatalogFailed(false);
    questionCatalog(controller.signal).then(c => { if (active) { setCatalog(c); setRole(c.roles[0]?.id || ''); setQuantities({ [c.disciplines[0]?.id || '']: '5' }); } }).catch(() => { if (active) setCatalogFailed(true); });
    return () => { active = false; controller.abort(); };
  }, [retry]);
  const portions = (catalog?.disciplines || []).map(d => ({ disciplineId: d.id, quantity: Number(quantities[d.id] || 0), topicIds: topics[d.id] || [] })).filter(d => d.quantity !== 0);
  const total = portions.reduce((n, d) => n + d.quantity, 0); const valid = !!role && Number.isInteger(total) && total >= 1 && total <= 100 && portions.every(p => Number.isInteger(p.quantity) && p.quantity > 0 && p.quantity <= 100);
  async function submit() {
    if (pending.current || !valid || !lifetime.current) return;
    const controller = lifetime.current;
    pending.current = true; setBusy(true); setMessage('');
    const composition: Composition = { roleId: role, disciplines: portions, useParameterizedQuestions }; const key = JSON.stringify(composition);
    if (operation.current?.key !== key) operation.current = { key, id: crypto.randomUUID() };
    try { const value = await createAttempt(composition, operation.current.id, controller.signal); if (!controller.signal.aborted) onCreated(value); }
    catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof AttemptApiError && [401, 403].includes(error.status)) { onUnavailable(); return; }
      setMessage(error instanceof AttemptApiError && error.available.length ? `Acervo insuficiente: ${error.available.map(a => `${catalog?.disciplines.find(d => d.id === a.disciplineId)?.title || a.disciplineId}: ${a.available} disponíveis`).join('; ')}.` : 'Criação não confirmada. Tente novamente com os mesmos critérios para recuperar a tentativa, se ela foi gravada.');
    } finally { pending.current = false; if (!controller.signal.aborted) setBusy(false); }
  }
  return <section aria-labelledby="compose-attempt-title">
    <h3 id="compose-attempt-title">Compor simulado</h3>
    <p>Escolha de 1 a 100 questões. Distribuição livre e um ponto por acerto; não são regras oficiais do edital.</p>
    {!catalog && !catalogFailed && <p role="status">Carregando composição…</p>}
    {catalogFailed && <><p role="alert">Composição indisponível. As aulas continuam acessíveis.</p><button onClick={() => setRetry(v => v + 1)}>Tentar composição novamente</button></>}
    {catalog && <form onSubmit={e => { e.preventDefault(); void submit(); }}>
      <label htmlFor="attempt-role">Cargo do simulado</label><select id="attempt-role" value={role} disabled={busy} onChange={e => setRole(e.target.value)}>{catalog.roles.map(r => <option key={r.id} value={r.id}>{r.title}</option>)}</select>
      <div className="attempt-portions">{catalog.disciplines.map(d => <fieldset key={d.id} disabled={busy}>
        <legend>{d.title}</legend><label htmlFor={`quantity-${d.id}`}>Quantidade de {d.title}</label>
        <input id={`quantity-${d.id}`} type="number" min="0" max="100" step="1" value={quantities[d.id] || ''} onChange={e => setQuantities(v => ({ ...v, [d.id]: e.target.value }))} />
        <details><summary>Assuntos de {d.title} (opcional)</summary>{catalog.topics.filter(t => t.disciplineId === d.id).map(t => <label className="attempt-topic" key={t.id}><input type="checkbox" checked={(topics[d.id] || []).includes(t.id)} onChange={e => setTopics(v => ({ ...v, [d.id]: e.target.checked ? [...(v[d.id] || []), t.id] : (v[d.id] || []).filter(id => id !== t.id) }))} />{t.title}</label>)}</details>
      </fieldset>)}</div>
      <label htmlFor="attempt-variations"><input id="attempt-variations" type="checkbox" checked={useParameterizedQuestions} disabled={busy}
        aria-describedby="attempt-variations-help" onChange={e => setUseParameterizedQuestions(e.target.checked)} />Usar variações numéricas quando disponíveis</label>
      <p id="attempt-variations-help">Pode misturar questões fixas e variações de regra de três direta, porcentagem e juros simples. Os valores podem se repetir entre simulados; não há garantia de questão nova.</p>
      <p role="status">Total: {total} questões. {!valid && 'Informe quantidades inteiras que somem de 1 a 100.'}</p>
      {message && <p role="alert">{message}</p>}
      <button disabled={!valid || busy} type="submit">{busy ? 'Criando simulado…' : message ? 'Tentar criar novamente' : 'Iniciar simulado'}</button>
    </form>}
  </section>;
}
