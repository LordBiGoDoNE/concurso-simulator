import { useLayoutEffect, useRef } from 'react';
import type { Result } from './attempt-api';
import AttemptText from './AttemptText';

export default function AttemptResult({ result, practice = false }: { result: Result; practice?: boolean }) {
  const heading = useRef<HTMLHeadingElement>(null); useLayoutEffect(() => heading.current?.focus(), []);
  return <section aria-labelledby={practice ? "lesson-result-title" : "attempt-result-title"}>
    <h3 id={practice ? "lesson-result-title" : "attempt-result-title"} tabIndex={-1} ref={heading}>{practice ? "Resultado da prática" : "Resultado do simulado"}</h3>
    <p>{result.points}/{result.total} pontos — {result.percentage.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%.</p>
    <p>{result.correct} acertos, {result.wrong} erros e {result.blank} em branco. Correção do servidor sobre as versões capturadas.</p>
    {result.items.map((correction, i) => { const selected = correction.item.alternatives.find(a => a.id === correction.item.selectedAlternativeId);return <article key={correction.item.id}>
      <h4>Revisão da questão {i + 1}</h4><AttemptText content={correction.item.statement} />
      <h5>Sua escolha</h5>{selected ? <AttemptText content={selected.content} /> : <p>Em branco.</p>}
      <h5>Resposta correta</h5><AttemptText content={correction.correctAnswer} />
      <h5>Resolução</h5><AttemptText content={correction.explanation} />
      <h5>Fontes para estudo</h5><ul>{correction.item.references.map((r, j) => <li key={j}><a href={r.url} rel="noopener noreferrer">{r.name}</a></li>)}</ul>
    </article>;})}
  </section>;
}
