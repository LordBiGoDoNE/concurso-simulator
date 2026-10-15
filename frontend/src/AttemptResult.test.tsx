import { render, screen } from '@testing-library/react';
import { expect, it } from 'vitest';
import AttemptResult from './AttemptResult';
import { result, view } from '../test/attempt-fixtures';

it('preserva texto e descrição da figura nas alternativas escolhida e correta', () => {
  const resource = (name: string, description: string) => ({ path: `assets/${name}.svg`, mediaType: 'image/svg+xml', sha256: 'a'.repeat(64), description });
  const selected = { ...view.items[0].alternatives[0], content: { text: 'Observe o gráfico escolhido.', resources: [resource('chosen', 'Barras crescentes da escolha.')] } };
  const correct = { ...view.items[0].alternatives[1], content: { text: 'Observe o mapa correto.', resources: [resource('correct', 'Mapa com dois bairros conectados.')] } };
  render(<AttemptResult result={{ ...result, wrong: 1, blank: 0, items: [{ item: { ...view.items[0], selectedAlternativeId: selected.id, alternatives: [selected, correct] }, correctAnswer: correct.content, explanation: result.items[0].explanation }] }} />);
  expect(screen.getByText('Observe o gráfico escolhido.')).toBeInTheDocument();expect(screen.getByText('Descrição da figura: Barras crescentes da escolha.')).toBeInTheDocument();
  expect(screen.getByText('Observe o mapa correto.')).toBeInTheDocument();expect(screen.getByText('Descrição da figura: Mapa com dois bairros conectados.')).toBeInTheDocument();
  expect(screen.getByText('0/1 pontos — 0%.')).toBeInTheDocument();expect(screen.getByRole('heading', { name: 'Resultado do simulado' })).toHaveFocus();
});
