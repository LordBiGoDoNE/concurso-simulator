import { fireEvent, render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import StudyHome from './StudyHome';
import { lessons, lessonSupportsRole, studyCatalog } from './study-catalog';

const noop = () => {};

describe('StudyHome', () => {
  it.each(['', ...studyCatalog.roles.map(role => role.id)])('sugere a primeira aula disponível para o cargo "%s" e a abre', roleId => {
    const expected = lessons.find(topic => lessonSupportsRole(topic, roleId))!;
    const onOpenLesson = vi.fn();
    render(<StudyHome roleId={roleId} onRoleChange={noop} onLessons={noop} onCompose={noop} onQuestions={noop} onOpenLesson={onOpenLesson} />);
    const suggestion = screen.getByRole('complementary', { name: 'Sugestão para começar' });
    expect(suggestion).toHaveTextContent(`${expected.id} · ${expected.title}`);
    fireEvent.click(within(suggestion).getByRole('button', { name: 'Abrir aula' }));
    expect(onOpenLesson).toHaveBeenCalledWith(expected.id);
  });
});
