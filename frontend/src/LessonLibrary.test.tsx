import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { expect, it, vi } from 'vitest';
import LessonLibrary from './LessonLibrary';
import catalog from './lesson-catalog.json';
import paths from './lesson-paths.json';
import manifest from '../../manifesto_v3.json';
import { lessonUrl } from './lesson-links';

it('catálogo de aulas cobre exatamente os 51 caminhos públicos do manifesto', () => {
  expect(catalog.topics.map(t => t.id)).toEqual(manifest.modulos.map(m => m.id));
  expect(new Set(catalog.topics.map(t => t.id)).size).toBe(51);
  for (const topic of catalog.topics) {
    expect(catalog.disciplines.some(d => d.id === topic.disciplineId)).toBe(true);
    expect(manifest.modulos.find(m => m.id === topic.id)?.modulo).toContain(topic.title);
    expect(Object.hasOwn(paths, topic.id)).toBe(true);
  }
});
it('busca sem acento combina disciplina, mantém estado vazio explícito e limpa filtros', () => {
  render(<LessonLibrary onPractice={vi.fn()} />);
  expect(screen.getByRole('status')).toHaveTextContent('51 aulas encontradas.');
  fireEvent.change(screen.getByLabelText('Buscar aula'), { target: { value: 'interpretacao' } });
  expect(screen.getByRole('status')).toHaveTextContent('1 aula encontrada.');
  fireEvent.change(screen.getByLabelText('Filtrar disciplina'), { target: { value: 'informatica' } });
  expect(screen.getByRole('heading', { name: 'Nenhuma aula por aqui' })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Limpar busca e filtros' }));
  expect(screen.getByRole('status')).toHaveTextContent('51 aulas encontradas.');
});
it('leitura usa teoria completa conhecida e permite navegar e compor sem criar tentativa', async () => {
  const practice = vi.fn();render(<LessonLibrary initialDiscipline="conhecimentos-escolares" onPractice={practice} />);
  fireEvent.click(screen.getByRole('button', { name: /S02.*Escrituração Escolar/ }));
  await waitFor(() => expect(screen.getByRole('heading', { name: 'Escrituração Escolar' })).toHaveFocus());
  expect(screen.getByTitle('Aula completa: Escrituração Escolar')).toHaveAttribute('src', lessonUrl('S02'));
  expect(screen.getByRole('link', { name: 'Abrir aula original (nova aba)' })).toHaveAttribute('href', lessonUrl('S02'));
  expect(screen.getByLabelText('Cargo para praticar esta aula')).toHaveValue('secretario-de-escola');
  expect(within(screen.getByLabelText('Cargo para praticar esta aula')).getAllByRole('option')).toHaveLength(1);
  expect(practice).not.toHaveBeenCalled();fireEvent.click(screen.getByRole('button', { name: 'Praticar este assunto' }));
  expect(practice).toHaveBeenCalledWith({ roleId: 'secretario-de-escola', disciplineId: 'conhecimentos-escolares', topicId: 'S02' });
  fireEvent.click(screen.getByRole('button', { name: 'Próxima aula' }));
  expect(screen.getByRole('heading', { name: 'Registros Individuais, Ficha e Histórico Escolar' })).toHaveFocus();
});
