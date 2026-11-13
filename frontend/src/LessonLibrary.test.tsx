import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import LessonLibrary from './LessonLibrary';
import catalog from './lesson-catalog.json';
import paths from './lesson-paths.json';
import manifest from '../../manifesto_v3.json';
import s02 from './lesson-documents/S02.json';
import s03 from './lesson-documents/S03.json';

afterEach(() => vi.unstubAllGlobals());

it('catálogo de aulas cobre exatamente os 51 caminhos públicos do manifesto', () => {
  expect(catalog.topics.map(t => t.id)).toEqual(manifest.modulos.map(m => m.id));
  expect(new Set(catalog.topics.map(t => t.id)).size).toBe(51);
  for (const topic of catalog.topics) {
    expect(catalog.disciplines.some(d => d.id === topic.disciplineId)).toBe(true);
    expect(manifest.modulos.find(m => m.id === topic.id)?.modulo).toContain(topic.title);
    expect(Object.hasOwn(paths, topic.id)).toBe(true);
  }
});
it('pede a escolha de disciplina antes de listar e mostra só as aulas dela', () => {
  render(<LessonLibrary />);
  expect(screen.getByRole('status')).toHaveTextContent('Escolha uma disciplina ou busque um assunto para ver as aulas.');
  expect(screen.queryByRole('list')).not.toBeInTheDocument();
  expect(screen.queryByRole('heading', { name: 'Nenhuma aula por aqui' })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /^Português/ }));
  expect(screen.getByRole('status')).toHaveTextContent('10 aulas encontradas.');
  const items = within(screen.getByRole('list')).getAllByRole('button');
  expect(items).toHaveLength(10);
  expect(items.every(item => /^P\d/.test(item.textContent || ''))).toBe(true);
  expect(within(items[0]).queryByText('Português')).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Recomeçar a escolha' }));
  expect(screen.getByRole('status')).toHaveTextContent('Escolha uma disciplina ou busque um assunto para ver as aulas.');
});
it('busca sem acento atravessa disciplinas, mantém estado vazio explícito e limpa filtros', () => {
  render(<LessonLibrary />);
  fireEvent.change(screen.getByLabelText('Buscar aula'), { target: { value: 'interpretacao' } });
  expect(screen.getByRole('status')).toHaveTextContent('1 aula encontrada.');
  expect(screen.getByRole('button', { name: /P01.*Interpretação de Textos.*Português/ })).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText('Filtrar disciplina'), { target: { value: 'informatica' } });
  expect(screen.getByRole('heading', { name: 'Nenhuma aula por aqui' })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Limpar busca e filtros' }));
  expect(screen.getByRole('status')).toHaveTextContent('Escolha uma disciplina ou busque um assunto para ver as aulas.');
});
it('leitura usa teoria completa conhecida e navega sem criar tentativa', async () => {
  vi.stubGlobal('fetch', vi.fn(async input => ({ ok: true, json: async () => String(input).includes('S03') ? s03 : s02 })));
  render(<LessonLibrary initialDiscipline="conhecimentos-escolares" />);
  fireEvent.click(screen.getByRole('button', { name: /S02.*Escrituração Escolar/ }));
  await waitFor(() => expect(screen.getByRole('heading', { name: 'Escrituração Escolar' })).toHaveFocus());
  expect(await screen.findByRole('article', { name: 'Conteúdo da aula: Escrituração Escolar' })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Começar prática' })).not.toBeInTheDocument();
  expect(document.querySelector('iframe')).toBeNull();
  expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  expect(fetch).toHaveBeenCalledTimes(1);
  fireEvent.click(screen.getByRole('button', { name: 'Próxima aula' }));
  expect(screen.getByRole('heading', { name: 'Registros Individuais, Ficha e Histórico Escolar' })).toHaveFocus();
  await screen.findByRole('article', { name: 'Conteúdo da aula: Registros Individuais, Ficha e Histórico Escolar' });
});
