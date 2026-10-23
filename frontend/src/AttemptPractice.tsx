import { useEffect, useRef, useState } from 'react';
import type { AttemptView } from './attempt-api';
import { useAttemptRun } from './useAttemptRun';
import ConfirmAttemptFinish from './ConfirmAttemptFinish';
import AttemptText from './AttemptText';
import AttemptResult from './AttemptResult';
import IndividualSubjectReview from './IndividualSubjectReview';


import type { DirectedPractice } from './directed-practice';

export default function AttemptPractice({ initial, onUnavailable, onFinished, reviewActive = true, onPractice }: { reviewActive?: boolean; initial: AttemptView; onUnavailable: () => void; onFinished?: () => void; onPractice?: (selection: DirectedPractice) => void }) {
  const run = useAttemptRun(initial, onUnavailable); const [position, setPosition] = useState(0); const heading = useRef<HTMLHeadingElement>(null);
  const [confirm, setConfirm] = useState(false); const finishButton = useRef<HTMLButtonElement>(null);
  const wasConfirming = useRef(false);
  useEffect(() => { if (wasConfirming.current && !confirm) finishButton.current?.focus(); wasConfirming.current = confirm; }, [confirm]);
  useEffect(() => { heading.current?.focus(); }, [position]);
  useEffect(() => { if (run.view.finishedAt !== null) onFinished?.(); }, [run.view.finishedAt, onFinished]);
  const item = run.view.items[position]; const selection = run.selected(item);
  const answered = run.view.items.filter(i => run.selected(i) !== null).length;
  if (run.view.finishedAt !== null) return <>
    {run.message && <p role="alert">{run.message}</p>}
    {run.result ? <AttemptResult result={run.result} /> : <p role="status">Carregando resultado do servidor…</p>}
    {run.result && <IndividualSubjectReview attemptId={run.view.id} roleId={run.view.roleId} active={reviewActive} onPractice={onPractice} onUnavailable={onUnavailable} />}
    {run.resultFailed && <><p role="alert">Resultado indisponível. A tentativa continua concluída no servidor.</p><button onClick={() => void run.loadResult()}>Tentar resultado novamente</button></>}
  </>;
  return <section className="practice" aria-labelledby="attempt-practice-title">
    <header className="practice-header"><div><p className="eyebrow">Um passo de cada vez</p><p>{answered} de {run.view.items.length} respondidas nesta tela. As escolhas pendentes ainda não estão confirmadas.</p></div><div className="practice-progress"><label htmlFor="attempt-progress">Questões respondidas</label><progress id="attempt-progress" value={answered} max={run.view.items.length} /></div></header>
    <div className="practice-layout"><div className="question-card">
    <h3 id="attempt-practice-title" ref={heading} tabIndex={-1}>Questão {position + 1} de {run.view.items.length}</h3>
    <p className={`save-status save-${run.phase}`} role="status" aria-live="polite">{run.phase === 'pending' ? 'Salvando fila…' : run.phase === 'saved' ? 'Respostas confirmadas.' : run.phase === 'unsaved' ? 'Escolhas locais aguardam confirmação.' : 'Há escolhas sem salvamento confirmado.'}</p>
    {run.message && <p role="alert">{run.message}</p>}
    {run.phase === 'failed' && <button onClick={run.retry}>Repetir salvamento</button>}
    {(run.phase === 'failed' || run.phase === 'conflict' || run.finishFailed) && <button onClick={() => void run.reload()}>Recarregar tentativa</button>}
    {run.phase === 'unsaved' && <button onClick={run.savePreserved}>Salvar escolhas preservadas</button>}
    <AttemptText content={item.statement} />
    <fieldset disabled={run.controlsLocked} className="attempt-options"><legend>Escolha uma alternativa</legend>
      {item.alternatives.map((a, i) => <label className={selection === a.id ? 'selected' : undefined} key={a.id}><input type="radio" name={`answer-${item.id}`} checked={selection === a.id} onChange={() => run.choose(item.id, a.id)} />
        <span className="option-letter" aria-hidden="true">{String.fromCharCode(65 + i)}</span><span><span className="sr-only">{String.fromCharCode(65 + i)}. </span><span className="question-text">{a.content.text}</span>{a.content.resources.map((r, j) => <span key={j}> Descrição da figura: {r.description}</span>)}</span></label>)}
      <button className="secondary" type="button" disabled={selection === null} onClick={() => run.choose(item.id, null)}>Limpar resposta</button>
    </fieldset>
    <nav aria-label="Navegação do simulado"><button disabled={position === 0} onClick={() => setPosition(v => v - 1)}>Questão anterior</button><button disabled={position === run.view.items.length - 1} onClick={() => setPosition(v => v + 1)}>Próxima questão</button></nav>
    <p>Versão confirmada: {run.view.version}. A correção só estará disponível após concluir.</p>
    </div><aside className="question-map" aria-labelledby="question-map-title"><h2 id="question-map-title">Seu progresso</h2><p>Escolha uma questão para navegar.</p><nav aria-label="Mapa de questões">{run.view.items.map((question, index) => <button key={question.id} className={run.selected(question) !== null ? 'answered' : undefined} aria-current={position === index ? 'step' : undefined} aria-label={`Ir para questão ${index + 1}: ${run.selected(question) !== null ? 'respondida' : 'sem resposta'}`} onClick={() => setPosition(index)}>{index + 1}</button>)}</nav><p><span className="map-key" aria-hidden="true" /> Respondida <span className="map-key blank" aria-hidden="true" /> Sem resposta</p><p>O mapa indica escolhas, sem antecipar a correção.</p>
    {run.finishFailed ? <button disabled={run.finishing} onClick={() => void run.finish()}>Tentar concluir novamente</button>
      : <button ref={finishButton} disabled={!run.canFinish || run.finishing} onClick={() => setConfirm(true)}>{run.finishing ? 'Concluindo…' : 'Concluir simulado'}</button>}
    </aside></div>
    {confirm && <ConfirmAttemptFinish answered={answered} total={run.view.items.length} onCancel={() => setConfirm(false)} onConfirm={() => { setConfirm(false); void run.finish(); }} />}
  </section>;
}
