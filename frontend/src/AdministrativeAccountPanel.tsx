import { useEffect, useRef, useState } from 'react';
import './AdministrativeAccountPanel.css';
import { administrativeAccount, administrativeMutation, identityApiUrl, type AdministrativeAccount } from './identity-api';

type Props = { connected: boolean; identityId?: string; onBeforeLogin?: () => boolean; onMutation: (pending: boolean) => void; navigate?: (url: string) => void };
export default function AdministrativeAccountPanel({ connected, identityId, onBeforeLogin, onMutation, navigate = url => window.location.assign(url) }: Props) {
  const contextKey = useRef(connected ? identityId : 'visitor');
  contextKey.current = connected ? identityId : 'visitor';
  const [account, setAccount] = useState<AdministrativeAccount | null>(null);
  const [loading, setLoading] = useState(connected);
  const [busy, setBusy] = useState(false);
  const [name, setName] = useState('admin');
  const [password, setPassword] = useState('');
  const [nextPassword, setNextPassword] = useState('');
  const [message, setMessage] = useState('');
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let active = true;
    setAccount(null); setPassword(''); setNextPassword('');
    if (!connected) { setLoading(false); return; }
    setLoading(true);
    administrativeAccount().then(value => { if (active) setAccount(value); })
      .catch(() => { if (active) setMessage('Consulta administrativa indisponível. Atualize a sessão para tentar novamente.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [connected, identityId, revision]);
  async function submit(action: 'login' | 'password' | 'link') {
    if (busy || loading || (action === 'login' && onBeforeLogin && !onBeforeLogin())) return;
    if (action === 'password' && (Array.from(nextPassword).length < 15 || Array.from(nextPassword).length > 128)) {
      setMessage('Use de 15 a 128 caracteres na nova senha.'); return;
    }
    const operationContext = contextKey.current;
    let redirecting = false;
    setBusy(true); setMessage(''); onMutation(true);
    try {
      const result = await administrativeMutation(action === 'login' ? '/auth/login' : action === 'password' ? '/account/password' : '/account/google-link',
        action === 'login' ? { username: name, password } : action === 'password' ? { currentPassword: password, newPassword: nextPassword } : { currentPassword: password });
      if (contextKey.current !== operationContext) return;
      if (!result.ok) {
        setMessage(result.status === 429 ? 'Muitas tentativas. Aguarde quinze minutos antes de tentar novamente.' : result.status === 401 ? 'Credencial inválida. Confira os dados e tente novamente.' : result.status === 403 ? 'Sessão ou proteção expirada. Atualize a sessão e tente novamente.' : 'Operação não concluída. Confira sua sessão e os dados informados.');
        return;
      }
      if (action === 'link') {
        if (result.body?.authorizationUrl !== '/oauth2/authorization/google') throw new Error('invalid authorization destination');
        navigate(`${identityApiUrl}${result.body.authorizationUrl}`); redirecting = true;
      } else {
        setMessage(action === 'password' ? 'Senha atualizada.' : 'Login administrativo confirmado.');
      }
    } catch { if (contextKey.current === operationContext) setMessage('Acesso administrativo indisponível. Nenhuma alteração foi confirmada; atualize a sessão antes de tentar novamente.'); }
    finally {
      setPassword(''); setNextPassword('');
      // Rechecking the old session while OIDC rotates it can overwrite the new cookie.
      if (!redirecting) { setBusy(false); setRevision(value => value + 1); onMutation(false); }
    }
  }
  return <div className="admin-account">
    {loading && <p role="status">Consultando acesso administrativo…</p>}
    {message && <p role="alert">{message}</p>}
    {!loading && !account && <form onSubmit={event => { event.preventDefault(); void submit('login'); }}>
      <p>Use a credencial ADM configurada na inicialização.</p>
      <label>Usuário ADM<input autoComplete="username" value={name} required maxLength={64} onChange={event => setName(event.target.value)} disabled={busy} /></label>
      <label>Senha ADM<input type="password" autoComplete="current-password" value={password} required maxLength={256} onChange={event => setPassword(event.target.value)} disabled={busy} /></label>
      <button disabled={busy} type="submit">{busy ? 'Entrando…' : 'Entrar como ADM'}</button>
    </form>}
    {!loading && account && <>
      <p>Administrador: {account.username}.</p>
      {account.mustChangePassword && <p role="status">Troque a senha temporária para liberar a importação e o vínculo Google.</p>}
      {!account.mustChangePassword && <p>{account.questionImportAvailable ? 'Importação administrativa disponível pela API.' : 'Importação ainda não configurada neste ambiente.'}</p>}
      <form onSubmit={event => { event.preventDefault(); void submit('password'); }}>
        <label>Senha atual<input type="password" autoComplete="current-password" value={password} required maxLength={256} disabled={busy} onChange={event => setPassword(event.target.value)} /></label>
        <label>Nova senha<input type="password" autoComplete="new-password" value={nextPassword} required minLength={15} maxLength={256} disabled={busy} onChange={event => setNextPassword(event.target.value)} /></label>
        <p>Use de 15 a 128 caracteres.</p>
        <button disabled={busy} type="submit">Trocar senha</button>
      </form>
      {account.googleLinked ? <p>Google vinculado. Você pode entrar por Google ou senha.</p> : !account.googleEnabled ? <p>Google desativado neste ambiente. O login por senha continua disponível.</p> : <>
        <p>Para vincular, confirme a senha atual e escolha sua conta no Google. Uma conta Google existente será preservada; o vínculo será bloqueado se o ADM local já tiver histórico.</p>
        <button disabled={busy || account.mustChangePassword || !password} onClick={() => void submit('link')}>Vincular Google</button>
      </>}
    </>}
  </div>;
}
