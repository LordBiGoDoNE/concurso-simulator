import { useLayoutEffect, useRef, useState } from 'react';
import type { Outcome, Result } from './attempt-api';
import AttemptText from './AttemptText';
import { lessons } from './study-catalog';

/** Atalhos da revisão para o assunto de cada questão; só existem depois do encerramento. */
export interface TopicShortcuts { review: (topicId: string) => void; practice: (topicId: string) => void }
const outcomeLabel: Record<Outcome, string> = { correct: 'Correta', wrong: 'Errada', blank: 'Em branco' };
const outcomeIcon: Record<Outcome, string> = { correct: '✓', wrong: '✕', blank: '–' };
const share = (n: number, total: number) => total ? `${(n / total) * 100}%` : '0%';

export default function AttemptResult({ result, practice = false, shortcuts }: { result: Result; practice?: boolean; shortcuts?: TopicShortcuts }) {
  const heading = useRef<HTMLHeadingElement>(null); useLayoutEffect(() => heading.current?.focus(), []);
  const [onlyMissed, setOnlyMissed] = useState(false);
  const prefix = practice ? 'lesson-result' : 'attempt-result';
  const reviewed = result.items.map((correction, i) => {
    return { correction, outcome: correction.outcome, number: i + 1 };
  });
  const visible = onlyMissed ? reviewed.filter(r => r.outcome !== 'correct') : reviewed;
  const missed = result.wrong + result.blank;
  return <section className="attempt-result" aria-labelledby={`${prefix}-title`}>
    <header className="result-summary">
      <h3 id={`${prefix}-title`} tabIndex={-1} ref={heading}>{practice ? 'Resultado da prática' : 'Resultado do simulado'}</h3>
      <p className="result-score">{result.points}/{result.total} pontos — {result.percentage.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%.</p>
      <div className="result-bar" role="img" aria-label={`${result.correct} acertos, ${result.wrong} erros e ${result.blank} em branco de ${result.total} questões`}>
        {(['correct', 'wrong', 'blank'] as const).map(o => { const n = o === 'correct' ? result.correct : o === 'wrong' ? result.wrong : result.blank;
          return n > 0 && <span key={o} className={`result-bar-${o}`} style={{ width: share(n, result.total) }} />; })}
      </div>
      <ul className="result-legend" aria-label="Legenda do resultado">
        <li><span className="result-key result-key-correct" aria-hidden="true" /><strong>{result.correct}</strong> acertos</li>
        <li><span className="result-key result-key-wrong" aria-hidden="true" /><strong>{result.wrong}</strong> erros</li>
        <li><span className="result-key result-key-blank" aria-hidden="true" /><strong>{result.blank}</strong> em branco</li>
      </ul>
      <p className="result-note">Correção do servidor sobre as versões capturadas.</p>
    </header>
    <div className="result-filter" role="group" aria-label="Mostrar na revisão">
      <button type="button" aria-pressed={!onlyMissed} onClick={() => setOnlyMissed(false)}>Todas ({result.total})</button>
      <button type="button" aria-pressed={onlyMissed} onClick={() => setOnlyMissed(true)}>Erros e em branco ({missed})</button>
    </div>
    {onlyMissed && !missed && <p role="status">Nenhum erro ou questão em branco nesta realização.</p>}
    {visible.map(({ correction, outcome, number }) => {
      const selected = correction.item.alternatives.find(a => a.id === correction.item.selectedAlternativeId);
      const topicId = correction.item.classification.topicId; const lesson = lessons.find(t => t.id === topicId);
      return <article key={correction.item.id} className={`result-item result-item-${outcome}`}>
        <header className="result-item-head"><h4>Revisão da questão {number}</h4><p className={`result-outcome result-outcome-${outcome}`}><span aria-hidden="true">{outcomeIcon[outcome]}</span> {outcomeLabel[outcome]}</p></header>
        <AttemptText content={correction.item.statement} />
        <h5>Sua escolha</h5>{selected ? <AttemptText content={selected.content} /> : <p>Em branco.</p>}
        <h5>Resposta correta</h5><AttemptText content={correction.correctAnswer} />
        <h5>Resolução</h5><AttemptText content={correction.explanation} />
        <h5>Fontes para estudo</h5><ul>{correction.item.references.map((r, j) => <li key={j}><a href={r.url} rel="noopener noreferrer">{r.name}</a></li>)}</ul>
        {shortcuts && lesson && <div className="result-shortcuts">
          <button className="secondary" onClick={() => shortcuts.review(lesson.id)} aria-label={`Rever a aula ${lesson.id}, questão ${number}`}>Rever a aula <span aria-hidden="true">{lesson.id}</span></button>
          <button className="secondary" onClick={() => shortcuts.practice(lesson.id)} aria-label={`Praticar este assunto, ${lesson.id}, questão ${number}`}>Praticar este assunto</button>
        </div>}
      </article>;
    })}
  </section>;
}
