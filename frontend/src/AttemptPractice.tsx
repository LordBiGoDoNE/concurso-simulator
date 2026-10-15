import { useEffect, useRef, useState } from 'react';
import type { AttemptView } from './attempt-api';
import type { Content } from './question-api';
import { useAttemptRun } from './useAttemptRun';

export function AttemptText({ content }: { content: Content }) {
  return <><p className="question-text">{content.text}</p>{content.resources.map((r, i) => <p key={`${r.sha256}-${i}`}>Descrição da figura: {r.description}</p>)}</>;
}
export default function AttemptPractice({ initial, onUnavailable }: { initial: AttemptView; onUnavailable: () => void }) {
  const run = useAttemptRun(initial, onUnavailable); const [position, setPosition] = useState(0); const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => { heading.current?.focus(); }, [position]);
  const item = run.view.items[position]; const selection = run.selected(item);
  const answered = run.view.items.filter(i => run.selected(i) !== null).length;
  return <section aria-labelledby="attempt-practice-title">
    <h3 id="attempt-practice-title" ref={heading} tabIndex={-1}>Questão {position + 1} de {run.view.items.length}</h3>
    <p>{answered} de {run.view.items.length} respondidas nesta tela. As escolhas pendentes ainda não estão confirmadas.</p>
    <p role="status" aria-live="polite">{run.phase === 'pending' ? 'Salvando fila…' : run.phase === 'saved' ? 'Respostas confirmadas.' : run.phase === 'unsaved' ? 'Escolhas locais aguardam confirmação.' : 'Há escolhas sem salvamento confirmado.'}</p>
    {run.message && <p role="alert">{run.message}</p>}
    {run.phase === 'failed' && <button onClick={run.retry}>Repetir salvamento</button>}
    {(run.phase === 'failed' || run.phase === 'conflict') && <button onClick={() => void run.reload()}>Recarregar tentativa</button>}
    {run.phase === 'unsaved' && <button onClick={run.savePreserved}>Salvar escolhas preservadas</button>}
    <AttemptText content={item.statement} />
    <fieldset disabled={run.view.finishedAt !== null} className="attempt-options"><legend>Escolha uma alternativa</legend>
      {item.alternatives.map((a, i) => <label key={a.id}><input type="radio" name={`answer-${item.id}`} checked={selection === a.id} onChange={() => run.choose(item.id, a.id)} />
        <span>{String.fromCharCode(65 + i)}. <span className="question-text">{a.content.text}</span>{a.content.resources.map((r, j) => <span key={j}> Descrição da figura: {r.description}</span>)}</span></label>)}
      <button type="button" disabled={selection === null} onClick={() => run.choose(item.id, null)}>Limpar resposta</button>
    </fieldset>
    <nav aria-label="Navegação do simulado"><button disabled={position === 0} onClick={() => setPosition(v => v - 1)}>Questão anterior</button><button disabled={position === run.view.items.length - 1} onClick={() => setPosition(v => v + 1)}>Próxima questão</button></nav>
    <p>Versão confirmada: {run.view.version}. A correção só estará disponível após concluir.</p>
  </section>;
}
