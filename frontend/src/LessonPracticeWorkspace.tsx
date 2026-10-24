import { useCallback, useEffect, useImperativeHandle, useLayoutEffect, useRef, useState, type Ref } from 'react';
import { AttemptApiError, type AttemptView } from './attempt-api';
import type { CurrentIdentity } from './identity-api';
import type { DirectedPractice } from './directed-practice';
import { createLessonPractice, lessonPracticeTransport } from './lesson-practice-api';
import AttemptPractice, { type AttemptPracticeHandle } from './AttemptPractice';
import LessonPracticeHistory from './LessonPracticeHistory';
import { attemptRoleTitle } from './AttemptHistory';
import catalog from './lesson-catalog.json';

export interface LessonPracticeHandle {start:(selection:DirectedPractice)=>boolean}
export default function LessonPracticeWorkspace({identity,contextActive,onUnavailable,onGuestActive,onBackToLesson,onChooseLesson,ref}:{identity:CurrentIdentity;contextActive:boolean;onUnavailable:()=>void;onGuestActive:(value:boolean)=>void;onBackToLesson:(topic:string)=>void;onChooseLesson:()=>void;ref?:Ref<LessonPracticeHandle>}) {
  const [selected,setSelected]=useState<AttemptView|null>(null);const selectedRef=useRef(selected);selectedRef.current=selected;
  const [busy,setBusy]=useState(false);const pending=useRef(false);const request=useRef<AbortController|null>(null);
  const [message,setMessage]=useState('');const [failed,setFailed]=useState(false);const [refresh,setRefresh]=useState(0);
  const [access,setAccess]=useState<'ready'|'checking'|'failed'>('ready');const [verify,setVerify]=useState(0);
  const job=useRef<{selection:DirectedPractice;commandId:string}|null>(null);const run=useRef<AttemptPracticeHandle>(null);
  useEffect(()=>()=>{request.current?.abort();pending.current=false;},[]);
  useEffect(()=>{onGuestActive(!identity.connected && selected!==null);return()=>onGuestActive(false);},[identity.connected,selected?.id,onGuestActive]);
  useLayoutEffect(()=>{
    const current=selectedRef.current;if(!current || !contextActive)return;const c=new AbortController();setAccess('checking');
    lessonPracticeTransport.read(current.id,c.signal).then(()=>{if(!c.signal.aborted)setAccess('ready');}).catch(error=>{
      if(c.signal.aborted)return;if(error instanceof AttemptApiError && [401,403,404].includes(error.status))onUnavailable();else setAccess('failed');
    });return()=>c.abort();
  },[identity,contextActive,selected?.id,verify,onUnavailable]);
  function replaceAllowed(){
    if(!contextActive || pending.current || access!=='ready')return false;
    if(selectedRef.current && run.current?.canReplace()!==true){setMessage('Aguarde o salvamento ou encerramento desta prática antes de trocar.');return false;}
    return !selectedRef.current || selectedRef.current.finishedAt!==null || window.confirm('Trocar de prática? Somente respostas confirmadas poderão ser retomadas pela conta; visitante não tem histórico permanente.');
  }
  async function create(){
    const work=job.current;if(!work || pending.current || !contextActive)return;
    const c=new AbortController();request.current?.abort();request.current=c;pending.current=true;setBusy(true);setMessage('');setFailed(false);
    try{
      const value=await createLessonPractice(work.selection,work.commandId,c.signal);if(c.signal.aborted)return;
      setSelected(value);setAccess('ready');job.current=null;setRefresh(v=>v+1);
    }catch(error){
      if(c.signal.aborted)return;
      if(error instanceof AttemptApiError && [401,403,404].includes(error.status)){onUnavailable();return;}
      const empty=error instanceof AttemptApiError && error.status===409 && error.available.some(a=>a.disciplineId===work.selection.disciplineId && a.available===0);
      setFailed(true);setMessage(empty?'Esta aula ainda não tem questões próprias de prática para o cargo escolhido. O acervo de simulados permanece separado.':'Não foi possível confirmar o início. Tente novamente para recuperar a mesma prática, sem criar outra.');
    }finally{if(!c.signal.aborted){pending.current=false;setBusy(false);}}
  }
  useImperativeHandle(ref,()=>({start:selection=>{
    if(!replaceAllowed())return false;job.current={selection,commandId:crypto.randomUUID()};void create();return true;
  }}));
  async function open(id:string){
    if(selectedRef.current?.id===id || !replaceAllowed())return;
    const c=new AbortController();request.current?.abort();request.current=c;pending.current=true;setBusy(true);setMessage('');setFailed(false);job.current=null;
    try{const value=await lessonPracticeTransport.read(id,c.signal);if(!c.signal.aborted){setSelected(value);setAccess('ready');}}
    catch(error){if(c.signal.aborted)return;if(error instanceof AttemptApiError && [401,403,404].includes(error.status))onUnavailable();else setMessage('Não foi possível abrir a prática. Use o histórico para tentar novamente.');}
    finally{if(!c.signal.aborted){pending.current=false;setBusy(false);}}
  }
  const finished=useCallback((value:AttemptView)=>{setSelected(current=>current?.id===value.id?value:current);setRefresh(v=>v+1);},[]);
  const topic=selected?.items[0].classification.topicId;const title=catalog.topics.find(t=>t.id===topic)?.title;
  return <section className="lesson-practice-workspace" aria-labelledby="practice-workspace-title"><header className="page-intro"><p className="eyebrow">Aprender fazendo</p><h1 id="practice-workspace-title">Prática das aulas</h1><p>Questões próprias do assunto, sem usar o acervo de simulados. Correção e explicações somente ao encerrar.</p></header>
    {busy && <p role="status">Preparando sua prática…</p>}{message && <p role="alert">{message}</p>}
    {failed && <button disabled={busy || !contextActive} onClick={()=>void create()}>Tentar iniciar prática novamente</button>}
    {!selected && !busy && <div className="study-guide"><h2>Qual assunto vamos praticar?</h2><p>Abra uma aula, confira o cargo e escolha Praticar. O treino reúne até cinco questões próprias daquele assunto.</p><button onClick={onChooseLesson}>Escolher uma aula</button></div>}
    {selected && <><div className="practice-context"><h2>{topic} — {title}</h2><p>{attemptRoleTitle(selected.roleId)} · {selected.items.length} questões da aula</p><button className="secondary" onClick={()=>onBackToLesson(topic!)}>Voltar à aula {topic}</button></div>
      {access==='checking' && <p role="status">Conferindo acesso à prática…</p>}{access==='failed' && <><p role="alert">Não foi possível conferir o acesso. A prática fica oculta até confirmar.</p><button onClick={()=>setVerify(v=>v+1)}>Conferir prática novamente</button></>}
      <div hidden={busy || access!=='ready' || !contextActive}><AttemptPractice ref={run} key={selected.id} initial={selected} practice transport={lessonPracticeTransport} onUnavailable={onUnavailable} onFinished={finished}/></div>
    </>}
    {identity.connected && contextActive && <LessonPracticeHistory refresh={refresh} onOpen={id=>void open(id)} onUnavailable={onUnavailable}/>}
  </section>;
}
