import { useEffect, useRef, useState, useCallback } from 'react';
import AdministrativeAccountPanel from './AdministrativeAccountPanel';
import { currentIdentity, identityApiUrl, identityRequest, type CurrentIdentity } from './identity-api';

type Identity = CurrentIdentity;

export default function LoginPanel({ onIdentity, onChecking, onBeforeLogin }: { onIdentity?: (v: CurrentIdentity | null) => void; onChecking?: () => void; onBeforeLogin?: () => boolean }) {
  const [identity, setIdentity] = useState<Identity | null>(null);
  const [state, setState] = useState<'loading' | 'ready' | 'unavailable'>('loading');
  const [attempt, setAttempt] = useState(0);
  const [exiting, setExiting] = useState(false);
  const [message, setMessage] = useState('');
  const [adminOpen, setAdminOpen] = useState(false);
  const [adminPending, setAdminPending] = useState(false);
  const adminMutationPending = useRef(false);
  const identityAbort = useRef<AbortController | null>(null);
  const generation = useRef(0);
  const logoutPending = useRef(false);

  function refreshSession() {
    generation.current += 1;
    if (!adminMutationPending.current) setAttempt(value => value + 1);
  }

  useEffect(() => {
    const url = new URL(window.location.href);
    const admin = url.searchParams.get('admin');
    if (admin) {
      setAdminOpen(true);
      setMessage(admin === 'linked' ? 'Google vinculado. Login por senha e Google disponíveis.' : admin === 'LOCAL_ACCOUNT_HAS_HISTORY' ? 'Vínculo recusado: o ADM local já possui histórico. As duas contas foram preservadas.' : 'Vínculo Google não concluído. As contas foram preservadas; confira sua sessão antes de tentar novamente.');
      url.searchParams.delete('admin');
      window.history.replaceState(window.history.state, '', url);
    }
    if (url.searchParams.get('auth') === 'failed') {
      setMessage('Não foi possível entrar com Google. Você pode tentar novamente ou continuar estudando sem login.');
      url.searchParams.delete('auth');
      window.history.replaceState(window.history.state, '', url);
    }
  }, []);

  useEffect(() => {
    let active = true;
    const requestGeneration = ++generation.current;
    const isCurrent = () => active && requestGeneration === generation.current && !logoutPending.current && !adminMutationPending.current;
    const controller = new AbortController(); identityAbort.current = controller;
    if (!logoutPending.current && !adminMutationPending.current) { setState('loading'); onChecking?.(); }
    if (!adminMutationPending.current) currentIdentity(controller.signal).then(value => {
      if (isCurrent()) { setIdentity(value); setState('ready'); onIdentity?.(value); }
    }).catch(() => { if (isCurrent()) { setState('unavailable'); onIdentity?.(null); } });
    const refresh = () => { if (document.visibilityState === 'visible') refreshSession(); };
    window.addEventListener('focus', refresh);
    document.addEventListener('visibilitychange', refresh);
    const restored = (event: PageTransitionEvent) => { if (event.persisted) { adminMutationPending.current = false; setAdminPending(false); setAdminOpen(false); refreshSession(); } };
    window.addEventListener('pageshow', restored);
    const recheck = () => refreshSession(); window.addEventListener('identity-recheck', recheck);
    return () => { active = false; controller.abort(); window.removeEventListener('focus', refresh); document.removeEventListener('visibilitychange', refresh); window.removeEventListener('pageshow', restored); window.removeEventListener('identity-recheck', recheck); };
  }, [attempt, onIdentity, onChecking]);

  const administrativeMutationChanged = useCallback((pending: boolean) => {
    adminMutationPending.current = pending; setAdminPending(pending); generation.current += 1;
    if (pending) { identityAbort.current?.abort(); onChecking?.(); }
    else { onIdentity?.(null); setAttempt(value => value + 1); }
  }, [onChecking, onIdentity]);

  async function logout() {
    logoutPending.current = true; identityAbort.current?.abort();
    onIdentity?.(null);
    generation.current += 1;
    setExiting(true);
    setMessage('');
    try {
      const csrf = await identityRequest('/api/v1/csrf');
      if (!csrf.ok) throw new Error('csrf unavailable');
      const token = csrf.body;
      if (token?.headerName !== 'X-CSRF-TOKEN' || typeof token.token !== 'string' || !token.token) throw new Error('invalid csrf');
      const response = await identityRequest('/api/v1/auth/logout', {
        method: 'POST', headers: { [token.headerName]: token.token },
      }, false);
      if (response.status === 403) {
        setMessage('Não foi possível confirmar a saída. Tente novamente para renovar a proteção da sessão.');
      } else if (response.status === 204 || response.status === 401) {
        setIdentity(value => value && { connected: false, googleEnabled: value.googleEnabled });
        if (identity) onIdentity?.({ connected: false, googleEnabled: identity.googleEnabled });
        setMessage('Você está sem sessão. Pode continuar estudando sem login.');
      } else throw new Error('logout unavailable');
    } catch {
      setMessage('Saída indisponível. Sua sessão pode continuar ativa; tente novamente. As aulas continuam acessíveis.');
    } finally {
      // Consultas iniciadas antes ou durante a mutação não representam sua sessão final.
      generation.current += 1;
      logoutPending.current = false;
      setState('ready');
      setExiting(false);
    }
  }

  return (
    <section className="session-card" aria-labelledby="identity-title">
      <h2 id="identity-title" className="sr-only">Login opcional</h2>
      <p role="status" aria-live="polite">
        {state === 'loading' ? 'Consultando sessão…' : state === 'unavailable'
          ? 'Consulta de sessão indisponível. Continue acessando as aulas sem login.'
          : identity?.connected ? 'Você está conectado.' : 'Você está estudando como visitante.'}
      </p>
      {message && <p role="alert">{message}</p>}
      {state === 'ready' && identity?.connected && <button disabled={exiting || adminPending} onClick={logout}>{exiting ? 'Saindo…' : 'Sair'}</button>}
      {state === 'ready' && !identity?.connected && identity?.googleEnabled && (
        <a className="material-link" href={`${identityApiUrl}/oauth2/authorization/google`} onClick={event => { if (onBeforeLogin && !onBeforeLogin()) event.preventDefault(); }}>Entrar com Google</a>
      )}
      <button className="session-refresh" disabled={state === 'loading' || exiting || adminPending} onClick={refreshSession}>Atualizar sessão</button>
      <details className="admin-access" open={adminOpen} onToggle={event => setAdminOpen(event.currentTarget.open)}>
        <summary>Acesso administrativo</summary>
        {adminOpen && <AdministrativeAccountPanel connected={!!identity?.connected} identityId={identity?.connected ? identity.id : undefined} onBeforeLogin={onBeforeLogin} onMutation={administrativeMutationChanged} />}
      </details>
      <details className="session-help"><summary>Sobre sua sessão</summary><p>Estude sem entrar. Com a conta, salve e retome simulados; como visitante, a tentativa é temporária e não será transferida no login.</p>
        {state === 'ready' && !identity?.googleEnabled && !identity?.connected && <p>Login Google desativado neste ambiente.</p>}
      </details>
    </section>
  );
}
