import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ThemeSelect from './ThemeSelect';
import { THEME_KEY } from './theme';

afterEach(() => {
  vi.restoreAllMocks();
  window.localStorage.clear();
  document.documentElement.removeAttribute('data-theme');
});

describe('ThemeSelect', () => {
  it('começa em automático sem preferência salva', () => {
    render(<ThemeSelect />);
    expect(screen.getByLabelText('Tema')).toHaveValue('auto');
    expect(document.documentElement).not.toHaveAttribute('data-theme');
  });

  it('aplica e guarda só a preferência visual escolhida', () => {
    render(<ThemeSelect />);
    fireEvent.change(screen.getByLabelText('Tema'), { target: { value: 'dark' } });
    expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
    expect(window.localStorage.getItem(THEME_KEY)).toBe('dark');
    expect(Object.keys(window.localStorage)).toEqual([THEME_KEY]);
  });

  it('volta a seguir o sistema ao escolher automático', () => {
    window.localStorage.setItem(THEME_KEY, 'light');
    document.documentElement.setAttribute('data-theme', 'light');
    render(<ThemeSelect />);
    expect(screen.getByLabelText('Tema')).toHaveValue('light');
    fireEvent.change(screen.getByLabelText('Tema'), { target: { value: 'auto' } });
    expect(document.documentElement).not.toHaveAttribute('data-theme');
    expect(window.localStorage.getItem(THEME_KEY)).toBeNull();
  });

  it('continua funcionando quando o armazenamento recusa acesso', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('bloqueado'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('bloqueado'); });
    render(<ThemeSelect />);
    expect(screen.getByLabelText('Tema')).toHaveValue('auto');
    fireEvent.change(screen.getByLabelText('Tema'), { target: { value: 'light' } });
    expect(document.documentElement).toHaveAttribute('data-theme', 'light');
  });
});
