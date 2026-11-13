import { useState } from 'react';
import { applyThemeChoice, readThemeChoice, type ThemeChoice } from './theme';

const labels: Record<ThemeChoice, string> = { auto: 'Automático', light: 'Claro', dark: 'Escuro' };

export default function ThemeSelect() {
  const [choice, setChoice] = useState<ThemeChoice>(readThemeChoice);
  return <label className="theme-select">Tema
    <select value={choice} onChange={event => { const next = event.target.value as ThemeChoice; applyThemeChoice(next); setChoice(next); }}>
      {(Object.keys(labels) as ThemeChoice[]).map(value => <option key={value} value={value}>{labels[value]}</option>)}
    </select>
  </label>;
}
