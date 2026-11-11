import { useCallback, useEffect, useImperativeHandle, useLayoutEffect, useRef, useState, type Ref } from 'react';
import { AttemptApiError, type RunView } from './attempt-api';
import type { CurrentIdentity } from './identity-api';
import type { PracticeView, LessonPracticeSelection } from './lesson-practice-api';
import { createLessonPractice, lessonPracticeTransport } from './lesson-practice-api';
import AttemptPractice, { type AttemptPracticeHandle } from './AttemptPractice';
import LessonPracticeHistory from './LessonPracticeHistory';
import catalog from './lesson-catalog.json';
import LessonPracticeEntry from './LessonPracticeEntry';
import { type Lesson } from './study-catalog';

export interface LessonPracticeHandle {start:(selection:LessonPracticeSelection)=>boolean}
interface InlineLesson {topic:Lesson}
export default function LessonPracticeWorkspace({identity,contextActive,onUnavailable,onGuestActive,onBackToLesson,onChooseLesson,inlineLesson,ref}:{identity:CurrentIdentity|null;contextActive:boolean;onUnavailable:()=>void;onGuestActive:(value:boolean)=>void;onBackToLesson:(topic:string)=>void;onChooseLesson:()=>void;inlineLesson?:InlineLesson;ref?:Ref<LessonPracticeHandle>}) {
  const [selected,setSelected]=useState<PracticeView|null>(null);const selectedRef=useRef(selected);selectedRef.current=selected;
  const [busy,setBusy]=useState(false);const pending=useRef(false);const request=useRef<AbortController|null>(null);
  const [message,setMessage]=useState('');const [failed,setFailed]=useState(false);const [refresh,setRefresh]=useState(0);
  const [access,setAccess]=useState<'ready'|'checking'|'failed'>('ready');const [verify,setVerify]=useState(0);
  const job=useRef<{selection:LessonPracticeSelection;commandId:string}|null>(null);const run=useRef<AttemptPracticeHandle>(null);
  const [openKey,setOpenKey]=useState('');
  const inlineKey=inlineLesson?.topic.id ?? '';
  const matches=!inlineLesson || selected?.topicId===inlineLesson.topic.id;
  const pendingMatches=!inlineLesson || job.current?.selection.topicId===inlineLesson.topic.id;
  useEffect(()=>()=>{request.current?.abort();pending.current=false;},[]);
  useEffect(()=>{onGuestActive(!!identity && !identity.connected && selected!==null);return()=>onGuestActive(false);},[identity?.connected,selected?.id,onGuestActive]);
  useLayoutEffect(()=>{
    const current=selectedRef.current;if(!current || !contextActive)return;const c=new AbortController();setAccess('checking');
    lessonPracticeTransport.read(current.id,c.signal).then(()=>{if(!c.signal.aborted)setAccess('ready');}).catch(error=>{
      if(c.signal.aborted)return;if(error instanceof AttemptApiError && [401,403,404].includes(error.status))onUnavailable();else setAccess('failed');
    });return()=>c.abort();
  },[identity,contextActive,selected?.id,verify,onUnavailable]);
  function replaceAllowed(){
    if(!identity || !contextActive || pending.current || access!=='ready')return false;
    if(selectedRef.current && run.current?.canReplace()!==true){setMessage('Aguarde o salvamento ou encerramento desta prática antes de trocar.');return false;}
    return !selectedRef.current || selectedRef.current.finishedAt!==null || window.confirm('Trocar de prática? Somente respostas confirmadas poderão ser retomadas pela conta; visitante não tem histórico permanente.');
  }
  async function create(){
    const work=job.current;if(!work || pending.current || !contextActive)return;
    // O retry pode ocorrer após editar a prática anterior, depois de uma falha.
    if(selectedRef.current && (access!=='ready' || run.current?.canReplace()!==true)){
      setMessage('Aguarde o salvamento ou encerramento desta prática antes de repetir o início.');return;
    }
    const c=new AbortController();request.current?.abort();request.current=c;pending.current=true;setBusy(true);setMessage('');setFailed(false);
    try{
      const value=await createLessonPractice(work.selection,work.commandId,c.signal);if(c.signal.aborted)return;
      setSelected(value);setAccess('ready');job.current=null;setRefresh(v=>v+1);
    }catch(error){
      if(c.signal.aborted)return;
      if(error instanceof AttemptApiError && [401,403,404].includes(error.status)){onUnavailable();return;}
      const empty=error instanceof AttemptApiError && error.status===409 && error.available.some(a=>a.available===0);
      setFailed(true);setMessage(empty?'Esta aula ainda não tem questões próprias de prática. O acervo de simulados permanece separado.':'Não foi possível confirmar o início. Tente novamente para recuperar a mesma prática, sem criar outra.');
    }finally{if(!c.signal.aborted){pending.current=false;setBusy(false);}}
  }
  function start(selection:LessonPracticeSelection){
    if(!replaceAllowed())return false;job.current={selection,commandId:crypto.randomUUID()};void create();return true;
  }
  useImperativeHandle(ref,()=>({start}));
  async function open(id:string){
    if(selectedRef.current?.id===id || !replaceAllowed())return;
    const c=new AbortController();request.current?.abort();request.current=c;pending.current=true;setBusy(true);setMessage('');setFailed(false);job.current=null;
    try{const value=await lessonPracticeTransport.read(id,c.signal);if(!c.signal.aborted){setSelected(value);setAccess('ready');}}
    catch(error){if(c.signal.aborted)return;if(error instanceof AttemptApiError && [401,403,404].includes(error.status))onUnavailable();else setMessage('Não foi possível abrir a prática. Use o histórico para tentar novamente.');}
    finally{if(!c.signal.aborted){pending.current=false;setBusy(false);}}
  }
  const finished=useCallback((value:RunView)=>{setSelected(current=>current?.id===value.id?{...current,...value}:current);setRefresh(v=>v+1);},[]);
  const topic=selected?.topicId;const title=catalog.topics.find(t=>t.id===topic)?.title;
  return <section className="lesson-practice-workspace" aria-labelledby={inlineLesson ? 'lesson-practice-panel-title' : 'practice-workspace-title'}>
    {!inlineLesson && <header className="page-intro"><p className="eyebrow">Aprender fazendo</p><h1 id="practice-workspace-title">Prática das aulas</h1><p>Questões próprias do assunto, sem usar o acervo de simulados. Correção e explicações somente ao encerrar.</p></header>}
    <details className={inlineLesson ? 'lesson-practice-panel' : 'practice-standalone'} open={!inlineLesson || openKey===inlineKey} onToggle={event=>{if(inlineLesson && event.currentTarget.open!==(openKey===inlineKey))setOpenKey(event.currentTarget.open ? inlineKey : '');}}>
    <summary hidden={!inlineLesson}><span><strong id="lesson-practice-panel-title">Pratique esta aula</strong><small>Opcional · até cinco questões{matches && selected ? selected.finishedAt ? ' · prática encerrada' : ' · prática em andamento' : ''}</small></span></summary>
    <div className="lesson-practice-panel-body">
    {inlineLesson && <LessonPracticeEntry topic={inlineLesson.topic} onPractice={start} active startEnabled={contextActive} startState={busy ? 'loading' : failed && job.current ? 'pending' : matches && selected ? selected.finishedAt ? 'finished' : 'running' : 'ready'} />}
    {busy && pendingMatches && <p role="status">Carregando questões da prática…</p>}
    {inlineLesson && job.current && !pendingMatches && <p role="status">Há um início pendente de {job.current.selection.topicId}. Aguarde ou retome esse início antes de criar outra prática.{!busy && <button className="secondary" onClick={()=>{const selection=job.current!.selection;setOpenKey(selection.topicId);onBackToLesson(selection.topicId);}}>Retomar início de {job.current.selection.topicId}</button>}</p>}
    {(pendingMatches || !job.current) && message && <p role="alert">{message}</p>}
    {pendingMatches && failed && <button disabled={busy || !contextActive} onClick={()=>void create()}>Tentar iniciar prática novamente</button>}
    {!inlineLesson && !selected && !busy && <div className="study-guide"><h2>Qual assunto vamos praticar?</h2><p>Abra uma aula e escolha Começar prática. O treino reúne até cinco questões próprias daquele assunto.</p><button onClick={onChooseLesson}>Escolher uma aula</button></div>}
    {selected && <div hidden={!matches || busy || !contextActive} className="inline-practice-run"><div className="practice-context"><h2>{topic} — {title}</h2><p>{selected.items.length} questões da aula</p>{!inlineLesson && <button className="secondary" onClick={()=>onBackToLesson(topic!)}>Voltar à aula {topic}</button>}</div>
      {access==='checking' && <p role="status">Conferindo acesso à prática…</p>}{access==='failed' && <><p role="alert">Não foi possível conferir o acesso. A prática fica oculta até confirmar.</p><button onClick={()=>setVerify(v=>v+1)}>Conferir prática novamente</button></>}
      <div hidden={busy || access!=='ready' || !contextActive}><AttemptPractice ref={run} key={selected.id} initial={selected} practice transport={lessonPracticeTransport} onUnavailable={onUnavailable} onFinished={finished}/></div>
    </div>}
    {inlineLesson && matches && selected && <button className="secondary collapse-practice" onClick={()=>setOpenKey('')}>Recolher prática e voltar à leitura</button>}
    {!inlineLesson && identity?.connected && contextActive && <LessonPracticeHistory refresh={refresh} onOpen={id=>void open(id)} onUnavailable={onUnavailable}/>}
    </div></details>
  </section>;
}
