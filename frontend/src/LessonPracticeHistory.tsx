import { useEffect, useState } from 'react';
import { AttemptApiError } from './attempt-api';
import { lessonPracticeHistory, type PracticeHistoryPage } from './lesson-practice-api';
import { attemptRoleTitle } from './AttemptHistory';
import catalog from './lesson-catalog.json';

export default function LessonPracticeHistory({ refresh, onOpen, onUnavailable }: {refresh:number;onOpen:(id:string)=>void;onUnavailable:()=>void}) {
  const [page,setPage]=useState(0);const [retry,setRetry]=useState(0);
  const [data,setData]=useState<{key:string;value?:PracticeHistoryPage;failed?:boolean}|null>(null);
  const key=`${page}:${refresh}:${retry}`;
  useEffect(()=>{
    const c=new AbortController();
    lessonPracticeHistory(page,c.signal).then(value=>{if(!c.signal.aborted)setData({key,value});}).catch(error=>{
      if(c.signal.aborted)return;
      if(error instanceof AttemptApiError && [401,403].includes(error.status))onUnavailable();else setData({key,failed:true});
    });return()=>c.abort();
  },[key,page,onUnavailable]);
  const current=data?.key===key?data:null;
  return <section aria-labelledby="lesson-history-title" className="lesson-history"><h2 id="lesson-history-title">Suas práticas</h2><p>Treinos das aulas, separados do histórico de simulados. Página {page+1}.</p>
    <button className="secondary" onClick={()=>setRetry(v=>v+1)}>Atualizar práticas</button>
    {!current && <p role="status">Carregando práticas…</p>}{current?.failed && <p role="alert">Histórico de práticas indisponível. Tente atualizar.</p>}
    {current?.value && <>{!current.value.items.length && <p>Nenhuma prática nesta página. Comece por uma aula.</p>}<ul className="attempt-history">{current.value.items.map(e=><li key={e.id}>
      <p><strong>{e.topicId} — {catalog.topics.find(t=>t.id===e.topicId)?.title}</strong></p><p>{attemptRoleTitle(e.roleId)} · {e.total} questões · {new Date(e.createdAt).toLocaleString('pt-BR')}</p>
      <p>{e.finishedAt===null?'Em andamento — correção ao encerrar.':`Encerrada — ${e.percentage?.toLocaleString('pt-BR')}%.`}</p>
      <button onClick={()=>onOpen(e.id)}>Abrir prática de {e.topicId} de {new Date(e.createdAt).toLocaleString('pt-BR')}</button>
    </li>)}</ul></>}
    <nav aria-label="Paginação das práticas"><button disabled={!current?.value || page===0} onClick={()=>setPage(v=>v-1)}>Práticas anteriores</button><button disabled={!current?.value || current.value.items.length<current.value.size} onClick={()=>setPage(v=>v+1)}>Mais práticas</button></nav>
  </section>;
}
