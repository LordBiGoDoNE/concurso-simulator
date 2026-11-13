// Preferência visual de tema. Guarda só 'light' | 'dark' (ausente = automático),
// nunca respostas, identidade ou dados de tentativa. O index.html aplica a mesma
// chave antes da primeira pintura; mantenha THEME_KEY sincronizada com ele.
export type ThemeChoice = 'auto' | 'light' | 'dark';
export const THEME_KEY = 'concurso-simulator.theme';

export function readThemeChoice(): ThemeChoice {
  try {
    const value = window.localStorage.getItem(THEME_KEY);
    return value === 'light' || value === 'dark' ? value : 'auto';
  } catch {
    return 'auto';
  }
}

export function applyThemeChoice(choice: ThemeChoice): void {
  const root = document.documentElement;
  if (choice === 'auto') root.removeAttribute('data-theme');
  else root.setAttribute('data-theme', choice);
  try {
    if (choice === 'auto') window.localStorage.removeItem(THEME_KEY);
    else window.localStorage.setItem(THEME_KEY, choice);
  } catch {
    // Armazenamento indisponível: o tema vale só nesta página.
  }
}
