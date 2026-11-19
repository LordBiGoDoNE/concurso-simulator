export interface ElementNode { tag: string; attrs?: Record<string, string>; tone?: string; children: LessonNode[] }
export interface AnimationStep { time: number; caption: string }
export interface AnimationNode { tag: 'animation'; id: string; mode: 'cycle' | 'steps'; title: string; description: string;
  duration: number; poster: number; steps?: AnimationStep[]; svg: ElementNode }
export type LessonNode = string | ElementNode
  | { tag: 'percentage-grid'; value: number; label: string }
  | { tag: 'percentage-bar'; value: number; labels: string[] }
  | AnimationNode;
export interface NativeLessonDocument { format: 'native-lesson/v1'; topicId: string; title: string; children: LessonNode[] }

const tags = new Set('section aside div span p h2 h3 h4 ul ol li strong b em i small br code var sub sup table thead tbody tr th td caption figure figcaption details summary a math mrow mi mn mo mfrac msup msub msqrt mtext mroot mtable mtr mtd svg g path rect circle ellipse line polyline polygon text tspan title desc defs marker'.split(' '));
const attributes = new Set('id href type role aria-label aria-labelledby aria-hidden scope colspan rowspan start display xmlns viewBox width height x y dx dy x1 y1 x2 y2 cx cy r rx ry points d transform fill stroke stroke-width stroke-dasharray stroke-linecap stroke-linejoin fill-rule opacity fill-opacity stroke-opacity font-family font-size font-weight font-style text-anchor dominant-baseline markerWidth markerHeight refX refY orient marker-end'.split(' '));
// Declarative SVG animation (SMIL) is accepted only inside an animation node, and
// only for geometry/visibility: never links, styles, events or external targets.
const animationTags = new Set('animate animateTransform animateMotion mpath set'.split(' '));
const animationAttributes = new Set('attributeName dur begin end repeatCount values keyTimes keySplines calcMode from to by rotate path additive accumulate visibility stroke-dashoffset pathLength'.split(' '));
const timingAttributes = new Set('attributeName dur begin end repeatCount values keyTimes keySplines calcMode from to by rotate path additive accumulate'.split(' '));
const animatable = new Set('opacity fill-opacity stroke-opacity transform x y cx cy r width height stroke-dashoffset visibility'.split(' '));
const transformTypes = new Set('translate scale rotate skewX skewY'.split(' '));
const time = String.raw`\d+(?:\.\d+)?(?:ms|s)?`;
const timePattern = new RegExp('^' + time + '$');
const syncPattern = new RegExp(String.raw`^([A-Za-z][\w.-]*?)\.(?:begin|end)(?:[+-]` + time + ')?$');
const nonNegative = (value: unknown): value is number => typeof value === 'number' && Number.isFinite(value) && value >= 0;
const tones = new Set('section diagram note example solution formula attention summary reference label sources description table steps contents'.split(' '));
const record = (value: unknown): value is Record<string, unknown> => value !== null && typeof value === 'object' && !Array.isArray(value);
export function externalSource(href: string): boolean {
  try { const url = new URL(href); return ['https:', 'http:'].includes(url.protocol) && !url.username && !url.password; } catch { return false; }
}

// Public, generated educational data has a deliberately small rendering vocabulary.
// No HTML parser, styles, scripts or remote lesson documents enter the browser.
export function parseNativeLesson(value: unknown, topicId: string): NativeLessonDocument {
  const invalid = () => { throw new Error('Conteúdo da aula inválido'); };
  if (!record(value) || value.format !== 'native-lesson/v1' || value.topicId !== topicId || typeof value.title !== 'string' || !Array.isArray(value.children)) return invalid();
  const prefix = 'lesson-' + topicId + '-';
  let count = 0; const ids = new Set<string>(), references: string[] = [];
  function animation(node: Record<string, unknown>, depth: number): void {
    const keys = ['tag', 'id', 'mode', 'title', 'description', 'duration', 'poster', 'svg', ...(node.mode === 'steps' ? ['steps'] : [])];
    if (Object.keys(node).length !== keys.length || keys.some(key => !(key in node))) return invalid();
    if (typeof node.id !== 'string' || !node.id.startsWith(prefix + 'animation-') || !/^[\w-]+$/.test(node.id) || ids.has(node.id)) return invalid();
    ids.add(node.id);
    if (!['cycle', 'steps'].includes(String(node.mode)) || typeof node.title !== 'string' || !node.title.trim() || typeof node.description !== 'string' || !node.description.trim()) return invalid();
    if (!nonNegative(node.duration) || node.duration === 0 || !nonNegative(node.poster) || node.poster > node.duration) return invalid();
    if (node.mode === 'steps') {
      if (!Array.isArray(node.steps) || node.steps.length < 1 || node.steps.length > 8) return invalid();
      let previous = -1;
      for (const step of node.steps) {
        if (!record(step) || Object.keys(step).length !== 2 || !nonNegative(step.time) || step.time <= previous || step.time > node.duration) return invalid();
        if (typeof step.caption !== 'string' || !step.caption.trim()) return invalid();
        previous = step.time;
      }
    }
    if (!record(node.svg) || node.svg.tag !== 'svg' || !record(node.svg.attrs) || typeof node.svg.attrs.viewBox !== 'string') return invalid();
    check(node.svg, depth + 1, true);
  }
  function smil(node: Record<string, unknown>, attrs: Record<string, unknown>, parent: string): void {
    const name = attrs.attributeName;
    if (node.tag === 'mpath') {
      const href = attrs.href;
      if (parent !== 'animateMotion' || Object.keys(attrs).length !== 1 || typeof href !== 'string' || !href.startsWith('#' + prefix)) return invalid();
      references.push(href.slice(1));
      return;
    }
    if ((node.tag === 'animate' || node.tag === 'set') && !animatable.has(String(name))) return invalid();
    if (node.tag === 'animateTransform' && (name !== 'transform' || !transformTypes.has(String(attrs.type)))) return invalid();
    if (node.tag === 'animateMotion' && name !== undefined) return invalid();
    if (attrs.fill !== undefined && !['freeze', 'remove'].includes(String(attrs.fill))) return invalid();
    if (attrs.repeatCount !== undefined && attrs.repeatCount !== 'indefinite' && !/^\d+(?:\.\d+)?$/.test(String(attrs.repeatCount))) return invalid();
    if (attrs.dur !== undefined && !timePattern.test(String(attrs.dur))) return invalid();
    for (const key of ['begin', 'end']) {
      if (attrs[key] === undefined) continue;
      for (const part of String(attrs[key]).split(';').map(value => value.trim())) {
        if (timePattern.test(part)) continue;
        const match = syncPattern.exec(part);
        if (!match || !match[1].startsWith(prefix)) return invalid();
        references.push(match[1]);
      }
    }
    if (Object.values(attrs).some(value => /url\s*\(|javascript|</i.test(String(value)))) return invalid();
    if ((node.children as unknown[]).some(child => !record(child) || child.tag !== 'mpath')) return invalid();
  }
  function check(node: unknown, depth: number, animated = false, parent = ''): void {
    if (++count > 15000 || depth > 60) return invalid();
    if (typeof node === 'string') return;
    if (!record(node) || typeof node.tag !== 'string') return invalid();
    if (node.tag === 'animation') return animated ? invalid() : animation(node, depth);
    if (node.tag === 'percentage-grid' || node.tag === 'percentage-bar') {
      if (!Number.isInteger(node.value) || Number(node.value) < 0 || Number(node.value) > 100) return invalid();
      if (node.tag === 'percentage-grid' ? typeof node.label !== 'string' : !Array.isArray(node.labels) || node.labels.length !== 2 || node.labels.some(label => typeof label !== 'string')) return invalid();
      return;
    }
    const motion = animationTags.has(node.tag);
    if (!(tags.has(node.tag) || animated && motion) || !Array.isArray(node.children) || (node.tone !== undefined && !tones.has(String(node.tone)))) return invalid();
    if (motion) smil(node, record(node.attrs) ? node.attrs : {}, parent);
    if (node.attrs !== undefined) {
      if (!record(node.attrs)) return invalid();
      for (const [name, val] of Object.entries(node.attrs)) {
        if (!(attributes.has(name) || animated && animationAttributes.has(name)) || typeof val !== 'string') return invalid();
        if (!motion && timingAttributes.has(name)) return invalid();
        if (name === 'id') {
          if (!val.startsWith(prefix) || !/^[\w.-]+$/.test(val) || ids.has(val)) return invalid();
          ids.add(val);
        }
        if (name === 'href' && node.tag === 'mpath') continue;
        if (name === 'href') {
          if (node.tag !== 'a') return invalid();
          if (val.startsWith('#' + prefix)) references.push(val.slice(1));
          else if (!externalSource(val)) return invalid();
        }
        if (name === 'aria-labelledby') references.push(...val.split(' '));
        if (name === 'xmlns' && !['http://www.w3.org/2000/svg', 'http://www.w3.org/1998/Math/MathML'].includes(val)) return invalid();
        if (/url\s*\(/i.test(val)) {
          const match = /^url\(#([\w.-]+)\)$/.exec(val);
          if (name !== 'marker-end' || !match) return invalid();
          references.push(match[1]);
        }
      }
    }
    node.children.forEach(child => check(child, depth + 1, animated, node.tag as string));
  }
  value.children.forEach(node => check(node, 0));
  if (references.some(id => !ids.has(id))) return invalid();
  return value as unknown as NativeLessonDocument;
}

const assets = import.meta.glob<string>('./lesson-documents/[A-Z][0-9][0-9].json', { eager: true, query: '?url', import: 'default' });
export async function loadNativeLesson(topicId: string, signal: AbortSignal): Promise<NativeLessonDocument> {
  const url = assets['./lesson-documents/' + topicId + '.json'];
  if (!url) throw new Error('Aula desconhecida');
  const response = await fetch(url, { signal, credentials: 'omit', referrerPolicy: 'no-referrer' });
  if (!response.ok) throw new Error('Aula indisponível');
  return parseNativeLesson(await response.json(), topicId);
}
