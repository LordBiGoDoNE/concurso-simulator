export type LessonNode = string | { tag: string; attrs?: Record<string, string>; tone?: string; children: LessonNode[] }
  | { tag: 'percentage-grid'; value: number; label: string }
  | { tag: 'percentage-bar'; value: number; labels: string[] };
export interface NativeLessonDocument { format: 'native-lesson/v1'; topicId: string; title: string; children: LessonNode[] }

const tags = new Set('section aside div span p h2 h3 h4 ul ol li strong b em i small br code var sub sup table thead tbody tr th td caption figure figcaption details summary a math mrow mi mn mo mfrac msup msub msqrt mtext mroot mtable mtr mtd svg g path rect circle ellipse line polyline polygon text tspan title desc defs marker'.split(' '));
const attributes = new Set('id href type role aria-label aria-labelledby aria-hidden scope colspan rowspan start display xmlns viewBox width height x y dx dy x1 y1 x2 y2 cx cy r rx ry points d transform fill stroke stroke-width stroke-dasharray stroke-linecap stroke-linejoin fill-rule opacity fill-opacity stroke-opacity font-family font-size font-weight font-style text-anchor dominant-baseline markerWidth markerHeight refX refY orient marker-end'.split(' '));
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
  function check(node: unknown, depth: number): void {
    if (++count > 15000 || depth > 60) return invalid();
    if (typeof node === 'string') return;
    if (!record(node) || typeof node.tag !== 'string') return invalid();
    if (node.tag === 'percentage-grid' || node.tag === 'percentage-bar') {
      if (!Number.isInteger(node.value) || Number(node.value) < 0 || Number(node.value) > 100) return invalid();
      if (node.tag === 'percentage-grid' ? typeof node.label !== 'string' : !Array.isArray(node.labels) || node.labels.length !== 2 || node.labels.some(label => typeof label !== 'string')) return invalid();
      return;
    }
    if (!tags.has(node.tag) || !Array.isArray(node.children) || (node.tone !== undefined && !tones.has(String(node.tone)))) return invalid();
    if (node.attrs !== undefined) {
      if (!record(node.attrs)) return invalid();
      for (const [name, val] of Object.entries(node.attrs)) {
        if (!attributes.has(name) || typeof val !== 'string') return invalid();
        if (name === 'id') {
          if (!val.startsWith(prefix) || !/^[\w.-]+$/.test(val) || ids.has(val)) return invalid();
          ids.add(val);
        }
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
    node.children.forEach(child => check(child, depth + 1));
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
