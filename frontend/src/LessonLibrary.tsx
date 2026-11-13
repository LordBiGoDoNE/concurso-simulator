import './styles/lessons.css';
import { useLayoutEffect, useRef, useState } from 'react';
import { studyCatalog as catalog, lessons, lessonSupportsRole } from './study-catalog';
import NativeLesson from './NativeLesson';
import { emptyLessonLocation, locationForRole, type LessonLocation } from './study-navigation';

const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR');
interface Props {
  initialDiscipline?: string;
  location?: LessonLocation;
  onLocationChange?: (value: LessonLocation, replace?: boolean) => void;
  showPagination?: boolean;
}
export default function LessonLibrary({ initialDiscipline = '', location, onLocationChange, showPagination = true }: Props) {
  const [local, setLocal] = useState<LessonLocation>({ ...emptyLessonLocation, disciplineId: initialDiscipline });
  const value = location ?? local;
  const { disciplineId: discipline, search, topicId: selected, roleId: role } = value;
  function change(patch: Partial<LessonLocation>, replace = false) {
    const next = { ...value, ...patch };
    if (onLocationChange) onLocationChange(next, replace); else setLocal(next);
  }
  const heading = useRef<HTMLHeadingElement>(null);
  const previousSelection = useRef(selected);
  const topic = lessons.find(t => t.id === selected);
  const eligible = lessons.filter(t => lessonSupportsRole(t, role));
  const disciplines = catalog.disciplines.filter(d => eligible.some(t => t.disciplineId === d.id));
  const neighbors = eligible.filter(t => t.disciplineId === topic?.disciplineId);
  useLayoutEffect(() => {
    if (selected || previousSelection.current) heading.current?.focus({ preventScroll: true });
    if (selected && selected !== previousSelection.current) window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
    previousSelection.current = selected;
  }, [selected]);
  const disciplineTitle = (id: string) => catalog.disciplines.find(d => d.id === id)?.title || id;
  if (topic) {
    return <section className="lesson-reader" aria-labelledby="lesson-reader-title">
      <button className="secondary" onClick={() => change({ topicId: '' })}>← Voltar às aulas</button>
      <aside className="reader-index"><h2>Nesta Disciplina - {disciplineTitle(topic.disciplineId)}</h2><nav aria-label="Aulas da disciplina">{neighbors.map(t => <button key={t.id} aria-current={selected === t.id ? 'page' : undefined} onClick={() => change({ topicId: t.id })}><small>{t.id}</small> {t.title}</button>)}</nav></aside>
      <header className="page-intro"><p className="eyebrow">{disciplineTitle(topic.disciplineId)} · {topic.id}</p><h1 id="lesson-reader-title" tabIndex={-1} ref={heading}>{topic.title}</h1><p>Teoria, exemplos resolvidos e fontes para estudar este assunto.</p></header>
      <div className="reader-layout"><NativeLesson topicId={topic.id} /></div>{showPagination && <LessonPagination topicId={topic.id} roleId={role} onSelect={topicId => change({ topicId })} />}
    </section>;
  }
  const query = normalize(search.trim());
  const results = eligible.filter(t => (!discipline || t.disciplineId === discipline) && normalize(t.id + ' ' + t.title + ' ' + disciplineTitle(t.disciplineId)).includes(query));
  return <section className="lesson-library" aria-labelledby="lesson-library-title">
    <header className="page-intro"><p className="eyebrow">Aprenda no seu ritmo</p><h1 id="lesson-library-title" ref={heading} tabIndex={-1}>Suas aulas, em um só lugar</h1><p>{eligible.length} assuntos para entender, revisar e colocar em prática. Escolha por onde começar.</p></header>
    <div className="lesson-role-filter"><label htmlFor="library-role">Cargo para encontrar aulas</label><select id="library-role" value={role} onChange={e => change(locationForRole(value, e.target.value))}><option value="">Todos os cargos</option>{catalog.roles.map(r => <option key={r.id} value={r.id}>{r.title}</option>)}</select></div>
    <div className="lesson-disciplines">{disciplines.map((d, i) => <button key={d.id} aria-pressed={discipline === d.id} onClick={() => change({ disciplineId: discipline === d.id ? '' : d.id })}><span className="discipline-icon" aria-hidden="true">{String(i + 1).padStart(2, '0')}</span><strong>{d.title}</strong><small>{eligible.filter(t => t.disciplineId === d.id).length} aulas</small></button>)}</div>
    <div className="library-layout"><div><div className="lesson-filters"><label htmlFor="lesson-search">Buscar aula<input id="lesson-search" type="search" maxLength={160} placeholder="Qual assunto você quer estudar?" value={search} onChange={e => change({ search: e.target.value }, true)} /></label><label htmlFor="lesson-discipline">Filtrar disciplina<select id="lesson-discipline" value={discipline} onChange={e => change({ disciplineId: e.target.value })}><option value="">Todas as disciplinas</option>{disciplines.map(d => <option key={d.id} value={d.id}>{d.title}</option>)}</select></label></div>
      <p role="status">{results.length} {results.length === 1 ? 'aula encontrada' : 'aulas encontradas'}.</p>
      {!results.length && <div className="library-empty"><h2>Nenhuma aula por aqui</h2><p>Tente outro assunto ou remova os filtros.</p><button className="secondary" onClick={() => change({ search: '', disciplineId: '' })}>Limpar busca e filtros</button></div>}
      <ul className="lesson-list">{results.map(t => <li key={t.id}><button onClick={() => change({ topicId: t.id })}><span className="lesson-code">{t.id}</span><span><strong>{t.title}</strong><small>{disciplineTitle(t.disciplineId)}</small></span><span className="discipline-arrow" aria-hidden="true">→</span></button></li>)}</ul>
    </div><aside className="study-guide"><h2>Um assunto de cada vez</h2><p>Explore a teoria, acompanhe os exemplos e use os exercícios para conferir seu entendimento.</p><p className="guest-note">As aulas estão disponíveis sem login. Para treinar um assunto, abra a aula e confira o código do assunto antes de praticar.</p><button className="secondary" onClick={() => change({ search: '', disciplineId: '', roleId: '' })}>Ver todas as aulas</button></aside></div>
  </section>;
}

export function LessonPagination({ topicId, roleId, onSelect }: { topicId: string; roleId: string; onSelect: (topicId: string) => void }) {
  const topic = lessons.find(value => value.id === topicId);
  const neighbors = lessons.filter(value => value.disciplineId === topic?.disciplineId && lessonSupportsRole(value, roleId));
  const index = neighbors.findIndex(value => value.id === topicId);
  return <nav className="lesson-pagination" aria-label="Navegação entre aulas"><button className="secondary" disabled={index <= 0} onClick={() => onSelect(neighbors[index - 1].id)}>Aula anterior</button><button className="secondary" disabled={index < 0 || index >= neighbors.length - 1} onClick={() => onSelect(neighbors[index + 1].id)}>Próxima aula</button></nav>;
}
