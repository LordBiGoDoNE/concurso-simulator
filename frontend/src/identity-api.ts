import { field, uuidPattern } from './contract';
import { apiFetch, withAbortScope } from './http-client';

export type IdentityResponse = { ok: boolean; status: number; body: unknown };
export function identityRequest(path: string, options: RequestInit = {}, readJson = true): Promise<IdentityResponse> {
  return withAbortScope(options.signal, 5000, 'Identity request aborted', async (signal) => {
    const response = await apiFetch(path, { ...options, credentials: 'include', cache: 'no-store', signal });
    const body: unknown = readJson && response.ok && response.status !== 204 ? await response.json() : undefined;
    signal.throwIfAborted();
    return { ok: response.ok, status: response.status, body };
  });
}

export type CurrentIdentity =
  { connected: false; googleEnabled: boolean } | { connected: true; googleEnabled: boolean; id: string };
export async function currentIdentity(signal?: AbortSignal): Promise<CurrentIdentity> {
  const [config, me] = await Promise.all([
    identityRequest('/api/v1/auth/config', { signal }),
    identityRequest('/api/v1/me', { signal }),
  ]);
  if (!config.ok || (!me.ok && me.status !== 401)) throw new Error('identity unavailable');
  const googleEnabled = field(config.body, 'googleEnabled');
  if (typeof googleEnabled !== 'boolean') throw new Error('invalid config');
  if (me.status === 401) return { connected: false, googleEnabled };
  const id = field(me.body, 'id');
  if (typeof id !== 'string' || !uuidPattern.test(id)) {
    throw new Error('invalid identity');
  }
  return { connected: true, googleEnabled, id };
}

export type AdministrativeAccount = {
  username: string;
  mustChangePassword: boolean;
  googleEnabled: boolean;
  googleLinked: boolean;
  questionImportAvailable: boolean;
};
export async function administrativeAccount(): Promise<AdministrativeAccount | null> {
  const result = await identityRequest('/api/v1/admin/account');
  if (result.status === 401 || result.status === 403) return null;
  const body = result.body;
  if (
    !result.ok ||
    typeof field(body, 'username') !== 'string' ||
    ['mustChangePassword', 'googleEnabled', 'googleLinked', 'questionImportAvailable'].some(
      (key) => typeof field(body, key) !== 'boolean',
    )
  ) {
    throw new Error('administrative account unavailable');
  }
  return body as AdministrativeAccount;
}
export async function administrativeMutation(
  path: '/auth/login' | '/account/password' | '/account/google-link',
  input: Record<string, string>,
) {
  const csrf = await identityRequest('/api/v1/csrf');
  const token = field(csrf.body, 'token');
  if (!csrf.ok || field(csrf.body, 'headerName') !== 'X-CSRF-TOKEN' || typeof token !== 'string' || !token) {
    throw new Error('csrf unavailable');
  }
  return identityRequest(`/api/v1/admin${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token },
    body: JSON.stringify(input),
  });
}
