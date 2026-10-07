export const identityApiUrl = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

export async function identityRequest(path: string, options: RequestInit = {}) {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), 5000);
  try {
    return await fetch(`${identityApiUrl}${path}`, { ...options, credentials: 'include', cache: 'no-store', signal: controller.signal });
  } finally {
    window.clearTimeout(timer);
  }
}

export async function currentIdentity() {
  const [config, me] = await Promise.all([
    identityRequest('/api/v1/auth/config'), identityRequest('/api/v1/me'),
  ]);
  if (!config.ok || (!me.ok && me.status !== 401)) throw new Error('identity unavailable');
  const settings = await config.json();
  if (typeof settings.googleEnabled !== 'boolean') throw new Error('invalid config');
  if (me.status === 401) return { connected: false, googleEnabled: settings.googleEnabled as boolean };
  const user = await me.json();
  if (typeof user.id !== 'string' || !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(user.id)) {
    throw new Error('invalid identity');
  }
  return { connected: true, googleEnabled: settings.googleEnabled as boolean };
}
