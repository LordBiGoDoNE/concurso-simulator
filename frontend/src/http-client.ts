// Transporte HTTP único da API. Cada módulo mantém credenciais, mensagens e mapeamento de erros;
// aqui ficam só a URL base, a regra de timeout e o escopo de abort comum a todas as requisições.

/** URL base da API, sem barra final. */
export const apiBaseUrl = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

/** Timeout configurado quando é um número positivo; senão, o padrão. */
export function positiveTimeout(configured: string | undefined, fallback: number): number {
  const value = Number(configured || fallback);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

/**
 * Executa `work` num escopo de abort ligado ao sinal externo e a um timeout. A promessa rejeita com
 * `AbortError` assim que o escopo é abortado, mesmo que `fetch` ou a validação ignorem o sinal; timer e
 * ouvintes são liberados ao final. `work` deve chamar `signal.throwIfAborted()` depois de ler o corpo.
 */
export async function withAbortScope<T>(
  external: AbortSignal | null | undefined,
  timeoutMs: number,
  abortMessage: string,
  work: (signal: AbortSignal) => Promise<T>,
): Promise<T> {
  const controller = new AbortController();
  const abort = () => controller.abort();
  external?.addEventListener('abort', abort, { once: true });
  if (external?.aborted) abort();
  const timer = window.setTimeout(abort, timeoutMs);
  let rejectAbort!: () => void;
  const aborted = new Promise<never>((_resolve, reject) => {
    rejectAbort = () => reject(new DOMException(abortMessage, 'AbortError'));
    controller.signal.addEventListener('abort', rejectAbort, { once: true });
    if (controller.signal.aborted) rejectAbort();
  });
  try {
    return await Promise.race([
      aborted,
      (async () => {
        controller.signal.throwIfAborted();
        return work(controller.signal);
      })(),
    ]);
  } finally {
    window.clearTimeout(timer);
    external?.removeEventListener('abort', abort);
    controller.signal.removeEventListener('abort', rejectAbort);
  }
}

/** Único ponto do app que chama `fetch` para a API (regra de lint). */
export function apiFetch(path: string, init: RequestInit): Promise<Response> {
  return fetch(`${apiBaseUrl}${path}`, init);
}
