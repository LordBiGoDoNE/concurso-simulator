import { useEffect, useRef, useState } from 'react';
import catalog from './lesson-catalog.json';
import paths from './lesson-paths.json';
import { lessonUrl, materialUrl } from './lesson-links';
import type { DirectedPractice } from './directed-practice';

const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR');
const lessons = catalog.topics.filter(topic => Object.hasOwn(paths, topic.id));
export default function LessonLibrary({ initialDiscipline = '', onPractice }: { initialDiscipline?: string; onPractice: (selection: DirectedPractice) => void }) {
  const [discipline, setDiscipline] = useState(initialDiscipline); const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(''); const heading = useRef<HTMLHeadingElement>(null);
  const [role, setRole] = useState('assistente-administrativo');
  const [frameState, setFrameState] = useState<'loading' | 'loaded' | 'failed'>('loading');
  const [frameRetry, setFrameRetry] = useState(0); const frameTimer = useRef<number | undefined>(undefined);
  useEffect(() => { setDiscipline(initialDiscipline); setSelected(''); }, [initialDiscipline]);
  const topic = lessons.find(t => t.id === selected);
  const neighbors = lessons.filter(t => t.disciplineId === topic?.disciplineId); const index = neighbors.findIndex(t => t.id === selected);
  useEffect(() => { if (!selected) return; setFrameState('loading'); heading.current?.focus(); frameTimer.current = window.setTimeout(() => setFrameState('failed'), 12000); return () => window.clearTimeout(frameTimer.current); }, [selected, frameRetry]);
  function read(id: string) { setSelected(id); }
  const disciplineTitle = (id: string) => catalog.disciplines.find(d => d.id === id)?.title || id;
  if (topic) {
    const url = lessonUrl(topic.id);
    const roles = catalog.roles.filter(r => topic.disciplineId === 'conhecimentos-escolares' ? r.id === 'secretario-de-escola' : topic.disciplineId === 'conhecimentos-administrativos' ? r.id === 'assistente-administrativo' : true);
    const chosenRole = roles.some(r => r.id === role) ? role : roles[0].id;
    return <section className="lesson-reader" aria-labelledby="lesson-reader-title">
      <button className="secondary" onClick={() => { setSelected(''); window.setTimeout(() => heading.current?.focus(), 0); }}>← Voltar às aulas</button>
      <header className="page-intro"><p className="eyebrow">{disciplineTitle(topic.disciplineId)} · {topic.id}</p><h1 id="lesson-reader-title" tabIndex={-1} ref={heading}>{topic.title}</h1><p>Teoria completa, exemplos e exercícios do material de estudo.</p></header>
      <div className="reader-layout"><div className="lesson-document">
        {url ? <><div className="lesson-document-toolbar"><a href={url} target="_blank" rel="noopener noreferrer">Abrir aula original (nova aba)</a><span role="status">{frameState === 'loading' ? 'Carregando aula…' : frameState === 'failed' ? 'A aula ainda não apareceu? Tente novamente ou abra o original.' : 'Se o conteúdo não aparecer abaixo, abra a aula original.'}</span>{frameState === 'failed' && <button className="secondary" onClick={() => setFrameRetry(v => v + 1)}>Tentar carregar aula novamente</button>}</div>
          <iframe key={`${topic.id}:${frameRetry}`} title={`Aula completa: ${topic.title}`} src={url} sandbox="allow-same-origin allow-popups allow-popups-to-escape-sandbox" referrerPolicy="no-referrer" onLoad={() => { window.clearTimeout(frameTimer.current); setFrameState('loaded'); }} onError={() => { window.clearTimeout(frameTimer.current); setFrameState('failed'); }} />
        </> : <p role="alert">Aula indisponível neste endereço. <a href={materialUrl}>Acessar material completo</a></p>}
      </div><aside className="reader-index"><h2>Nesta disciplina</h2><nav aria-label="Aulas da disciplina">{neighbors.map(t => <button key={t.id} aria-current={selected === t.id ? 'page' : undefined} onClick={() => read(t.id)}><small>{t.id}</small> {t.title}</button>)}</nav>
        <div className="lesson-practice"><h2>Pratique a aula selecionada</h2><p id="lesson-practice-subject"><strong>Assunto da prática: {topic.id} — {topic.title}</strong></p><p>Os links dentro do material não alteram essa seleção. Para mudar o assunto da prática, escolha outra aula nesta página.</p><label htmlFor="lesson-role">Cargo para praticar esta aula</label><select id="lesson-role" value={chosenRole} onChange={e => setRole(e.target.value)}>{roles.map(r => <option key={r.id} value={r.id}>{r.title}</option>)}</select><button aria-describedby="lesson-practice-subject" onClick={() => onPractice({ roleId: chosenRole, disciplineId: topic.disciplineId, topicId: topic.id })}>Praticar {topic.id}</button></div>
      </aside></div><nav className="lesson-pagination" aria-label="Navegação entre aulas"><button className="secondary" disabled={index <= 0} onClick={() => read(neighbors[index - 1].id)}>Aula anterior</button><button className="secondary" disabled={index >= neighbors.length - 1} onClick={() => read(neighbors[index + 1].id)}>Próxima aula</button></nav>
    </section>;
  }
  const query = normalize(search.trim());
  const results = lessons.filter(t => (!discipline || t.disciplineId === discipline) && normalize(`${t.id} ${t.title} ${disciplineTitle(t.disciplineId)}`).includes(query));
  return <section className="lesson-library" aria-labelledby="lesson-library-title"><header className="page-intro"><p className="eyebrow">Aprenda no seu ritmo</p><h1 id="lesson-library-title" ref={heading} tabIndex={-1}>Suas aulas, em um só lugar</h1><p>{lessons.length} assuntos para entender, revisar e colocar em prática. Escolha por onde começar.</p></header>
    <div className="lesson-disciplines">{catalog.disciplines.map((d, i) => <button key={d.id} aria-pressed={discipline === d.id} onClick={() => setDiscipline(discipline === d.id ? '' : d.id)}><span className="discipline-icon" aria-hidden="true">{String(i + 1).padStart(2, '0')}</span><strong>{d.title}</strong><small>{lessons.filter(t => t.disciplineId === d.id).length} aulas</small></button>)}</div>
    <div className="library-layout"><div><div className="lesson-filters"><label htmlFor="lesson-search">Buscar aula<input id="lesson-search" type="search" placeholder="Qual assunto você quer estudar?" value={search} onChange={e => setSearch(e.target.value)} /></label><label htmlFor="lesson-discipline">Filtrar disciplina<select id="lesson-discipline" value={discipline} onChange={e => setDiscipline(e.target.value)}><option value="">Todas as disciplinas</option>{catalog.disciplines.map(d => <option key={d.id} value={d.id}>{d.title}</option>)}</select></label></div>
      <p role="status">{results.length} {results.length === 1 ? 'aula encontrada' : 'aulas encontradas'}.</p>
      {!results.length && <div className="library-empty"><h2>Nenhuma aula por aqui</h2><p>Tente outro assunto ou remova os filtros.</p><button className="secondary" onClick={() => { setSearch(''); setDiscipline(''); }}>Limpar busca e filtros</button></div>}
      <ul className="lesson-list">{results.map(t => <li key={t.id}><button onClick={() => read(t.id)}><span className="lesson-code">{t.id}</span><span><strong>{t.title}</strong><small>{disciplineTitle(t.disciplineId)}</small></span><span className="discipline-arrow" aria-hidden="true">→</span></button></li>)}</ul>
    </div><aside className="study-guide"><h2>Um assunto de cada vez</h2><p>Explore a teoria, acompanhe os exemplos e use os exercícios para conferir seu entendimento.</p><p className="guest-note">As aulas estão disponíveis sem login. Para treinar um assunto, abra a aula e confira o código do assunto antes de praticar.</p><a href={materialUrl}>Acessar todo o material</a></aside></div>
  </section>;
}
