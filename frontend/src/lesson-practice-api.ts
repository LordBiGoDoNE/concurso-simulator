import { attemptRequest, validateAttempt, ensure, row, id, number, list, type AttemptView, type Answer, type Finished, type Result, type AttemptTransport } from './attempt-api';
import type { DirectedPractice } from './directed-practice';
import catalog from './lesson-catalog.json';

function view(value: unknown): asserts value is AttemptView {
  validateAttempt(value);ensure(value.items.length<=5);
  const first=value.items[0].classification;
  ensure(catalog.topics.some(t=>t.id===first.topicId && t.disciplineId===first.disciplineId));
  ensure(value.items.every(i=>i.classification.topicId===first.topicId && i.classification.disciplineId===first.disciplineId && i.classification.roleIds.includes(value.roleId)));
}
const base='/api/v1/lesson-practices';
const route=(value: string)=>{id(value);return `${base}/${value}`;};
export const createLessonPractice=(selection: DirectedPractice,commandId:string,signal:AbortSignal)=>attemptRequest<AttemptView>(base,signal,v=>{
  view(v);ensure(v.roleId===selection.roleId && v.items.every(i=>i.classification.topicId===selection.topicId && i.classification.disciplineId===selection.disciplineId));
},'POST',{commandId,roleId:selection.roleId,topicId:selection.topicId});
export const lessonPracticeTransport: AttemptTransport={
  read:(value,signal)=>attemptRequest<AttemptView>(route(value),signal,v=>{view(v);ensure(v.id===value);}),
  save:(value,itemId,answer:Answer,signal)=>attemptRequest<AttemptView>(`${route(value)}/answers/${encodeURIComponent(itemId)}`,signal,v=>{
    view(v);ensure(v.id===value && v.version===answer.expectedVersion+1 && v.finishedAt===null && v.items.some(i=>i.id===itemId && i.selectedAlternativeId===answer.alternativeId));
  },'PUT',answer),
  finish:(value,commandId,expectedVersion,signal)=>attemptRequest<Finished>(`${route(value)}/finish`,signal,async v=>{
    const validator=await import('./attempt-result-api');const finished=validator.finished(v,value,expectedVersion);view(finished.attempt);return finished;
  },'POST',{commandId,expectedVersion}),
  result:(value,signal)=>attemptRequest<Result>(`${route(value)}/result`,signal,async v=>{
    const validator=await import('./attempt-result-api');return validator.result(v);
  }),
};
export interface PracticeHistoryItem {id:string;roleId:string;topicId:string;createdAt:string;finishedAt:string|null;total:number;percentage:number|null}
export interface PracticeHistoryPage {items:PracticeHistoryItem[];page:number;size:number}
export const lessonPracticeHistory=(page:number,signal:AbortSignal)=>attemptRequest<PracticeHistoryPage>(`/api/v1/me/lesson-practices?page=${page}&size=20`,signal,v=>{
  const r=row(v,['items','page','size']);ensure(r.page===page && r.size===20);list(r.items);
  r.items.forEach(value=>{
    const e=row(value,['id','roleId','topicId','createdAt','finishedAt','total','percentage']);id(e.id);number(e.total);ensure(Number(e.total)>=1 && Number(e.total)<=5);
    ensure(catalog.roles.some(role=>role.id===e.roleId) && catalog.topics.some(t=>t.id===e.topicId));
    ensure(typeof e.createdAt==='string' && Number.isFinite(Date.parse(e.createdAt)));
    if(e.finishedAt===null)ensure(e.percentage===null);
    else ensure(typeof e.finishedAt==='string' && Number.isFinite(Date.parse(e.finishedAt)) && typeof e.percentage==='number' && e.percentage>=0 && e.percentage<=100);
  });
});
