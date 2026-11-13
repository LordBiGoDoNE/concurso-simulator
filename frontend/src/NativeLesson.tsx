import { createElement, useEffect, useState, type ReactNode } from 'react';
import { loadNativeLesson, type LessonNode, type NativeLessonDocument } from './native-lesson';

const reactAttributes: Record<string, string> = { colspan: 'colSpan', rowspan: 'rowSpan',
  'stroke-width': 'strokeWidth', 'stroke-dasharray': 'strokeDasharray', 'stroke-linecap': 'strokeLinecap',
  'stroke-linejoin': 'strokeLinejoin', 'fill-rule': 'fillRule', 'fill-opacity': 'fillOpacity', 'stroke-opacity': 'strokeOpacity',
  'font-family': 'fontFamily', 'font-size': 'fontSize', 'font-weight': 'fontWeight', 'font-style': 'fontStyle',
  'text-anchor': 'textAnchor', 'dominant-baseline': 'dominantBaseline', 'marker-end': 'markerEnd' };
function renderNode(node: LessonNode, key: number): ReactNode {
  if (typeof node === 'string') return node;
  if ('value' in node) {
    if (node.tag === 'percentage-grid') return <div key={key} className="native-percentage-grid" role="img" aria-label={node.label}>{Array.from({ length: 100 }, (_, i) => <span key={i} className={i < node.value ? 'filled' : undefined} />)}</div>;
    return <div key={key} className="native-percentage-bar" role="img" aria-label={node.labels.join('; ')}><div aria-hidden="true" style={{ gridTemplateColumns: `${node.value}fr ${100 - node.value}fr` }}><span /><span /></div><p>{node.labels.join(' · ')}</p></div>;
  }
  const props: Record<string, unknown> = { key };
  for (const [attr, value] of Object.entries(node.attrs ?? {})) props[reactAttributes[attr] ?? attr] = value;
  if (node.tone) props.className = 'native-' + node.tone;
  const children = node.children.map(renderNode);
  if (node.tag === 'a' && String(props.href).startsWith('#')) {
    const target = String(props.href).slice(1);
    return <button key={key} type="button" className="native-anchor" onClick={() => {
      const element = document.getElementById(target);
      if (element) { element.tabIndex = -1; element.focus({ preventScroll: true }); element.scrollIntoView({ block: 'start' }); }
    }}>{children}</button>;
  }
  if (node.tag === 'a') Object.assign(props, { target: '_blank', rel: 'noopener noreferrer', referrerPolicy: 'no-referrer' });
  if (node.tag === 'svg') return <div key={key}><p className="native-diagram-help">Percorra o esquema na horizontal para ver os detalhes.</p><div className="native-diagram-view" role="region" aria-label="Esquema da aula" tabIndex={0}>{createElement('svg', props, children)}</div></div>;
  if (node.tag === 'table') return <div key={key} className="native-table" role="region" aria-label="Tabela da aula" tabIndex={0}>{createElement('table', props, children)}</div>;
  return createElement(node.tag, props, ...(node.tag === 'br' ? [] : children));
}

export default function NativeLesson({ topicId }: { topicId: string }) {
  const [retry, setRetry] = useState(0);
  const [state, setState] = useState<{ topicId: string; phase: 'loading' | 'error' } | { topicId: string; phase: 'ready'; document: NativeLessonDocument }>({ topicId, phase: 'loading' });
  useEffect(() => {
    const controller = new AbortController(); let active = true;
    setState({ topicId, phase: 'loading' });
    const timer = window.setTimeout(() => { controller.abort(); if (active) setState({ topicId, phase: 'error' }); }, 12000);
    void loadNativeLesson(topicId, controller.signal).then(document => {
      if (active && !controller.signal.aborted) setState({ topicId, phase: 'ready', document });
    }).catch(() => { if (active) setState({ topicId, phase: 'error' }); }).finally(() => window.clearTimeout(timer));
    return () => { active = false; window.clearTimeout(timer); controller.abort(); };
  }, [topicId, retry]);
  const current = state.topicId === topicId ? state : { phase: 'loading' as const };
  if (current.phase === 'loading') return <p role="status">Carregando aula…</p>;
  if (current.phase === 'error') return <div className="native-load-error"><p role="alert">Não foi possível carregar esta aula.</p><button className="secondary" onClick={() => setRetry(value => value + 1)}>Tentar carregar aula novamente</button></div>;
  if (current.phase !== 'ready') return null;
  return <article className="native-lesson" aria-label={'Conteúdo da aula: ' + current.document.title}>{current.document.children.map(renderNode)}</article>;
}
