// Lint do frontend: regras de TypeScript com tipos, hooks do React e nada de estilo
// (formatação é do Prettier, por isso eslint-config-prettier vem por último).
import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist/', 'coverage/', 'test-results/', 'playwright-report/', 'node_modules/'] },
  js.configs.recommended,
  ...tseslint.configs.recommendedTypeChecked,
  {
    languageOptions: {
      parserOptions: { project: ['./tsconfig.json', './tsconfig.node.json'], tsconfigRootDir: import.meta.dirname },
    },
  },
  {
    rules: {
      // Prefixo _ marca omissão intencional (desestruturação, parâmetro exigido pela assinatura).
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_', destructuredArrayIgnorePattern: '^_' },
      ],
    },
  },
  {
    files: ['src/**/*.{ts,tsx}', 'test/**/*.ts'],
    languageOptions: { globals: globals.browser },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'error',
    },
  },
  {
    // Requisições à API só pelo cliente único; native-lesson busca documentos estáticos de aula, não a API.
    files: ['src/**/*.{ts,tsx}'],
    ignores: ['src/http-client.ts', 'src/native-lesson.ts', 'src/**/*.test.{ts,tsx}', 'src/test-setup.ts'],
    rules: {
      'no-restricted-globals': ['error', { name: 'fetch', message: 'Use apiFetch de src/http-client.ts.' }],
    },
  },
  {
    files: ['e2e/**/*.ts', '*.config.ts', 'eslint.config.js'],
    languageOptions: { globals: globals.node },
  },
  {
    // Testes e e2e: mocks async sem await, fixtures e page.evaluate retornam any de propósito.
    // As regras de dados não confiáveis valem para o código de produção; o resto continua ativo.
    files: ['src/**/*.test.{ts,tsx}', 'src/test-setup.ts', 'test/**/*.ts', 'e2e/**/*.ts'],
    rules: {
      '@typescript-eslint/no-unsafe-assignment': 'off',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      '@typescript-eslint/no-unsafe-argument': 'off',
      '@typescript-eslint/no-unsafe-return': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/require-await': 'off',
      '@typescript-eslint/unbound-method': 'off',
      // mock.calls tipa a URL como RequestInfo | URL; as chamadas do app usam string.
      '@typescript-eslint/no-base-to-string': 'off',
    },
  },
  {
    files: ['eslint.config.js'],
    ...tseslint.configs.disableTypeChecked,
  },
  prettier,
);
