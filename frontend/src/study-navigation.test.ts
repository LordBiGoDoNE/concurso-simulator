import { describe, expect, it } from 'vitest';
import { emptyLessonLocation, locationForRole, parseStudyRoute, routeHash } from './study-navigation';

describe('endereços públicos de estudo', () => {
  it('restaura aula, cargo, disciplina e busca com codificação segura', () => {
    const route = { ...emptyLessonLocation, area: 'lessons' as const, topicId: 'P01', roleId: 'secretario-de-escola', disciplineId: 'portugues', search: 'interpretação & texto' };
    expect(parseStudyRoute(routeHash(route))).toEqual({ route, notice: '' });
    for (const area of ['home','practice','attempts','questions'] as const) {
      expect(parseStudyRoute(routeHash({ ...route, area })).route).toEqual({ ...route, area });
    }
  });
  it.each(['#/segredo/123', '#/aulas/../auth', '#/aulas/UUID', '#/simulados/123', '#/aulas?cargo=admin', '#/aulas?cargo=assistente-administrativo&cargo=secretario-de-escola', '#/aulas?respostas=A&token=segredo'])('descarta opções desconhecidas em %s', hash => {
    const result = parseStudyRoute(hash);
    expect(result.notice).not.toBe('');
    expect(routeHash(result.route)).not.toMatch(/admin$|segredo|token|UUID|respostas/);
  });
  it('não transforma cargo e aula incompatíveis em outro cargo silenciosamente', () => {
    const result = parseStudyRoute('#/aulas/S01?cargo=assistente-administrativo&disciplina=conhecimentos-escolares');
    expect(result.notice).not.toBe('');
    expect(result.route).toMatchObject({ area: 'lessons', roleId: 'assistente-administrativo', disciplineId: '', topicId: '' });
  });
  it('trocar cargo conserva busca e remove apenas aula e disciplina incompatíveis', () => {
    const previous = { roleId: 'assistente-administrativo', topicId: 'A01', disciplineId: 'conhecimentos-administrativos', search: 'arquivo' };
    expect(locationForRole(previous, 'secretario-de-escola')).toEqual({ roleId: 'secretario-de-escola', topicId: '', disciplineId: '', search: 'arquivo' });
  });
  it('limita busca e remove caracteres de controle sem aceitar parâmetros privados', () => {
    const result = parseStudyRoute('#/aulas?busca=' + encodeURIComponent('\u0000' + 'a'.repeat(180)));
    expect(result.route.search).toBe('a'.repeat(160));
    expect(result.notice).not.toBe('');
  });
});
