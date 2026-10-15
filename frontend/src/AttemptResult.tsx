import { useEffect, useRef } from 'react';
import type { Result } from './attempt-api';
import AttemptText from './AttemptText';

export default function AttemptResult({ result }: { result: Result }) {
  const heading = useRef<HTMLHeadingElement>(null); useEffect(() => heading.current?.focus(), []);
  return <section aria-labelledby="attempt-result-title">
    <h3 id="attempt-result-title" tabIndex={-1} ref={heading}>Resultado do simulado</h3>
    <p>{result.points}/{result.total} pontos — {result.percentage.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%.</p>
    <p>{result.correct} acertos, {result.wrong} erros e {result.blank} em branco. Correção do servidor sobre as versões capturadas.</p>
    {result.items.map((correction, i) => <article key={correction.item.id}>
      <h4>Revisão da questão {i + 1}</h4><AttemptText content={correction.item.statement} />
      <p>Sua escolha: {correction.item.alternatives.find(a => a.id === correction.item.selectedAlternativeId)?.content.text || 'Em branco.'}</p>
      <p>Resposta correta: {correction.correctAnswer.text}</p>
      <h5>Resolução</h5><AttemptText content={correction.explanation} />
      <h5>Fontes para estudo</h5><ul>{correction.item.references.map((r, j) => <li key={j}><a href={r.url} rel="noopener noreferrer">{r.name}</a></li>)}</ul>
    </article>)}
  </section>;
}
