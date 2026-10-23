import { useEffect, useState } from 'react';
import { AttemptApiError, attemptHistory, type HistoryPage } from './attempt-api';
export const attemptRoleTitle = (id: string) => ({ 'assistente-administrativo': 'Assistente Administrativo', 'secretario-de-escola': 'Secretário de Escola' })[id as 'assistente-administrativo'] || 'Simulado';
export default function AttemptHistory({ refresh, onOpen, onUnavailable }: { refresh: number; onOpen: (id: string) => void; onUnavailable: () => void }) {
  const [page, setPage] = useState(0); const [retry, setRetry] = useState(0);
  const [data, setData] = useState<{ key: string; value?: HistoryPage; failed?: boolean } | null>(null);
  const key = `${page}:${refresh}:${retry}`;
  useEffect(() => {
    const controller = new AbortController(); let active = true;
    attemptHistory(page, controller.signal).then(value => { if (active) setData({ key, value }); }).catch(error => {
      if (!active) return;
      if (error instanceof AttemptApiError && [401, 403].includes(error.status)) onUnavailable(); else setData({ key, failed: true });
    });
    return () => { active = false; controller.abort(); };
  }, [key, page, onUnavailable]);
  const current = data?.key === key ? data : null;
  return <section aria-labelledby="attempt-history-title"><h3 id="attempt-history-title">Histórico de simulados</h3>
    <p>Somente respostas confirmadas são retomadas pela conta. Página {page + 1}.</p>
    <button onClick={() => setRetry(v => v + 1)}>Atualizar histórico</button>
    {!current && <p role="status">Carregando histórico…</p>}
    {current?.failed && <p role="alert">Histórico indisponível. Tente atualizar; não é uma lista vazia confirmada.</p>}
    {current?.value && <>{!current.value.items.length && <p>Nenhuma tentativa nesta página.</p>}<ul className="attempt-history">{current.value.items.map(e => <li key={e.id}>
      <p>{attemptRoleTitle(e.roleId)} — {new Date(e.createdAt).toLocaleString('pt-BR')} — {e.total} questões.</p>
      <p>{e.finishedAt === null ? 'Em andamento — sem nota parcial.' : `Concluído — ${e.percentage?.toLocaleString('pt-BR')}%.`}</p>
      <button onClick={() => onOpen(e.id)}>Abrir simulado de {new Date(e.createdAt).toLocaleString('pt-BR')}</button>
    </li>)}</ul></>}
    <nav aria-label="Paginação do histórico"><button disabled={!current?.value || page === 0} onClick={() => setPage(v => v - 1)}>Página anterior do histórico</button><button disabled={!current?.value || current.value.items.length < current.value.size} onClick={() => setPage(v => v + 1)}>Próxima página do histórico</button></nav>
  </section>;
}
