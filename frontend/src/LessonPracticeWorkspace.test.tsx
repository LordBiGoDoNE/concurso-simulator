import { fireEvent, render, screen, waitFor, act } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { useRef } from 'react';
import LessonPracticeWorkspace, { type LessonPracticeHandle } from './LessonPracticeWorkspace';
import { createLessonPractice, lessonPracticeTransport, lessonPracticeHistory } from './lesson-practice-api';
import { view, result } from '../test/attempt-fixtures';
vi.mock('./lesson-practice-api',()=>({createLessonPractice:vi.fn(),lessonPracticeTransport:{read:vi.fn(),save:vi.fn(),finish:vi.fn(),result:vi.fn()},lessonPracticeHistory:vi.fn()}));
afterEach(()=>vi.resetAllMocks());
const selection={topicId:'P01',disciplineId:'portugues',roleId:'assistente-administrativo'};
function Fixture({connected=false}:{connected?:boolean}) {
 const ref=useRef<LessonPracticeHandle>(null);
 return <><button onClick={()=>ref.current?.start(selection)}>Começar aula</button><LessonPracticeWorkspace ref={ref} identity={connected?{connected:true,googleEnabled:false,id:'90000000-0000-0000-0000-000000000001'}:{connected:false,googleEnabled:false}} contextActive onUnavailable={vi.fn()} onGuestActive={vi.fn()} onBackToLesson={vi.fn()} onChooseLesson={vi.fn()}/></>;
}
function setup(){vi.mocked(createLessonPractice).mockResolvedValue(view);vi.mocked(lessonPracticeTransport.read).mockResolvedValue(view);vi.mocked(lessonPracticeHistory).mockResolvedValue({page:0,size:20,items:[]});}
it('retry de criação mantém comando e não toca o compositor de simulados',async()=>{
 setup();vi.mocked(createLessonPractice).mockRejectedValueOnce(new TypeError('network'));
 render(<Fixture/>);fireEvent.click(screen.getByRole('button',{name:'Começar aula'}));fireEvent.click(await screen.findByRole('button',{name:'Tentar iniciar prática novamente'}));
 await screen.findByRole('heading',{name:'Questão 1 de 1'});
 expect(vi.mocked(createLessonPractice).mock.calls[0].slice(0,2)).toEqual(vi.mocked(createLessonPractice).mock.calls[1].slice(0,2));
 expect(screen.queryByRole('button',{name:'Iniciar simulado'})).not.toBeInTheDocument();
});
it('criação tardia após desmontar o contexto não apresenta conteúdo da identidade anterior',async()=>{
 setup();let resolve!:(v:typeof view)=>void;vi.mocked(createLessonPractice).mockReturnValue(new Promise(r=>resolve=r));
 const rendered=render(<Fixture/>);fireEvent.click(screen.getByRole('button',{name:'Começar aula'}));rendered.unmount();
 await act(async()=>resolve(view));expect(screen.queryByRole('heading',{name:'Questão 1 de 1'})).not.toBeInTheDocument();
});
it('prática bloqueia sua troca durante salvamento e mostra correção somente ao encerrar',async()=>{
 setup();let resolve!:(v:typeof view)=>void;vi.mocked(lessonPracticeTransport.save).mockReturnValue(new Promise(r=>resolve=r));
 const selected={...view,version:1,items:[{...view.items[0],selectedAlternativeId:view.items[0].alternatives[0].id}]};
 vi.mocked(lessonPracticeTransport.finish).mockResolvedValue({attempt:{...selected,version:2,finishedAt:'2026-10-24T12:00:00Z'},result});
 vi.mocked(lessonPracticeTransport.result).mockResolvedValue(result);
 render(<Fixture/>);fireEvent.click(screen.getByRole('button',{name:'Começar aula'}));await screen.findByRole('heading',{name:'Questão 1 de 1'});
 await waitFor(()=>expect(lessonPracticeTransport.read).toHaveBeenCalled());fireEvent.click(screen.getAllByRole('radio')[0]);
 await waitFor(()=>expect(lessonPracticeTransport.save).toHaveBeenCalledTimes(1));fireEvent.click(screen.getByRole('button',{name:'Começar aula'}));
 expect(createLessonPractice).toHaveBeenCalledTimes(1);expect(screen.getByRole('alert')).toHaveTextContent('Aguarde o salvamento');
 expect(screen.queryByText('PRIVATE_ATTEMPT_RESOLUTION_SENTINEL')).not.toBeInTheDocument();expect(lessonPracticeTransport.result).not.toHaveBeenCalled();
 await act(async()=>resolve(selected));fireEvent.click(screen.getByRole('button',{name:'Encerrar prática'}));expect(lessonPracticeTransport.finish).not.toHaveBeenCalled();
 fireEvent.click(screen.getByRole('button',{name:'Confirmar encerramento'}));await screen.findByRole('heading',{name:'Resultado da prática'});
 expect(screen.getByText('PRIVATE_ATTEMPT_RESOLUTION_SENTINEL')).toBeInTheDocument();expect(screen.queryByRole('radio')).not.toBeInTheDocument();
});
it('histórico próprio é reservado à conta e inclui o assunto',async()=>{
 setup();vi.mocked(lessonPracticeHistory).mockResolvedValue({page:0,size:20,items:[{id:view.id,topicId:'P01',roleId:view.roleId,createdAt:view.createdAt,finishedAt:null,total:1,percentage:null}]});
 const r=render(<Fixture/>);expect(lessonPracticeHistory).not.toHaveBeenCalled();r.unmount();render(<Fixture connected/>);
 fireEvent.click(await screen.findByRole('button',{name:/Abrir prática de P01/}));await screen.findByRole('heading',{name:'Questão 1 de 1'});expect(lessonPracticeTransport.read).toHaveBeenCalledWith(view.id,expect.any(AbortSignal));
});
