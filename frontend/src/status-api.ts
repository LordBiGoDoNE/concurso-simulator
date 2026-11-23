import { field } from './contract';
import { apiFetch, positiveTimeout, withAbortScope } from './http-client';

const timeoutMs = positiveTimeout(import.meta.env.VITE_STATUS_TIMEOUT_MS, 5000);

/** Disponibilidade da API: `up` só com resposta OK e corpo `{ status: 'UP' }`; qualquer outra coisa é `down`. */
export async function apiStatus(signal: AbortSignal): Promise<'up' | 'down'> {
  try {
    return await withAbortScope(signal, timeoutMs, 'Status interrompido', async (scope) => {
      const response = await apiFetch('/api/v1/status', { signal: scope });
      const body: unknown = await response.json();
      scope.throwIfAborted();
      return response.ok && field(body, 'status') === 'UP' ? 'up' : 'down';
    });
  } catch {
    return 'down';
  }
}
