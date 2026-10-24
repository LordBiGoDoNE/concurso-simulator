import { test, expect } from '@playwright/test';
import { readFileSync, readdirSync } from 'node:fs';
import { resolve } from 'node:path';
const api = 'http://127.0.0.1:18080/api/v1/question-bank';
test.beforeAll(async ({ request }) => {
  await expect.poll(async () => (await (await request.get(`${api}/questions`)).json()).total, { timeout: 30000 }).toBe(22);
});

for (const width of [390, 1280]) {
  test(`questões reais: teclado/filtros/página/detalhe/fontes sem dados corretivos, ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    const reads: Promise<string>[] = [];
    // Inspecionar somente requisições concluídas: trocas rápidas de filtro abortam as anteriores.
    page.on('requestfinished', request => {
      if (request.url().startsWith(api)) {
        const read = request.response().then(response => response!.text());
        void read.catch(() => {}); // não deixar rejeição assíncrona solta; Promise.all abaixo exige sucesso.
        reads.push(read);
      }
    });
    await page.goto('/');await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Banco de questões', exact: true }).click();
    await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();
    await expect(page.getByText('22 questões elegíveis. Página 1.')).toBeVisible();
    await page.getByLabel('Disciplina', { exact: true }).focus();
    await page.keyboard.press('ArrowDown'); // Português, primeira disciplina
    await expect(page.getByLabel('Disciplina', { exact: true })).toHaveValue('portugues');
    await page.keyboard.press('Tab');await expect(page.getByLabel('Assunto', { exact: true })).toBeFocused();
    await page.keyboard.press('ArrowDown');await expect(page.getByLabel('Assunto', { exact: true })).toHaveValue('P01');
    await page.keyboard.press('Tab');await expect(page.getByLabel('Cargo', { exact: true })).toBeFocused();
    await page.keyboard.press('ArrowDown');await expect(page.getByLabel('Cargo', { exact: true })).toHaveValue('assistente-administrativo');
    await expect(page.getByText('21 questões elegíveis. Página 1.')).toBeVisible();
    const item = page.getByRole('button', { name: /Consultar:/ }).first();
    const name = await item.textContent();await item.focus();await page.keyboard.press('Enter');
    await expect(page.getByRole('heading', { name: 'Detalhe da questão' })).toBeFocused();
    await expect(page.getByRole('link', { name: 'Referência sintética' })).toHaveAttribute('href', 'https://example.test/study');
    await expect(page.locator('article')).toContainText('[destaque: πr²]');
    await expect(page.locator('article')).toContainText('(x)_(1) + (y)_(2); 2^(3); H_(2)O; _abc; x < y e y > z');
    await expect(page.locator('article em,article strong')).toHaveCount(0);
    await expect(page.locator('article')).toContainText('(1)/(2)');
    await expect(page.getByRole('radio')).toHaveCount(0);await expect(page.locator('article').getByRole('checkbox')).toHaveCount(0);
    await expect(page.getByRole('checkbox')).toHaveCount(0);
    await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
    await expect(page.getByRole('checkbox')).toHaveCount(1);
    await expect(page.getByRole('region', { name: 'Simulados', exact: true }).getByRole('checkbox', { name: 'Usar variações numéricas quando disponíveis' })).not.toBeChecked();
    await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Banco de questões', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Detalhe da questão' })).toBeVisible();
    await page.getByRole('heading', { name: 'Detalhe da questão' }).focus();
    await expect(page.locator('[data-correct], .correct, .answer, script[src*="gabarito"]')).toHaveCount(0);
    expect(await page.content()).not.toMatch(/correctAlternativeId|PRIVATE_BROWSER_RESOLUTION_SENTINEL|PRIVATE_DRAFT_SENTINEL/);
    await page.keyboard.press('Tab');await expect(page.getByRole('button', { name: 'Fechar detalhe' })).toBeFocused();
    await page.keyboard.press('Enter');await expect(page.getByRole('button', { name: name!.trim(), exact: true })).toBeFocused();
    await page.getByRole('button', { name: 'Próxima página' }).focus();await page.keyboard.press('Enter');
    await expect(page.getByText('21 questões elegíveis. Página 2.')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Resultados da consulta' })).toBeFocused();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();
    expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
    const bodies = await Promise.all(reads);
    expect(bodies.length).toBeGreaterThan(2);
    for (const body of bodies) expect(body).not.toMatch(/correctAlternativeId|"resolution"|"provenance"|"status"|"checklist"|PRIVATE_BROWSER_RESOLUTION_SENTINEL/);
  });
}

test('mesmos filtros antes/depois de login OIDC real e aulas disponíveis durante falha de consulta', async ({ page, request }) => {
  await page.goto('/');await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Banco de questões', exact: true }).click();await page.getByLabel('Disciplina', { exact: true }).selectOption('matematica-raciocinio');
  await expect(page.getByText('1 questão elegível. Página 1.')).toBeVisible();
  const before = await page.getByRole('button', { name: /Consultar:/ }).allTextContents();
  expect((await request.post('http://127.0.0.1:18081/fixture/control', { form: { subject: 'question-browser-user', mode: 'valid' } })).ok()).toBe(true);
  await page.getByRole('link', { name: 'Entrar com Google' }).click();await expect(page.getByText('Você está conectado.')).toBeVisible();await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Banco de questões', exact: true }).click();
  await page.getByLabel('Disciplina', { exact: true }).selectOption('matematica-raciocinio');
  await expect(page.getByText('1 questão elegível. Página 1.')).toBeVisible();
  expect(await page.getByRole('button', { name: /Consultar:/ }).allTextContents()).toEqual(before);
  await page.route(`${api}/**`, route => route.abort('failed'));
  await page.getByLabel('Disciplina', { exact: true }).selectOption('portugues');
  await expect(page.getByText('Consulta de questões indisponível. Não é um acervo vazio.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();
  await expect(page.getByText('Você está conectado.')).toBeVisible();await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Banco de questões', exact: true }).click();
  await page.unroute(`${api}/**`);await page.getByRole('button', { name: 'Tentar questões novamente' }).click();
  await expect(page.getByText('21 questões elegíveis. Página 1.')).toBeVisible();
  const assets = resolve('dist/assets');
  const bundle = readdirSync(assets).filter(name => name.endsWith('.js')).map(name => readFileSync(resolve(assets, name), 'utf8')).join('');
  expect(bundle).not.toMatch(/PRIVATE_BROWSER_RESOLUTION_SENTINEL|NEW_BROWSER_RESOLUTION_SENTINEL|Fixture de navegador|Questão sintética/);
  // A tela de resultado usa o campo corretivo em chunk tardio, sem payload no bundle.
  const html = readFileSync(resolve('dist/index.html'), 'utf8');
  const entry = html.match(/src="([^\"]+\.js)"/)?.[1];expect(entry).toBeTruthy();
  expect(readFileSync(resolve('dist', entry!.replace(/^\//, '')), 'utf8')).not.toContain('correctAlternativeId');
  expect(await page.evaluate(() => performance.getEntriesByType('resource').map(r => r.name).filter(name => /AttemptResult-|attempt-result-api-/.test(name)))).toEqual([]);
});
