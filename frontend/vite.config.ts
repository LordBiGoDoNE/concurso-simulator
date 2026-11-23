import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // css.include: os testes de tokens leem as folhas de estilo com ?raw (sem isso o Vitest devolve vazio).
  test: {
    environment: 'jsdom', setupFiles: ['./src/test-setup.ts'], include: ['src/**/*.test.{ts,tsx}'], css: { include: [/\.css\?raw$/] },
    // Cobertura só como relatório local (npm run test:coverage), sem limite mínimo.
    coverage: {
      provider: 'v8', reportsDirectory: 'coverage', reporter: ['text-summary', 'html', 'lcov'],
      include: ['src/**/*.{ts,tsx}'], exclude: ['src/**/*.test.{ts,tsx}', 'src/test-setup.ts', 'src/vite-env.d.ts'],
    },
  },
});
