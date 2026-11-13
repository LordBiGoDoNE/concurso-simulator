import { studyCatalog, lessons, lessonSupportsRole } from './study-catalog';

interface Props {
  roleId: string;
  onRoleChange: (role: string) => void;
  onLessons: (discipline?: string, roleId?: string) => void;
  onCompose: () => void;
  onQuestions: () => void;
  onOpenLesson: (topicId: string) => void;
}

export default function StudyHome({ roleId, onRoleChange, onLessons, onCompose, onQuestions, onOpenLesson }: Props) {
  const available = lessons.filter(topic => lessonSupportsRole(topic, roleId));
  const suggestion = available[0];
  const suggestionDiscipline = studyCatalog.disciplines.find(d => d.id === suggestion?.disciplineId)?.title;
  return <>
    <section className="welcome" aria-labelledby="welcome-title">
      <div>
        <p className="eyebrow">Limeira · Concurso Público 01/2026</p>
        <h1 id="welcome-title">Seu cargo.<br />Seu próximo passo.</h1>
        <p className="intro">Prepare-se para Assistente Administrativo ou Secretário de Escola. Comece por uma aula e coloque o assunto em prática, no seu ritmo.</p>
        <form className="study-entry" onSubmit={event => { event.preventDefault(); onLessons('', roleId); }}>
          <label htmlFor="entry-role">Para qual cargo você vai estudar?</label>
          <select id="entry-role" value={roleId} onChange={event => onRoleChange(event.target.value)} required>
            <option value="">Escolha um cargo</option>
            {studyCatalog.roles.map(role => <option key={role.id} value={role.id}>{role.title}</option>)}
          </select>
          <button type="submit" disabled={!roleId}>Encontrar aulas <span aria-hidden="true">→</span></button>
        </form>
        <button className="secondary" onClick={() => onLessons('', '')}>Explorar todas as aulas</button>
        <p className="home-facts"><span>{lessons.length} aulas no material</span><span>{studyCatalog.roles.length} cargos</span><span>Comece sem login</span></p>
      </div>
      {suggestion && <aside className="start-suggestion" aria-labelledby="start-suggestion-title">
        <p id="start-suggestion-title" className="start-suggestion-label">Sugestão para começar</p>
        <p className="start-suggestion-topic">{suggestion.id} · {suggestion.title}</p>
        <p className="start-suggestion-meta">{suggestionDiscipline} · aula e prática</p>
        <button className="secondary" onClick={() => onOpenLesson(suggestion.id)}>Abrir aula <span aria-hidden="true">→</span></button>
      </aside>}
    </section>
    <p className="independent-note">Material independente de estudo. Não é um site oficial do concurso. Confira o edital e as fontes de cada aula.</p>
    <div className="home-lower">
      <section className="home-subjects" aria-labelledby="home-subjects-title">
        <p className="eyebrow">Explore o conteúdo</p><h2 id="home-subjects-title">O que vamos estudar?</h2>
        <p className="muted">{roleId ? 'Disciplinas do cargo escolhido. Você pode trocar o cargo ou explorar todas as aulas.' : 'Escolha uma disciplina para começar pelas aulas.'}</p>
        <div className="discipline-list">{studyCatalog.disciplines.filter(d => available.some(t => t.disciplineId === d.id)).map((d, i) =>
          <button className="discipline-link" key={d.id} onClick={() => onLessons(d.id)}><span className="discipline-icon" aria-hidden="true">{String(i + 1).padStart(2, '0')}</span><span><strong>{d.title}</strong><small>{available.filter(t => t.disciplineId === d.id).length} aulas para explorar</small></span><span className="discipline-arrow" aria-hidden="true">→</span></button>
        )}</div>
      </section>
      <aside className="study-guide" aria-labelledby="guide-title">
        <h2 id="guide-title">Seu primeiro caminho</h2>
        <ol><li><strong>Escolha seu cargo</strong><p>Encontre as disciplinas para a sua preparação.</p></li><li><strong>Entenda uma aula</strong><p>Leia a teoria, os exemplos e as referências.</p></li><li><strong>Pratique o assunto</strong><p>Quando houver questões disponíveis, comece uma prática de até cinco questões e revise ao encerrar.</p></li></ol>
        <p className="guest-note">Você pode estudar sem login. Com a conta habilitada no ambiente, respostas confirmadas podem ser retomadas pelo histórico.</p>
        <div className="secondary-study-actions"><button className="secondary" onClick={onCompose}>Montar um simulado</button><button className="secondary" onClick={onQuestions}>Consultar banco de questões</button></div>
      </aside>
    </div>
  </>;
}
