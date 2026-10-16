import { useLayoutEffect, useRef, useState } from 'react';
import { ReviewApiError } from './subject-review-api';

type State<T> = { key: string; phase: 'idle' | 'loading' | 'error'; status?: number } | { key: string; phase: 'ready'; value: T };
/** Conteúdo privado vive somente no contexto atual; nunca reutiliza resposta de outra consulta. */
export function useSubjectReview<T>(key: string, enabled: boolean, refresh: number, load: (signal: AbortSignal) => Promise<T>, onUnavailable: () => void) {
  const [state, setState] = useState<State<T>>({ key: '', phase: 'idle' });
  const loader = useRef(load);loader.current = load;const unavailable = useRef(onUnavailable);unavailable.current = onUnavailable;
  const requestKey = `${key}:${enabled}:${refresh}`;
  useLayoutEffect(() => {
    const controller = new AbortController();let current = true;
    setState({ key: requestKey, phase: enabled ? 'loading' : 'idle' });
    if (enabled) void loader.current(controller.signal).then(value => { if (current && !controller.signal.aborted) setState({ key: requestKey, phase: 'ready', value }); })
      .catch(error => {
        if (!current || controller.signal.aborted) return;
        const status = error instanceof ReviewApiError ? error.status : undefined;
        if (status !== undefined && [401, 403, 404].includes(status)) { setState({ key: requestKey, phase: 'idle' });unavailable.current(); }
        else setState({ key: requestKey, phase: 'error', status });
      });
    return () => { current = false;controller.abort(); };
  }, [requestKey, enabled]);
  return state.key === requestKey && enabled ? state : { key: requestKey, phase: enabled ? 'loading' as const : 'idle' as const };
}
