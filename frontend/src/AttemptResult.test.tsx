import { fireEvent, render, screen, within } from '@testing-library/react';
import { expect, it, vi } from 'vitest';
import AttemptResult from './AttemptResult';
import type { Result } from './attempt-api';
import { result, view } from '../test/attempt-fixtures';

it('preserva texto e descrição da figura nas alternativas escolhida e correta', () => {
  const resource = (name: string, description: string) => ({ path: `assets/${name}.svg`, mediaType: 'image/svg+xml', sha256: 'a'.repeat(64), description });
  const selected = { ...view.items[0].alternatives[0], content: { text: 'Observe o gráfico escolhido.', resources: [resource('chosen', 'Barras crescentes da escolha.')] } };
  const correct = { ...view.items[0].alternatives[1], content: { text: 'Observe o mapa correto.', resources: [resource('correct', 'Mapa com dois bairros conectados.')] } };
  render(<AttemptResult result={{ ...result, wrong: 1, blank: 0, items: [{ item: { ...view.items[0], selectedAlternativeId: selected.id, alternatives: [selected, correct] }, correctAlternativeId: correct.id, correctAnswer: correct.content, explanation: result.items[0].explanation }] }} />);
  expect(screen.getByText('Observe o gráfico escolhido.')).toBeInTheDocument();expect(screen.getByText('Descrição da figura: Barras crescentes da escolha.')).toBeInTheDocument();
  expect(screen.getByText('Observe o mapa correto.')).toBeInTheDocument();expect(screen.getByText('Descrição da figura: Mapa com dois bairros conectados.')).toBeInTheDocument();
  expect(screen.getByText('0/1 pontos — 0%.')).toBeInTheDocument();expect(screen.getByRole('heading', { name: 'Resultado do simulado' })).toHaveFocus();
});

// Três questões: 1 correta (P07), 2 errada (P01), 3 em branco (tópico fora do catálogo de aulas).
const [a, b] = view.items[0].alternatives;
const question = (n: number, topicId: string, selectedAlternativeId: string | null) => ({
  item: { ...view.items[0], id: `item-${n}`, classification: { ...view.items[0].classification, topicId }, statement: { text: `Enunciado ${n}`, resources: [] }, selectedAlternativeId },
  correctAlternativeId: a.id, correctAnswer: a.content, explanation: { text: `Resolução ${n}`, resources: [] },
});
const mixed: Result = { total: 3, points: 1, correct: 1, wrong: 1, blank: 1, percentage: 33.33, items: [question(1, 'P07', a.id), question(2, 'P01', b.id), question(3, 'SEM-AULA', null)] };

it('resume a proporção com legenda numérica e rotula cada questão em texto', () => {
  render(<AttemptResult result={mixed} />);
  expect(screen.getByRole('img', { name: '1 acertos, 1 erros e 1 em branco de 3 questões' })).toBeInTheDocument();
  const legend = screen.getByRole('list', { name: 'Legenda do resultado' }); expect(legend).toHaveTextContent('1 acertos'); expect(legend).toHaveTextContent('1 erros'); expect(legend).toHaveTextContent('1 em branco');
  const articles = screen.getAllByRole('article');
  expect(within(articles[0]).getByText('Correta')).toBeInTheDocument(); expect(within(articles[1]).getByText('Errada')).toBeInTheDocument(); expect(within(articles[2]).getByText('Em branco')).toBeInTheDocument();
});

it('filtra erros e em branco mantendo a numeração original', () => {
  render(<AttemptResult result={mixed} />);
  fireEvent.click(screen.getByRole('button', { name: 'Erros e em branco (2)' }));
  expect(screen.queryByRole('heading', { name: 'Revisão da questão 1' })).not.toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Revisão da questão 2' })).toBeInTheDocument(); expect(screen.getByRole('heading', { name: 'Revisão da questão 3' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Erros e em branco (2)' })).toHaveAttribute('aria-pressed', 'true');
  fireEvent.click(screen.getByRole('button', { name: 'Todas (3)' })); expect(screen.getAllByRole('article')).toHaveLength(3);
});

it('informa quando não há erros nem em branco no filtro', () => {
  render(<AttemptResult result={{ ...mixed, total: 1, wrong: 0, blank: 0, percentage: 100, items: [mixed.items[0]] }} />);
  fireEvent.click(screen.getByRole('button', { name: 'Erros e em branco (0)' }));
  expect(screen.getByRole('status')).toHaveTextContent('Nenhum erro ou questão em branco'); expect(screen.queryAllByRole('article')).toHaveLength(0);
});

it('oferece rever a aula e praticar o assunto pelo topicId, só quando a aula existe', () => {
  const review = vi.fn(), practice = vi.fn();
  render(<AttemptResult result={mixed} shortcuts={{ review, practice }} />);
  fireEvent.click(screen.getByRole('button', { name: 'Rever a aula P01, questão 2' })); expect(review).toHaveBeenCalledWith('P01');
  fireEvent.click(screen.getByRole('button', { name: 'Praticar este assunto, P07, questão 1' })); expect(practice).toHaveBeenCalledWith('P07');
  expect(within(screen.getAllByRole('article')[2]).queryByRole('button')).not.toBeInTheDocument();
});

it('não exibe atalhos sem os callbacks do resultado', () => {
  render(<AttemptResult result={mixed} />);
  expect(screen.queryByRole('button', { name: /Rever a aula|Praticar este assunto/ })).not.toBeInTheDocument();
});
