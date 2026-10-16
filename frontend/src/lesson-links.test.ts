import { expect, it } from 'vitest';
import manifest from '../../manifesto_v3.json';
const files = import.meta.glob('../../{01_Portugues,02_Matematica_Logica,03_Informatica,04_Assistente_Administrativo,05_Secretario_Escola}/*.html', { query: '?raw', import: 'default' });
import paths from './lesson-paths.json';
import { lessonUrl } from './lesson-links';

it('rastreia exatamente os 51 conteudo pÃºblicos do manifesto, todos existentes e distintos dos gabaritos', () => {
  expect(Object.keys(paths)).toHaveLength(51);
  expect(paths).toEqual(Object.fromEntries(manifest.modulos.map(m => [m.id, m.conteudo])));
  for (const m of manifest.modulos) {
    const href = lessonUrl(m.id, 'https://example.test/material/subpath')!;
    expect(decodeURIComponent(new URL(href).pathname)).toBe('/material/subpath/' + m.conteudo);
    expect(href).not.toContain(' ');expect(href).not.toContain('PortuguÃªs');
    expect(m.conteudo).not.toBe(m.gabarito);expect(Object.hasOwn(files, '../../' + m.conteudo)).toBe(true);
  }
});
it.each(['../P01', '%2e%2e/P01', 'javascript:alert(1)', 'https://evil.test', '__proto__', 'constructor', 'P01?gabarito'])('rejeita chave fora do mapa: %s', id => {
  expect(lessonUrl(id)).toBeNull();
});
it.each(['javascript:alert(1)', 'data:text/html,hi', 'https://user:password@example.test/', 'https://example.test/?query=x', 'https://example.test/#x', '//example.test', 'not a url'])('rejeita base invÃ¡lida: %s', base => {
  expect(lessonUrl('P01', base)).toBeNull();
});
it('codifica espaÃ§o e acento uma Ãºnica vez, preservando subpath e permitindo material local', () => {
  const url = lessonUrl('P01', 'http://127.0.0.1:8082/estudo%20local/')!;
  expect(url).toContain('/estudo%20local/01_Portugues/Portugu%C3%AAs%2001%20-');expect(url).not.toContain('%2520');
});
