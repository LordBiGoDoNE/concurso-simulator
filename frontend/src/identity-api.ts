export const identityApiUrl = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

export async function identityRequest(path: string, options: RequestInit = {}, readJson = true) {
  const controller = new AbortController();
  const abort = () => controller.abort();
  options.signal?.addEventListener('abort', abort, { once: true });
  if (options.signal?.aborted) controller.abort();
  const timer = window.setTimeout(() => controller.abort(), 5000);
  let rejectAbort!: () => void;
  const aborted = new Promise<never>((_resolve, reject) => {
    rejectAbort = () => reject(new DOMException('Identity request aborted', 'AbortError'));
    controller.signal.addEventListener('abort', rejectAbort, { once: true });
    if (controller.signal.aborted) rejectAbort();
  });
  try {
    return await Promise.race([aborted, (async () => {
      controller.signal.throwIfAborted();
      const response = await fetch(`${identityApiUrl}${path}`, { ...options, credentials: 'include', cache: 'no-store', signal: controller.signal });
      const body = readJson && response.ok && response.status !== 204 ? await response.json() : undefined;
      controller.signal.throwIfAborted();
      return { ok: response.ok, status: response.status, body };
    })()]);
  } finally {
    window.clearTimeout(timer);
    options.signal?.removeEventListener('abort', abort);
    controller.signal.removeEventListener('abort', rejectAbort);
  }
}

export async function currentIdentity() {
  const [config, me] = await Promise.all([
    identityRequest('/api/v1/auth/config'), identityRequest('/api/v1/me'),
  ]);
  if (!config.ok || (!me.ok && me.status !== 401)) throw new Error('identity unavailable');
  const settings = config.body;
  if (typeof settings?.googleEnabled !== 'boolean') throw new Error('invalid config');
  if (me.status === 401) return { connected: false, googleEnabled: settings.googleEnabled as boolean };
  const user = me.body;
  if (typeof user?.id !== 'string' || !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(user.id)) {
    throw new Error('invalid identity');
  }
  return { connected: true, googleEnabled: settings.googleEnabled as boolean };
}
