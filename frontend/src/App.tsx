import { useEffect, useState } from 'react';

const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const materialUrl = import.meta.env.VITE_MATERIAL_URL || 'https://lordbigodone.github.io/concurso-simulator/';
const configuredTimeout = Number(import.meta.env.VITE_STATUS_TIMEOUT_MS || 5000);
const timeoutMs = Number.isFinite(configuredTimeout) && configuredTimeout > 0 ? configuredTimeout : 5000;

export default function App() {
  const [attempt, setAttempt] = useState(0);
  const [status, setStatus] = useState<'loading' | 'up' | 'down'>('loading');

  useEffect(() => {
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), timeoutMs);
    let active = true;
    setStatus('loading');
    fetch(`${apiUrl.replace(/\/$/, '')}/api/v1/status`, { signal: controller.signal })
      .then(async response => {
        const body = await response.json();
        if (active) setStatus(response.ok && body.status === 'UP' ? 'up' : 'down');
      })
      .catch(() => { if (active) setStatus('down'); })
      .finally(() => window.clearTimeout(timer));
    return () => { active = false; window.clearTimeout(timer); controller.abort(); };
  }, [attempt]);

  return (
    <main>
      <p className="eyebrow">Seu espaço de estudo</p>
      <h1>Concurso Simulator</h1>
      <p>Continue aprendendo com as aulas, questões e resoluções do material de estudo.</p>
      <a className="material-link" href={materialUrl}>Acessar material de estudo</a>
      <section aria-labelledby="application-title">
        <h2 id="application-title">Nova aplicação — ambiente local</h2>
        <p>A base técnica está em desenvolvimento. O material de estudo continua independente da aplicação.</p>
        <p role="status" aria-live="polite">
          {status === 'loading' ? 'Verificando disponibilidade da API…' : status === 'up' ? 'API disponível.' : 'API indisponível. Você pode continuar acessando as aulas.'}
        </p>
        <button disabled={status === 'loading'} onClick={() => setAttempt(value => value + 1)}>Verificar novamente</button>
      </section>
    </main>
  );
}
