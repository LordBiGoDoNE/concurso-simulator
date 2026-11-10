import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import AdministrativeAccountPanel from './AdministrativeAccountPanel';
import App from './App';
import { emptyQuestionResponse } from '../test/question-fixtures';
const response = (body: unknown, status = 200) => ({ ok: status >= 200 && status < 300, status, json: async () => body });
const account = { username: 'admin', mustChangePassword: true, googleEnabled: true, googleLinked: false, questionImportAvailable: false };
afterEach(() => vi.unstubAllGlobals());
it('login local obtém CSRF, respeita o aviso de visitante e apaga a senha', async () => {
  const calls = vi.fn(async (url: string) => url.endsWith('/csrf') ? response({ token: 'fresh', headerName: 'X-CSRF-TOKEN' }) : response(null, 204));
  vi.stubGlobal('fetch', calls); const pending = vi.fn(); const before = vi.fn(() => false);
  const view = render(<AdministrativeAccountPanel connected={false} onBeforeLogin={before} onMutation={pending} />);
  fireEvent.change(screen.getByLabelText('Senha ADM'), { target: { value: 'private-password' } });
  fireEvent.submit(screen.getByRole('button', { name: 'Entrar como ADM' }).closest('form')!);
  expect(calls).not.toHaveBeenCalled();expect(pending).not.toHaveBeenCalled();
  view.rerender(<AdministrativeAccountPanel connected={false} onBeforeLogin={() => true} onMutation={pending} />);
  fireEvent.submit(screen.getByRole('button', { name: 'Entrar como ADM' }).closest('form')!);
  await screen.findByText('Login administrativo confirmado.');
  expect(screen.getByLabelText('Senha ADM')).toHaveValue('');
  expect(calls).toHaveBeenLastCalledWith(expect.stringContaining('/admin/auth/login'), expect.objectContaining({ credentials: 'include', cache: 'no-store', headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': 'fresh' }, body: JSON.stringify({ username: 'admin', password: 'private-password' }) }));
  expect(pending.mock.calls).toEqual([[true], [false]]);
  expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
});
it('troca obrigatória bloqueia vínculo e confirma mudança somente depois do servidor', async () => {
  let temporary = true;
  const fetch = vi.fn(async (url: string) => {
    if (url.endsWith('/account')) return response({ ...account, mustChangePassword: temporary });
    if (url.endsWith('/csrf')) return response({ token: 'fresh', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/password')) { temporary = false; return response(null, 204); }
    throw new Error('unexpected');
  });vi.stubGlobal('fetch', fetch);
  render(<AdministrativeAccountPanel connected identityId="account-a" onMutation={() => {}} />);
  expect(await screen.findByText(/Troque a senha temporária/)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Vincular Google' })).toBeDisabled();
  fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'temporary-private' } });
  fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: 'chosen-private-password' } });
  fireEvent.submit(screen.getByRole('button', { name: 'Trocar senha' }).closest('form')!);
  await screen.findByText('Senha atualizada.');
  await waitFor(() => expect(screen.queryByText(/Troque a senha temporária/)).not.toBeInTheDocument());
  expect(screen.getByLabelText('Senha atual')).toHaveValue('');expect(screen.getByLabelText('Nova senha')).toHaveValue('');
});
it.each([401, 403, 429, 503])('falha %s mantém a operação sem sucesso e limpa segredos', async status => {
  vi.stubGlobal('fetch', vi.fn(async (url: string) => url.endsWith('/csrf') ? response({ token: 'fresh', headerName: 'X-CSRF-TOKEN' }) : response(null, status)));
  render(<AdministrativeAccountPanel connected={false} onMutation={() => {}} />);
  fireEvent.change(screen.getByLabelText('Senha ADM'), { target: { value: 'private-password' } });
  fireEvent.submit(screen.getByRole('button', { name: 'Entrar como ADM' }).closest('form')!);
  expect(await screen.findByRole('alert')).not.toHaveTextContent('confirmado');
  expect(screen.getByLabelText('Senha ADM')).toHaveValue('');
});
it('não carrega área administrativa até abrir e troca o UUID na integração de sessão', async () => {
  let connected = false;
  const fetch = vi.fn(async (url: string) => {
    if (url.includes('/question-bank/')) return emptyQuestionResponse(url);
    if (url.endsWith('/status')) return response({ status: 'UP' });
    if (url.endsWith('/auth/config')) return response({ googleEnabled: false });
    if (url.endsWith('/me')) return connected ? response({ id: '12345678-1234-1234-1234-123456789abc' }) : response(null, 401);
    if (url.endsWith('/csrf')) return response({ token: 'fresh', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/auth/login')) { connected = true; return response(null, 204); }
    if (url.endsWith('/account')) return response(account);
    throw new Error('unexpected');
  });vi.stubGlobal('fetch', fetch);render(<App />);
  await screen.findByText('Você está estudando como visitante.');
  expect(fetch.mock.calls.some(([url]) => url.endsWith('/account'))).toBe(false);
  fireEvent.click(screen.getByText('Acesso administrativo'));
  const details = screen.getByText('Acesso administrativo').closest('details')!;
  fireEvent(details, new Event('toggle'));
  // jsdom does not implement the native summary toggle consistently.
  if (!details.open) { details.open = true; fireEvent(details, new Event('toggle')); }
  fireEvent.change(await screen.findByLabelText('Senha ADM'), { target: { value: 'private-password' } });
  fireEvent.submit(screen.getByRole('button', { name: 'Entrar como ADM' }).closest('form')!);
  await screen.findByText('Você está conectado.');
  expect(await screen.findByText(/Troque a senha temporária/)).toBeInTheDocument();
});

it('Google desligado mantém senha e mudança de conta limpa os campos privados', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => response({ ...account, googleEnabled: false, mustChangePassword: false })));
  const view = render(<AdministrativeAccountPanel connected identityId="account-a" onMutation={() => {}} />);
  await screen.findByText('Google desativado neste ambiente. O login por senha continua disponível.');
  expect(screen.queryByRole('button', { name: 'Vincular Google' })).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'private-account-a' } });
  fireEvent.change(screen.getByLabelText('Nova senha'), { target: { value: 'private-account-a-next' } });
  view.rerender(<AdministrativeAccountPanel connected identityId="account-b" onMutation={() => {}} />);
  await screen.findByLabelText('Senha atual');expect(screen.getByLabelText('Senha atual')).toHaveValue('');expect(screen.getByLabelText('Nova senha')).toHaveValue('');
});
it('resposta atrasada de login não anuncia sucesso na conta que substituiu o contexto', async () => {
  let finish!: (value: ReturnType<typeof response>) => void;
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    if (url.endsWith('/csrf')) return response({ token: 'fresh', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/account')) return response(account);
    return new Promise<ReturnType<typeof response>>(resolve => { finish = resolve; });
  }));
  const view = render(<AdministrativeAccountPanel connected={false} onMutation={() => {}} />);
  fireEvent.change(screen.getByLabelText('Senha ADM'), { target: { value: 'private-password' } });
  fireEvent.submit(screen.getByRole('button', { name: 'Entrar como ADM' }).closest('form')!);
  await waitFor(() => expect(finish).toBeDefined());
  view.rerender(<AdministrativeAccountPanel connected identityId="account-b" onMutation={() => {}} />);
  await screen.findByText(/Administrador: admin/);finish(response(null, 204));
  await waitFor(() => expect(screen.getByRole('button', { name: 'Trocar senha' })).not.toBeDisabled());
  expect(screen.queryByText('Login administrativo confirmado.')).not.toBeInTheDocument();expect(screen.getByLabelText('Senha atual')).toHaveValue('');
});

it('redirecionamento Google suspende rechecagem da sessão antiga e limpa a prova de senha', async () => {
  const mutation = vi.fn(), navigate = vi.fn();
  const fetch = vi.fn(async (url: string) => {
    if (url.endsWith('/account')) return response({ ...account, mustChangePassword: false });
    if (url.endsWith('/csrf')) return response({ token: 'fresh', headerName: 'X-CSRF-TOKEN' });
    return response({ authorizationUrl: '/oauth2/authorization/google' });
  });vi.stubGlobal('fetch', fetch);
  render(<AdministrativeAccountPanel connected identityId="account-a" onMutation={mutation} navigate={navigate} />);
  await screen.findByLabelText('Senha atual');fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'private-link-proof' } });
  fireEvent.click(screen.getByRole('button', { name: 'Vincular Google' }));
  await waitFor(() => expect(navigate).toHaveBeenCalledWith('http://localhost:8080/oauth2/authorization/google'));
  expect(mutation.mock.calls).toEqual([[true]]);expect(screen.getByLabelText('Senha atual')).toHaveValue('');
  expect(screen.getByRole('button', { name: 'Vincular Google' })).toBeDisabled();
  expect(fetch.mock.calls.filter(([url]) => url.endsWith('/account'))).toHaveLength(1);
});
