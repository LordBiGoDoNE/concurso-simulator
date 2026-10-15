import { useEffect, useRef, useState } from 'react';
import { AttemptApiError, readAttempt, saveAnswer, type Answer, type AttemptItem, type AttemptView } from './attempt-api';

type Phase = 'saved' | 'pending' | 'failed' | 'conflict' | 'unsaved';
interface Job { itemId: string; alternativeId: string | null; commandId: string; expectedVersion?: number }
export function useAttemptRun(initial: AttemptView, onUnavailable: () => void) {
  const [view, setView] = useState(initial); const server = useRef(initial);
  const [phase, setPhase] = useState<Phase>('saved'); const [message, setMessage] = useState('');
  const drafts = useRef<Record<string, string | null>>({}); const queue = useRef<Job[]>([]);
  const [revision, redraw] = useState(0); const working = useRef(false); const blocked = useRef(false);
  const lifetime = useRef<AbortController | null>(null); const unavailable = useRef(onUnavailable); unavailable.current = onUnavailable;
  useEffect(() => { const controller = new AbortController(); lifetime.current = controller; return () => controller.abort(); }, []);
  async function pump() {
    const controller = lifetime.current; if (!controller || controller.signal.aborted || working.current || blocked.current) return;
    working.current = true; setPhase('pending'); setMessage('');
    try {
      while (queue.current.length) {
        const job = queue.current[0]; job.expectedVersion ??= server.current.version;
        const answer: Answer = { commandId: job.commandId, expectedVersion: job.expectedVersion, alternativeId: job.alternativeId };
        const confirmed = await saveAnswer(server.current.id, job.itemId, answer, controller.signal);
        if (controller.signal.aborted) return;
        server.current = confirmed; setView(confirmed); queue.current.shift();
        if (drafts.current[job.itemId] === job.alternativeId && !queue.current.some(j => j.itemId === job.itemId)) delete drafts.current[job.itemId];
        redraw(v => v + 1);
      }
      setPhase('saved');
    } catch (error) {
      if (controller.signal.aborted) return;
      blocked.current = true;
      if (error instanceof AttemptApiError && [401, 404].includes(error.status)) { unavailable.current(); return; }
      const conflict = error instanceof AttemptApiError && error.status === 409;
      setPhase(conflict ? 'conflict' : 'failed'); setMessage(conflict
        ? 'Outra alteração avançou a tentativa. Recarregue para conferir a versão atual; sua escolha não confirmada será preservada.'
        : 'Salvamento não confirmado. Sua escolha permanece neste navegador; repita o mesmo comando para confirmar.');
    } finally { working.current = false; }
  }
  function choose(itemId: string, alternativeId: string | null) {
    if (server.current.finishedAt !== null) return;
    drafts.current[itemId] = alternativeId; queue.current.push({ itemId, alternativeId, commandId: crypto.randomUUID() }); redraw(v => v + 1);
    if (!blocked.current) void pump();
  }
  function retry() { if (phase !== 'failed') return; if (!queue.current.length) { void reload(); return; } blocked.current = false; void pump(); }
  async function reload() {
    const controller = lifetime.current; if (!controller || controller.signal.aborted || working.current) return;
    working.current = true; setPhase('pending');
    try {
      const current = await readAttempt(server.current.id, controller.signal); if (controller.signal.aborted) return;
      server.current = current; setView(current); queue.current = []; blocked.current = true;
      const dirty = Object.keys(drafts.current).length > 0;
      setPhase(dirty && current.finishedAt === null ? 'unsaved' : 'saved');
      setMessage(dirty ? current.finishedAt === null ? 'Versão atual carregada. Escolhas locais preservadas, ainda não salvas; confira antes de enviar.' : 'A tentativa já foi concluída. Escolhas locais não confirmadas não foram aplicadas ao resultado.' : 'Versão atual carregada.');
      if (!dirty) blocked.current = false;
    } catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof AttemptApiError && [401, 404].includes(error.status)) { unavailable.current(); return; }
      setPhase('failed'); setMessage('Não foi possível recarregar. Escolhas locais continuam sem confirmação.');
    } finally { working.current = false; }
  }
  function savePreserved() {
    if (phase !== 'unsaved' || server.current.finishedAt !== null) return;
    queue.current = Object.entries(drafts.current).map(([itemId, alternativeId]) => ({ itemId, alternativeId, commandId: crypto.randomUUID() }));
    blocked.current = false; void pump();
  }
  return { view, phase, message, revision, choose, retry, reload, savePreserved,
    selected: (item: AttemptItem) => Object.hasOwn(drafts.current, item.id) ? drafts.current[item.id] : item.selectedAlternativeId,
    canFinish: phase === 'saved' && queue.current.length === 0 && Object.keys(drafts.current).length === 0 && view.finishedAt === null,
  };
}
