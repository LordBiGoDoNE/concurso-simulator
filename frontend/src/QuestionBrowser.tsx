import { useEffect, useRef, useState } from 'react';
import { questionCatalog, questionDetail, questionList, type Catalog, type Content, type Detail, type Filters, type QuestionPage } from './question-api';

type Load<T> = { key: string; state: 'loading' | 'error' } | { key: string; state: 'ready'; value: T };
function Text({ content }: { content: Content }) {
  return <><p className="question-text">{content.text.split(/(\*\*[^*]+\*\*|_[^_]+_)/g).map((piece, i) => piece.startsWith('**') ? <strong key={i}>{piece.slice(2, -2)}</strong> : piece.startsWith('_') ? <em key={i}>{piece.slice(1, -1)}</em> : piece)}</p>{content.resources.map((r, i) => <p key={`${r.sha256}-${i}`}>Descrição da figura: {r.description}</p>)}</>;
}
export default function QuestionBrowser() {
  const [catalog, setCatalog] = useState<Load<Catalog>>({ key: '', state: 'loading' });
  const [catalogRetry, setCatalogRetry] = useState(0);
  const [filters, setFilters] = useState<Filters>({ disciplineId: '', topicId: '', roleId: '', page: 0 });
  const [list, setList] = useState<Load<QuestionPage>>({ key: '', state: 'loading' });
  const [detail, setDetail] = useState<Load<Detail>>({ key: '', state: 'loading' });
  const [selected, setSelected] = useState('');
  const [retry, setRetry] = useState(0);
  const [detailRetry, setDetailRetry] = useState(0);
  const heading = useRef<HTMLHeadingElement>(null);
  const selectionButton = useRef<HTMLButtonElement | null>(null);
  const resultsHeading = useRef<HTMLHeadingElement>(null);
  const focusResults = useRef(false);
  const queryKey = JSON.stringify(filters);

  useEffect(() => {
    const controller = new AbortController(); let active = true;
    setCatalog({ key: '', state: 'loading' });
    questionCatalog(controller.signal).then(value => { if (active) setCatalog({ key: '', state: 'ready', value }); })
      .catch(() => { if (active) setCatalog({ key: '', state: 'error' }); });
    return () => { active = false; controller.abort(); };
  }, [catalogRetry]);
  const catalogReady = catalog.state === 'ready';
  useEffect(() => {
    if (!catalogReady) return;
    const controller = new AbortController(); let active = true;
    setList({ key: queryKey, state: 'loading' });
    questionList(filters, controller.signal).then(value => { if (active) setList({ key: queryKey, state: 'ready', value }); })
      .catch(() => { if (active) setList({ key: queryKey, state: 'error' }); });
    return () => { active = false; controller.abort(); };
  }, [catalogReady, queryKey, retry]); // key contém todos os filtros; identidade não interfere na consulta pública.
  useEffect(() => {
    if (!selected) return;
    const controller = new AbortController(); let active = true;
    setDetail({ key: selected, state: 'loading' });
    questionDetail(selected, controller.signal).then(value => { if (active) setDetail({ key: selected, state: 'ready', value }); })
      .catch(() => { if (active) setDetail({ key: selected, state: 'error' }); });
    return () => { active = false; controller.abort(); };
  }, [selected, detailRetry]);
  useEffect(() => { if (selected) heading.current?.focus(); }, [selected]);
  useEffect(() => { if (focusResults.current && list.state === 'ready' && list.key === queryKey) { resultsHeading.current?.focus(); focusResults.current = false; } }, [list, queryKey]);
  function change(next: Partial<Filters>) { setSelected(''); setFilters(current => ({ ...current, ...next, page: next.page ?? 0 })); }
  const currentList = list.key === queryKey ? list : { state: 'loading' as const };
  const currentDetail = detail.key === selected ? detail : { state: 'loading' as const };
  return <section aria-labelledby="question-bank-title">
    <h2 id="question-bank-title">Banco de questões</h2>
    <p>Consulta livre, com ou sem login. Não responde, corrige ou salva tentativas.</p>
    {catalog.state === 'loading' && <p role="status">Carregando catálogo…</p>}
    {catalog.state === 'error' && <><p role="alert">Catálogo indisponível. As aulas continuam acessíveis.</p><button onClick={() => setCatalogRetry(v => v + 1)}>Tentar catálogo novamente</button></>}
    {catalog.state === 'ready' && <>
      <div className="question-filters">
        <label>Disciplina<select value={filters.disciplineId} onChange={e => change({ disciplineId: e.target.value, topicId: '' })}><option value="">Todas as disciplinas</option>{catalog.value.disciplines.map(d => <option key={d.id} value={d.id}>{d.title}</option>)}</select></label>
        <label>Assunto<select value={filters.topicId} onChange={e => change({ topicId: e.target.value })}><option value="">Todos os assuntos</option>{catalog.value.topics.filter(t => !filters.disciplineId || t.disciplineId === filters.disciplineId).map(t => <option key={t.id} value={t.id}>{t.title}</option>)}</select></label>
        <label>Cargo<select value={filters.roleId} onChange={e => change({ roleId: e.target.value })}><option value="">Ambos os cargos</option>{catalog.value.roles.map(r => <option key={r.id} value={r.id}>{r.title}</option>)}</select></label>
      </div>
      <h3 ref={resultsHeading} tabIndex={-1}>Resultados da consulta</h3>
      {currentList.state === 'loading' && <p role="status">Carregando questões…</p>}
      {currentList.state === 'error' && <><p role="alert">Consulta de questões indisponível. Não é um acervo vazio.</p><button onClick={() => setRetry(v => v + 1)}>Tentar questões novamente</button></>}
      {currentList.state === 'ready' && <>
        <p role="status">{currentList.value.total === 0 ? 'Nenhuma questão elegível para estes filtros.' : `${currentList.value.total} questões elegíveis. Página ${filters.page + 1}.`}</p>
        <ul className="question-list">{currentList.value.items.map(q => <li key={q.questionId}><button onClick={event => { selectionButton.current = event.currentTarget; setSelected(q.questionId); }}>Consultar: {q.statementStart}</button></li>)}</ul>
      </>}
      <nav aria-label="Paginação de questões"><button disabled={currentList.state !== 'ready' || filters.page === 0} onClick={() => { focusResults.current = true; change({ page: filters.page - 1 }); }}>Página anterior</button><button disabled={currentList.state !== 'ready' || (filters.page + 1) * currentList.value.size >= currentList.value.total} onClick={() => { focusResults.current = true; change({ page: filters.page + 1 }); }}>Próxima página</button></nav>
      {selected && <article aria-labelledby="question-detail-title">
        <h3 id="question-detail-title" ref={heading} tabIndex={-1}>Detalhe da questão</h3>
        <button onClick={() => { setSelected(''); selectionButton.current?.focus(); }}>Fechar detalhe</button>
        {currentDetail.state === 'loading' && <p role="status">Carregando detalhe…</p>}
        {currentDetail.state === 'error' && <><p role="alert">Detalhe indisponível ou questão não elegível.</p><button onClick={() => setDetailRetry(v => v + 1)}>Tentar detalhe novamente</button></>}
        {currentDetail.state === 'ready' && <>
          <Text content={currentDetail.value.statement} />
          <ol type="A">{currentDetail.value.alternatives.map(a => <li key={a.id}><Text content={a.content} /></li>)}</ol>
          <h4>Fontes para estudo</h4><ul>{currentDetail.value.references.map(r => <li key={`${r.name}-${r.url}`}><a href={r.url} rel="noopener noreferrer">{r.name}</a></li>)}</ul>
        </>}
      </article>}
    </>}
  </section>;
}
