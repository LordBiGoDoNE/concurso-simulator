import paths from './lesson-paths.json';

export const materialUrl = import.meta.env.VITE_MATERIAL_URL || 'https://lordbigodone.github.io/concurso-simulator/';

// Only the versioned public conteudo map can supply a path. Never use API content or gabarito.
export function lessonUrl(topicId: string, base = materialUrl): string | null {
  if (!Object.hasOwn(paths, topicId)) return null;
  const path = paths[topicId as keyof typeof paths];
  if (path.split('/').some(segment => !segment || segment === '.' || segment === '..') || /[\\%?#:]/.test(path) || !path.endsWith('.html') || /gabarito/i.test(path)) return null;
  try {
    const url = new URL(base);
    if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) return null;
    url.pathname = url.pathname.replace(/\/$/, '') + '/';
    return new URL(path.split('/').map(encodeURIComponent).join('/'), url).href;
  } catch { return null; }
}
