import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import type { AnimationNode } from './native-lesson';

// SMIL timeline control. Browsers without these methods (or tests) still show the
// figure's static poster state, which the generator writes into the attributes.
type Timeline = SVGSVGElement & Partial<Pick<SVGSVGElement, 'pauseAnimations' | 'unpauseAnimations' | 'setCurrentTime' | 'getCurrentTime'>>;
const seek = (svg: Timeline | null, time: number) => { svg?.pauseAnimations?.(); svg?.setCurrentTime?.(time); };
const now = (svg: Timeline | null) => svg?.getCurrentTime?.() ?? 0;

function useReducedMotion(): boolean {
  const query = '(prefers-reduced-motion: reduce)';
  const [reduced, setReduced] = useState(() => typeof window.matchMedia === 'function' && window.matchMedia(query).matches);
  useEffect(() => {
    if (typeof window.matchMedia !== 'function') return;
    const media = window.matchMedia(query);
    const update = () => setReduced(media.matches);
    media.addEventListener?.('change', update);
    return () => media.removeEventListener?.('change', update);
  }, []);
  return reduced;
}

export default function LessonAnimation({ node, svg }: { node: AnimationNode; svg: (ref: (element: SVGSVGElement | null) => void, labelledBy: string) => ReactNode }) {
  const figure = useRef<HTMLElement>(null);
  const timeline = useRef<Timeline | null>(null);
  const reduced = useReducedMotion();
  const steps = node.mode === 'steps' ? node.steps ?? [] : [];
  const [step, setStep] = useState(0);
  const [stepsPlaying, setStepsPlaying] = useState(false);
  const [visible, setVisible] = useState(false);
  // What the student asked for in a cycle: null follows the automatic rule (on screen
  // and without reduced motion); a pause holds until they press play again.
  const [intent, setIntent] = useState<null | 'play' | 'pause'>(null);
  const playing = node.mode === 'steps' ? stepsPlaying : intent === 'play' || (intent === null && visible && !reduced);
  const titleId = node.id + '-title', descriptionId = node.id + '-description';

  // Start still, before paint: first step, or the poster frame of a cycle.
  useLayoutEffect(() => { seek(timeline.current, node.mode === 'steps' ? steps[0].time : node.poster); }, [node]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const element = figure.current;
    if (node.mode !== 'cycle' || !element || typeof IntersectionObserver !== 'function') return;
    let wasVisible = false;
    const observer = new IntersectionObserver(entries => {
      const isVisible = entries.some(entry => entry.isIntersecting);
      // Leaving the screen stops a cycle the student started; a pause they chose persists.
      // Only a real exit counts: a play pressed before the first report is kept.
      if (wasVisible && !isVisible) setIntent(current => current === 'play' ? null : current);
      wasVisible = isVisible;
      setVisible(isVisible);
    }, { threshold: 0.5 });
    observer.observe(element);
    return () => observer.disconnect();
  }, [node.mode]);

  useEffect(() => {
    const svg = timeline.current;
    if (!playing || !svg) { if (svg) svg.pauseAnimations?.(); return; }
    if (node.mode === 'cycle' && (now(svg) >= node.duration || reduced)) svg.setCurrentTime?.(0);
    svg.unpauseAnimations?.();
    let frame = 0;
    const tick = () => {
      const time = now(svg);
      if (node.mode === 'steps') {
        let current = 0;
        steps.forEach((item, index) => { if (time >= item.time) current = index; });
        setStep(current);
        if (time >= node.duration) { seek(svg, steps[steps.length - 1].time); setStep(steps.length - 1); setStepsPlaying(false); return; }
      } else if (time >= node.duration) {
        svg.setCurrentTime?.(0);
      }
      frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [playing]); // eslint-disable-line react-hooks/exhaustive-deps

  const go = (index: number) => { setStepsPlaying(false); setStep(index); seek(timeline.current, steps[index].time); };
  const playAll = () => {
    if (playing) { setStepsPlaying(false); return; }
    // From the current step onwards; from the beginning after the last one.
    seek(timeline.current, step === steps.length - 1 ? 0 : steps[step].time);
    setStepsPlaying(true);
  };
  const toggleCycle = () => {
    if (playing) { setIntent('pause'); seek(timeline.current, now(timeline.current)); }
    else setIntent('play');
  };

  return <figure ref={figure} id={node.id} className="native-animation" aria-labelledby={titleId}>
    <figcaption id={titleId}>{node.title}</figcaption>
    <p className="native-animation-help">Deslize a figura para o lado para ver tudo.</p>
    <div className="native-animation-stage" role="region" aria-label={'Figura animada: ' + node.title} tabIndex={0}>{svg(element => { timeline.current = element; }, titleId + ' ' + descriptionId)}</div>
    <div className="native-animation-controls" role="group" aria-label={'Controles da animação: ' + node.title}>
      {node.mode === 'steps' ? <>
        <button type="button" className="secondary" onClick={() => go(step - 1)} disabled={step === 0}>Anterior</button>
        <span className="native-animation-progress">Etapa {step + 1} de {steps.length}</span>
        <button type="button" className="secondary" onClick={() => go(step + 1)} disabled={step === steps.length - 1}>Próxima</button>
        <button type="button" className="secondary" onClick={playAll}>{playing ? 'Pausar' : 'Reproduzir tudo'}</button>
      </> : <button type="button" className="secondary" onClick={toggleCycle}>{playing ? 'Pausar' : 'Reproduzir'}</button>}
    </div>
    {node.mode === 'steps' && <p className="native-animation-caption" aria-live="polite">{steps[step].caption}</p>}
    <p id={descriptionId} className="native-animation-description">{node.description}</p>
  </figure>;
}
