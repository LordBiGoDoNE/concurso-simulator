import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { AttemptApiError, attemptResult, finishAttempt, readAttempt, saveAnswer, type AttemptTransport, type Answer, type AttemptItem, type AttemptView, type Result } from './attempt-api';

const defaultTransport: AttemptTransport = {read:readAttempt,save:saveAnswer,finish:finishAttempt,result:attemptResult};

type Phase = 'saved' | 'pending' | 'failed' | 'conflict' | 'unsaved';
interface Job { itemId: string; alternativeId: string | null; commandId: string; expectedVersion?: number }
export function useAttemptRun(initial: AttemptView, onUnavailable: () => void, transport: AttemptTransport = defaultTransport) {
  const [view, setView] = useState(initial); const server = useRef(initial);
  const [phase, setPhase] = useState<Phase>('saved'); const [message, setMessage] = useState('');
  const [result, setResult] = useState<Result | null>(null); const [resultFailed, setResultFailed] = useState(false);
  const [finishing, setFinishing] = useState(false); const [finishFailed, setFinishFailed] = useState(false);
  const closeOperation = useRef<{ commandId: string; version: number } | null>(null); const closing = useRef(false);
  const drafts = useRef<Record<string, string | null>>({}); const queue = useRef<Job[]>([]);
  const [revision, redraw] = useState(0); const working = useRef(false); const blocked = useRef(false);
  const lifetime = useRef<AbortController | null>(null); const unavailable = useRef(onUnavailable); unavailable.current = onUnavailable;
  useLayoutEffect(() => { const controller = new AbortController(); lifetime.current = controller; if (queue.current.length) void pump(); return () => controller.abort(); }, []);
  useEffect(() => { if (view.finishedAt !== null && !result) void loadResult(); }, [view.finishedAt]);
  async function loadResult() {
    const controller = lifetime.current; if (!controller || view.finishedAt === null) return;
    setResultFailed(false);
    try { const value = await transport.result(view.id, controller.signal); if (!controller.signal.aborted) setResult(value); }
    catch (error) { if (!controller.signal.aborted) { if (error instanceof AttemptApiError && [401, 403, 404].includes(error.status)) unavailable.current(); else setResultFailed(true); } }
  }
  async function pump() {
    const controller = lifetime.current; if (!controller || controller.signal.aborted || working.current || blocked.current) return;
    working.current = true; setPhase('pending'); setMessage('');
    try {
      while (queue.current.length) {
        const job = queue.current[0]; job.expectedVersion ??= server.current.version;
        const answer: Answer = { commandId: job.commandId, expectedVersion: job.expectedVersion, alternativeId: job.alternativeId };
        const confirmed = await transport.save(server.current.id, job.itemId, answer, controller.signal);
        if (controller.signal.aborted) return;
        server.current = confirmed; setView(confirmed); queue.current.shift();
        if (drafts.current[job.itemId] === job.alternativeId && !queue.current.some(j => j.itemId === job.itemId)) delete drafts.current[job.itemId];
        redraw(v => v + 1);
      }
      setPhase('saved');
    } catch (error) {
      if (controller.signal.aborted) return;
      blocked.current = true;
      if (error instanceof AttemptApiError && [401, 403, 404].includes(error.status)) { unavailable.current(); return; }
      const conflict = error instanceof AttemptApiError && error.status === 409;
      setPhase(conflict ? 'conflict' : 'failed'); setMessage(conflict
        ? 'Outra alteração avançou a tentativa. Recarregue para conferir a versão atual; sua escolha não confirmada será preservada.'
        : 'Salvamento não confirmado. Sua escolha permanece neste navegador; repita o mesmo comando para confirmar.');
    } finally {
      working.current = false;
      // Effect recreation preserves jobs. Retry only in a live, newer generation with the same command.
      if (controller.signal.aborted && lifetime.current !== controller && lifetime.current && !lifetime.current.signal.aborted && queue.current.length && !blocked.current) void pump();
    }
  }
  function choose(itemId: string, alternativeId: string | null) {
    if (server.current.finishedAt !== null || closeOperation.current || closing.current) return;
    drafts.current[itemId] = alternativeId; queue.current.push({ itemId, alternativeId, commandId: crypto.randomUUID() }); redraw(v => v + 1);
    if (!blocked.current) void pump();
  }
  function retry() { if (phase !== 'failed') return; if (!queue.current.length) { void reload(); return; } blocked.current = false; void pump(); }
  async function reload() {
    const controller = lifetime.current; if (!controller || controller.signal.aborted || working.current || closing.current) return;
    working.current = true; setPhase('pending');
    try {
      const current = await transport.read(server.current.id, controller.signal); if (controller.signal.aborted) return;
      server.current = current; setView(current); queue.current = []; blocked.current = true;
      closeOperation.current = null; setFinishFailed(false);
      const dirty = Object.keys(drafts.current).length > 0;
      setPhase(dirty && current.finishedAt === null ? 'unsaved' : 'saved');
      setMessage(dirty ? current.finishedAt === null ? 'Versão atual carregada. Escolhas locais preservadas, ainda não salvas; confira antes de enviar.' : 'A tentativa já foi concluída. Escolhas locais não confirmadas não foram aplicadas ao resultado.' : 'Versão atual carregada.');
      if (!dirty) blocked.current = false;
    } catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof AttemptApiError && [401, 403, 404].includes(error.status)) { unavailable.current(); return; }
      setPhase('failed'); setMessage('Não foi possível recarregar. Escolhas locais continuam sem confirmação.');
    } finally { working.current = false; }
  }
  function savePreserved() {
    if (phase !== 'unsaved' || server.current.finishedAt !== null) return;
    queue.current = Object.entries(drafts.current).map(([itemId, alternativeId]) => ({ itemId, alternativeId, commandId: crypto.randomUUID() }));
    blocked.current = false; void pump();
  }
  async function finish() {
    const controller = lifetime.current;
    if (!controller || controller.signal.aborted || closing.current || working.current || queue.current.length || server.current.finishedAt !== null) return;
    if (!closeOperation.current) {
      if (phase !== 'saved' || Object.keys(drafts.current).length) return;
      closeOperation.current = { commandId: crypto.randomUUID(), version: server.current.version };
    }
    closing.current = true; setFinishing(true); setFinishFailed(false); setMessage('');
    try {
      const closed = await transport.finish(server.current.id, closeOperation.current.commandId, closeOperation.current.version, controller.signal);
      if (controller.signal.aborted) return;
      server.current = closed.attempt; setView(closed.attempt); setResult(closed.result);
    } catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof AttemptApiError && [401, 403, 404].includes(error.status)) { unavailable.current(); return; }
      if (error instanceof AttemptApiError && error.status === 409) {
        closeOperation.current = null; blocked.current = true; setPhase('conflict');
        setMessage('A versão mudou antes de concluir. Recarregue e confira as respostas confirmadas.');
      } else { setFinishFailed(true); setMessage('Conclusão não confirmada. Repita a mesma operação ou recarregue para verificar; nenhuma nota local foi calculada.'); }
    } finally { closing.current = false; if (!controller.signal.aborted) setFinishing(false); }
  }
  return { view, phase, message, revision, choose, retry, reload, savePreserved, finish, finishing, finishFailed, result, resultFailed, loadResult,
    canReplace: () => !working.current && !closing.current,
    selected: (item: AttemptItem) => Object.hasOwn(drafts.current, item.id) ? drafts.current[item.id] : item.selectedAlternativeId,
    controlsLocked: closeOperation.current !== null || finishing || view.finishedAt !== null,
    canFinish: phase === 'saved' && queue.current.length === 0 && Object.keys(drafts.current).length === 0 && view.finishedAt === null && closeOperation.current === null,
  };
}
