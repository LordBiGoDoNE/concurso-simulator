import type { ReviewSummary } from './subject-review-api';
const date = (value: string) => new Date(value).toLocaleString('pt-BR');

export default function SubjectReviewSummary({ summary, individual = false }: { summary: ReviewSummary; individual?: boolean }) {
  const hasData = summary.subjects.some(m => m.assessed > 0);
  return <>
    <p>{individual ? 'Somente esta tentativa concluída; não reúne outras tentativas.' : 'Última observação de cada questão-base na janela. Revisões e variantes da mesma base contam uma única vez; exposições incluem repetições. O resumo pode diminuir e não representa todo o histórico.'}</p>
    {!hasData && <p role="status">Sem dados para este cargo. Nenhuma dificuldade foi inferida.</p>}
    <h4>Prioridades de revisão</h4>
    <p>Heurística: ao menos {summary.minimumBases} bases, com erros ou brancos. Ordem pela proporção exata de erros e brancos, depois quantidade e assunto. Não indica domínio do conteúdo nem prevê aprovação.</p>
    {summary.priorityTopicIds.length ? <ol>{summary.priorityTopicIds.map(id => { const m = summary.subjects.find(m => m.topicId === id)!;return <li key={id}>{m.title}: {m.wrong + m.blank} erros ou brancos em {m.assessed} bases.</li>; })}</ol> : <p>Nenhuma prioridade nesta amostra.</p>}
    <p>Assuntos com menos de {summary.minimumBases} bases têm poucos dados. A tabela pode ser percorrida horizontalmente; com foco nela, use as setas.</p>
    <div className="subject-review-table" role="region" aria-label={individual ? 'Tabela de revisão deste simulado' : 'Tabela de desempenho por assunto'} tabIndex={0}>
      <table><caption>{individual ? 'Desempenho dos 51 assuntos nesta tentativa' : 'Desempenho dos 51 assuntos na janela'}</caption>
        <thead><tr>{['Disciplina e assunto', 'Acertos', 'Erros', 'Em branco', 'Bases', 'Exposições', 'Acerto (%)', 'Última conclusão', 'Amostra'].map(title => <th key={title} scope="col">{title}</th>)}</tr></thead>
        <tbody>{summary.subjects.map(m => <tr key={m.topicId}><th scope="row">{m.disciplineTitle}<br />{m.topicId} — {m.title}</th>
          <td>{m.correct}</td><td>{m.wrong}</td><td>{m.blank}</td><td>{m.assessed}</td><td>{m.exposures}</td>
          <td>{m.percentage === null ? '—' : m.percentage.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
          <td>{m.lastFinishedAt === null ? '—' : <time dateTime={m.lastFinishedAt}>{date(m.lastFinishedAt)}</time>}</td><td>{m.assessed === 0 ? 'Sem dados' : m.assessed < summary.minimumBases ? 'Poucos dados' : `${m.assessed} bases`}</td></tr>)}</tbody>
      </table>
    </div>
  </>;
}
