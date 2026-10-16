import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import type { CurrentIdentity } from './identity-api';
import { AttemptApiError, readAttempt, type AttemptView } from './attempt-api';
import AttemptComposer from './AttemptComposer';
import AttemptPractice from './AttemptPractice';
import AttemptHistory from './AttemptHistory';
import SubjectReviewPanel from './SubjectReviewPanel';

export default function AttemptWorkspace({ identity, onUnavailable, onGuestActive, reviewActive = true }: { reviewActive?: boolean; identity: CurrentIdentity; onUnavailable: () => void; onGuestActive: (active: boolean) => void }) {
  const [selected, setSelected] = useState<AttemptView | null>(null); const [loading, setLoading] = useState(false); const [message, setMessage] = useState('');
  const [historyRefresh, setHistoryRefresh] = useState(0); const generation = useRef(0); const request = useRef<AbortController | null>(null);
  const selectedRef = useRef(selected); selectedRef.current = selected;
  const [access, setAccess] = useState<'ready' | 'checking' | 'failed'>('ready'); const [verifyRetry, setVerifyRetry] = useState(0);
  useLayoutEffect(() => {
    const target = selectedRef.current; if (!target) return;
    const controller = new AbortController(); setAccess('checking');
    readAttempt(target.id, controller.signal).then(() => { if (!controller.signal.aborted && selectedRef.current?.id === target.id) setAccess('ready'); })
      .catch(error => { if (controller.signal.aborted || selectedRef.current?.id !== target.id) return;
        if (error instanceof AttemptApiError && [401, 403, 404].includes(error.status)) onUnavailable(); else setAccess('failed');
      });
    return () => controller.abort();
  }, [identity, verifyRetry, onUnavailable]);
  useEffect(() => () => { generation.current++; request.current?.abort(); }, []);
  useEffect(() => { onGuestActive(!identity.connected && selected !== null); }, [identity.connected, selected?.id, onGuestActive]);
  useEffect(() => () => onGuestActive(false), [onGuestActive]);
  const finished = useCallback(() => setHistoryRefresh(v => v + 1), []);
  function created(view: AttemptView) { onGuestActive(!identity.connected); setSelected(view); setAccess('ready'); setMessage(''); setHistoryRefresh(v => v + 1); }
  async function open(id: string) {
    if (selected?.id === id) return;
    if (selected && selected.id !== id && !window.confirm('Trocar de tentativa? Apenas respostas confirmadas poderão ser retomadas.')) return;
    request.current?.abort(); const controller = new AbortController(); request.current = controller; const current = ++generation.current;
    setSelected(null); setLoading(true); setMessage('');
    try { const view = await readAttempt(id, controller.signal); if (current === generation.current && !controller.signal.aborted) { setSelected(view); setAccess('ready'); } }
    catch (error) { if (current !== generation.current || controller.signal.aborted) return; if (error instanceof AttemptApiError && [401, 403, 404].includes(error.status)) onUnavailable(); else setMessage('Tentativa indisponível. Tente abri-la novamente pelo histórico.'); }
    finally { if (current === generation.current && !controller.signal.aborted) setLoading(false); }
  }
  return <section aria-labelledby="mock-exam-title"><h2 id="mock-exam-title">Simulados</h2>
    <p>{identity.connected ? 'Respostas confirmadas ficam salvas na sua conta para retomar em outra sessão.' : 'Modo visitante: tentativa temporária por até 24 horas, acessível somente nesta sessão válida. Não há histórico permanente; entrar na conta não transfere esta tentativa.'}</p>
    {message && <p role="alert">{message}</p>}
    {loading ? <p role="status">Retomando tentativa…</p> : selected ? <>
      {access === 'checking' && <p role="status">Conferindo acesso à tentativa…</p>}
      {access === 'failed' && <><p role="alert">Não foi possível confirmar o acesso à tentativa. O conteúdo permanece oculto.</p><button onClick={() => setVerifyRetry(v => v + 1)}>Conferir tentativa novamente</button></>}
      <div hidden={access !== 'ready'}><AttemptPractice key={selected.id} initial={selected} reviewActive={reviewActive && access === 'ready'} onUnavailable={onUnavailable} onFinished={finished} /></div>
      <button onClick={() => { if (window.confirm('Compor outro simulado? Apenas respostas confirmadas ficam salvas na conta; visitante não possui histórico.')) setSelected(null); }}>Compor outro simulado</button>
    </> : <AttemptComposer onCreated={created} onUnavailable={onUnavailable} />}
    {identity.connected && <SubjectReviewPanel active={reviewActive} refresh={historyRefresh} onUnavailable={onUnavailable} />}
    {identity.connected && <AttemptHistory refresh={historyRefresh} onOpen={id => void open(id)} onUnavailable={onUnavailable} />}
  </section>;
}
