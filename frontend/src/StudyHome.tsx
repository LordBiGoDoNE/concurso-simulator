import { useEffect, useState } from 'react';
import { questionCatalog, questionList, type Catalog } from './question-api';

export default function StudyHome({ onCompose, onLessons }: { onCompose: () => void; onLessons: (discipline?: string) => void }) {
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [total, setTotal] = useState<number | null>(null);
  const [failed, setFailed] = useState(false); const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController(); setFailed(false);
    questionCatalog(controller.signal).then(value => { if (!controller.signal.aborted) setCatalog(value); })
      .catch(() => { if (!controller.signal.aborted) setFailed(true); });
    questionList({ disciplineId: '', topicId: '', roleId: '', page: 0 }, controller.signal)
      .then(value => { if (!controller.signal.aborted) setTotal(value.total); })
      .catch(() => { if (!controller.signal.aborted) setTotal(null); });
    return () => controller.abort();
  }, [retry]);
  return <>
    <section className="welcome" aria-labelledby="welcome-title">
      <div><p className="eyebrow">Seu próximo passo começa aqui</p>
        <h1 id="welcome-title">Mais clareza.<br />Mais preparo.</h1>
        <p className="intro">Estude por assunto, coloque seus conhecimentos em prática e encontre o seu ritmo de preparação.</p>
        <div className="actions"><button onClick={onCompose}>Montar um simulado <span aria-hidden="true">→</span></button><button className="secondary" onClick={() => onLessons()}>Explorar as aulas</button></div>
        <p className="home-facts">{total !== null && <span>{total} questões no acervo</span>}{catalog && <><span>{catalog.topics.length} assuntos</span><span>{catalog.roles.length} cargos</span></>}</p>
      </div>
      <div className="study-visual" aria-hidden="true"><div className="study-paper"><small>UM PASSO DE CADA VEZ</small><p>Conhecimento que vira preparo.</p><div className="paper-line" /><div className="paper-line short" /><div className="paper-option">A <i /></div><div className="paper-option selected">B <i /></div><div className="paper-option">C <i /></div></div><span className="visual-caption">Estude. Pratique. Evolua.</span></div>
    </section>
    <div className="home-lower"><section className="home-subjects" aria-labelledby="home-subjects-title"><p className="eyebrow">Explore o conteúdo</p><h2 id="home-subjects-title">O que vamos estudar?</h2><p className="muted">Escolha uma disciplina para começar pelas aulas.</p>
      {!catalog && !failed && <p role="status">Carregando disciplinas…</p>}
      {failed && <><p role="status">Não foi possível consultar as disciplinas. Você ainda pode acessar as aulas.</p><button className="secondary" onClick={() => setRetry(v => v + 1)}>Atualizar disciplinas</button></>}
      <div className="discipline-list">{catalog?.disciplines.map((d, i) => <button className="discipline-link" key={d.id} onClick={() => onLessons(d.id)}><span className="discipline-icon" aria-hidden="true">{String(i + 1).padStart(2, '0')}</span><span><strong>{d.title}</strong><small>{catalog.topics.filter(t => t.disciplineId === d.id).length} assuntos para explorar</small></span><span className="discipline-arrow" aria-hidden="true">→</span></button>)}</div>
    </section><aside className="study-guide" aria-labelledby="guide-title"><h2 id="guide-title">Um caminho para estudar</h2><ol><li><strong>Entenda o assunto</strong><p>Consulte a teoria e os exemplos das aulas.</p></li><li><strong>Coloque em prática</strong><p>Monte um simulado com os assuntos que quer treinar.</p></li><li><strong>Revise com atenção</strong><p>Após concluir, confira as resoluções e volte ao que precisa.</p></li></ol><p className="guest-note">Você pode estudar sem login. Com a conta, suas respostas confirmadas ficam disponíveis para retomar.</p></aside></div>
  </>;
}
