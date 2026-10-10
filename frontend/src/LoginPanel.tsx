import { useEffect, useRef, useState } from 'react';
import { currentIdentity, identityApiUrl, identityRequest } from './identity-api';

type Identity = { connected: boolean; googleEnabled: boolean };

export default function LoginPanel() {
  const [identity, setIdentity] = useState<Identity | null>(null);
  const [state, setState] = useState<'loading' | 'ready' | 'unavailable'>('loading');
  const [attempt, setAttempt] = useState(0);
  const [exiting, setExiting] = useState(false);
  const [message, setMessage] = useState('');
  const generation = useRef(0);
  const logoutPending = useRef(false);

  function refreshSession() {
    generation.current += 1;
    setAttempt(value => value + 1);
  }

  useEffect(() => {
    const url = new URL(window.location.href);
    if (url.searchParams.get('auth') === 'failed') {
      setMessage('Não foi possível entrar com Google. Você pode tentar novamente ou continuar estudando sem login.');
      url.searchParams.delete('auth');
      window.history.replaceState(window.history.state, '', url);
    }
  }, []);

  useEffect(() => {
    let active = true;
    const requestGeneration = ++generation.current;
    const isCurrent = () => active && requestGeneration === generation.current && !logoutPending.current;
    if (!logoutPending.current) setState('loading');
    currentIdentity().then(value => {
      if (isCurrent()) { setIdentity(value); setState('ready'); }
    }).catch(() => { if (isCurrent()) setState('unavailable'); });
    const refresh = () => { if (document.visibilityState === 'visible') refreshSession(); };
    window.addEventListener('focus', refresh);
    return () => { active = false; window.removeEventListener('focus', refresh); };
  }, [attempt]);

  async function logout() {
    logoutPending.current = true;
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
        setIdentity(value => value && { ...value, connected: false });
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
    <section aria-labelledby="identity-title">
      <h2 id="identity-title">Login opcional</h2>
      <p>Estude sem entrar. O login prepara sua identidade; a sincronização de histórico ainda não está disponível.</p>
      <p role="status" aria-live="polite">
        {state === 'loading' ? 'Consultando sessão…' : state === 'unavailable'
          ? 'Consulta de sessão indisponível. Continue acessando as aulas sem login.'
          : identity?.connected ? 'Você está conectado.' : 'Você está estudando como visitante.'}
      </p>
      {message && <p role="alert">{message}</p>}
      {state === 'ready' && identity?.connected && <button disabled={exiting} onClick={logout}>{exiting ? 'Saindo…' : 'Sair'}</button>}
      {state === 'ready' && !identity?.connected && identity?.googleEnabled && (
        <a className="material-link" href={`${identityApiUrl}/oauth2/authorization/google`}>Entrar com Google</a>
      )}
      {state === 'ready' && !identity?.googleEnabled && !identity?.connected && <p>Login Google desativado neste ambiente.</p>}
      <button className="session-refresh" disabled={state === 'loading' || exiting} onClick={refreshSession}>Atualizar sessão</button>
    </section>
  );
}
