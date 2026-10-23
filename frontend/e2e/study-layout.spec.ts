import { test, expect } from '@playwright/test';
import { catalog, emptyPage } from '../test/question-fixtures';
import { view } from '../test/attempt-fixtures';
import { readFileSync } from 'node:fs';
import { resolve, sep } from 'node:path';
const paths: Record<string, string> = JSON.parse(readFileSync(resolve('src/lesson-paths.json'), 'utf8'));

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/**', route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith('/status')) return route.fulfill({ json: { status: 'UP' } });
    if (path.endsWith('/auth/config')) return route.fulfill({ json: { googleEnabled: false } });
    if (path.endsWith('/me')) return route.fulfill({ status: 401, json: { error: 'unauthenticated' } });
    if (path.endsWith('/catalog')) return route.fulfill({ json: catalog });
    if (path.includes('/question-bank/questions')) return route.fulfill({ json: emptyPage });
    return route.fulfill({ status: 503, json: { error: 'unavailable' } });
  });
});
for (const width of [390, 1440]) test(`realização, mapa e confirmação por teclado em ${width}px`, async ({ page }, info) => {
  let current = { ...view, items: [...view.items, { ...view.items[0], id: '80000000-0000-0000-0000-000000000002', questionId: '10000000-0000-0000-0000-000000000002' }] };
  await page.route('**/api/v1/csrf', route => route.fulfill({ json: { token: 'test', headerName: 'X-CSRF-TOKEN' } }));
  await page.route('**/api/v1/attempts**', route => {
    if (route.request().method() === 'PUT') {
      const answer = route.request().postDataJSON(); const id = new URL(route.request().url()).pathname.split('/').at(-1);
      current = { ...current, version: current.version + 1, items: current.items.map(item => item.id === id ? { ...item, selectedAlternativeId: answer.alternativeId } : item) };
    }
    return route.fulfill({ status: route.request().method() === 'POST' ? 201 : 200, json: current });
  });
  await page.setViewportSize({ width, height: 900 });await page.goto('/');
  await page.getByRole('button', { name: 'Montar um simulado' }).click();
  await page.getByLabel('Quantidade de Português').fill('2');await page.getByRole('button', { name: 'Iniciar simulado' }).click();
  await expect(page.getByRole('heading', { name: 'Questão 1 de 2' })).toBeFocused();
  await page.getByRole('radio').first().focus();await page.keyboard.press('Space');
  await expect(page.getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Ir para questão 2: sem resposta' }).focus();await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Questão 2 de 2' })).toBeFocused();
  await page.getByRole('button', { name: 'Concluir simulado', exact: true }).click();
  await expect(page.getByRole('dialog')).toContainText('1 de 2 questões respondidas; 1 em branco.');
  await page.keyboard.press('Escape');await expect(page.getByRole('button', { name: 'Concluir simulado', exact: true })).toBeFocused();
  await expect(page.getByRole('button', { name: 'Ir para questão 1: respondida' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath(`practice-${width}.png`), fullPage: true });
});
for (const width of [390, 1440]) test(`composição mantém critérios e resumo acessíveis em ${width}px`, async ({ page }, info) => {
  await page.setViewportSize({ width, height: 900 }); await page.goto('/');
  await page.getByRole('button', { name: 'Montar um simulado' }).click();
  const start = page.getByRole('button', { name: 'Iniciar simulado' });
  await expect(start).toBeEnabled();
  await page.getByLabel('Quantidade de Português').fill('101'); await expect(start).toBeDisabled();
  await expect(page.getByRole('status').filter({ hasText: 'Informe quantidades inteiras' })).toBeVisible();
  await page.getByLabel('Quantidade de Português').fill('2'); await page.getByLabel('Quantidade de Matemática', { exact: true }).fill('3');
  await page.getByText('Assuntos de Português (opcional)', { exact: true }).click();
  await page.getByLabel('Interpretação', { exact: true }).check();
  await page.getByLabel('Usar variações numéricas quando disponíveis').check();
  await expect(page.getByRole('status').filter({ hasText: 'Total: 5 questões.' })).toBeVisible();
  await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Início', exact: true }).click();
  await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
  await expect(page.getByLabel('Interpretação', { exact: true })).toBeChecked();
  await expect(page.getByLabel('Quantidade de Matemática', { exact: true })).toHaveValue('3');
  await expect(page.getByLabel('Usar variações numéricas quando disponíveis')).toBeChecked();
  await start.scrollIntoViewIfNeeded(); await expect(start).toBeInViewport();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath(`composition-${width}.png`), fullPage: true });
});
for (const width of [390, 1440]) test(`aulas completas e prática dirigida sem criação automática em ${width}px`, async ({ page }, info) => {
  const root = resolve('..');const known = new Set(Object.values(paths));let created = 0;
  page.on('request', request => { if (request.method() === 'POST' && request.url().endsWith('/attempts')) created++; });
  await page.route('https://lordbigodone.github.io/concurso-simulator/**', route => {
    const name = decodeURIComponent(new URL(route.request().url()).pathname).replace('/concurso-simulator/', '');
    const file = resolve(root, name);expect(file.startsWith(root + sep)).toBe(true);
    if (!known.has(name) && !name.startsWith('assets/')) return route.abort('blockedbyclient');
    return route.fulfill({ contentType: name.endsWith('.svg') ? 'image/svg+xml' : 'text/html; charset=utf-8', body: readFileSync(file) });
  });
  await page.setViewportSize({ width, height: 900 });await page.goto('/');
  await page.getByRole('button', { name: 'Explorar as aulas' }).click();
  await expect(page.getByRole('status').filter({ hasText: '51 aulas encontradas.' })).toBeVisible();
  await page.getByLabel('Buscar aula').fill('interpretacao');await expect(page.getByRole('status').filter({ hasText: '1 aula encontrada.' })).toBeVisible();
  await page.getByLabel('Filtrar disciplina').selectOption('informatica');await expect(page.getByRole('heading', { name: 'Nenhuma aula por aqui' })).toBeVisible();
  await page.getByRole('button', { name: 'Limpar busca e filtros' }).click();
  await page.screenshot({ path: info.outputPath(`lessons-${width}.png`), fullPage: true });
  await page.getByLabel('Buscar aula').fill('M01');await page.getByRole('button', { name: /M01.*Operações com Números Reais/ }).click();
  await expect(page.getByRole('heading', { name: 'Operações com Números Reais', exact: true })).toBeFocused();
  const frame = page.frameLocator('iframe[title="Aula completa: Operações com Números Reais"]');
  await expect(frame.getByRole('heading', { name: 'Matemática 01 - Operações com Números Reais', exact: true })).toBeVisible();
  await expect(frame.locator('#s5')).toContainText('Estimativa');await expect(frame.locator('img').first()).toBeVisible();
  expect(await frame.locator('img').first().evaluate((img: HTMLImageElement) => img.naturalWidth)).toBeGreaterThan(0);
  expect(await frame.locator('html').evaluate(el => el.scrollWidth <= innerWidth)).toBe(true);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath(`reader-${width}.png`), fullPage: true });
  await page.getByRole('button', { name: 'Praticar este assunto' }).click();
  await expect(page.getByLabel('Quantidade de Matemática', { exact: true })).toHaveValue('1');
  await expect(page.getByLabel('Quantidade de Português')).toHaveValue('0');
  await expect(page.getByLabel('Operações', { exact: true })).toBeChecked();expect(created).toBe(0);
});
