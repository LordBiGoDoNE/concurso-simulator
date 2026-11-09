import { expect, it } from 'vitest';
import catalog from './lesson-catalog.json';
import { nativeLessonUrl } from './lesson-links';

it('os 51 atalhos abrem a aula nativa, sem referência a arquivos estáticos', () => {
  for (const topic of catalog.topics) expect(nativeLessonUrl(topic.id)).toBe('#/aulas/' + topic.id);
});
it.each(['../P01', '%2e%2e/P01', 'javascript:alert(1)', 'https://evil.test', '__proto__', 'constructor', 'P01?gabarito'])('recusa assunto desconhecido: %s', id => {
  expect(nativeLessonUrl(id)).toBeNull();
});
it('preserva cargo compatível, permite leitura livre de outro cargo e recusa desconhecido', () => {
  expect(nativeLessonUrl('P01', 'secretario-de-escola')).toBe('#/aulas/P01?cargo=secretario-de-escola');
  expect(nativeLessonUrl('S02', 'assistente-administrativo')).toBe('#/aulas/S02');
  expect(nativeLessonUrl('P01', 'cargo-desconhecido')).toBeNull();
  expect(nativeLessonUrl('M04')).toBe('#/aulas/M04');
});
