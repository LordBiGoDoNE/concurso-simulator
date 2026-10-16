import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import type { AttemptView } from '../src/attempt-api';
const api = 'http://127.0.0.1:18080'; const provider = 'http://127.0.0.1:18081';
const area = (page: Page) => page.getByRole('region', { name: 'Simulados', exact: true });
const option = 'Usar variações numéricas quando disponíveis';
const privateKeys = ['source', 'capture', 'templateId', 'templateVersion', 'parameters', 'definitionDigest', 'domainDigest', 'manifestDigest', 'outputDigest', 'generatorVersion', 'seed', 'provenance'];
function safe(value: unknown, active = true) {
  if (Array.isArray(value)) value.forEach(v => safe(v, active));
  else if (value && typeof value === 'object') for (const [key, child] of Object.entries(value)) {
    expect([...privateKeys, ...(active ? ['correctAlternativeId', 'resolution', 'percentage'] : [])]).not.toContain(key);safe(child, active);
  }
}
async function settings(page: Page) {
  await area(page).getByLabel('Quantidade de Português').fill('0');
  await area(page).getByLabel('Quantidade de Matemática/Raciocínio Lógico').fill('4');
  const checkbox = area(page).getByRole('checkbox', { name: option });await expect(checkbox).not.toBeChecked();
  await expect(checkbox).toHaveAccessibleDescription(/não há garantia de questão nova/);await checkbox.focus();await checkbox.press('Space');await expect(checkbox).toBeChecked();
}
async function start(page: Page): Promise<AttemptView> {
  await settings(page);const pending = page.waitForResponse(r => r.url() === api + '/api/v1/attempts' && r.request().method() === 'POST');
  await area(page).getByRole('button', { name: 'Iniciar simulado' }).click();const response = await pending;expect(response.status()).toBe(201);
  const view = await response.json();safe(view);await expect(area(page).getByRole('heading', { name: 'Questão 1 de 4' })).toBeFocused();return view;
}
async function finish(page: Page) {
  await area(page).getByRole('button', { name: 'Concluir simulado', exact: true }).click();
  const pending = page.waitForResponse(r => r.url().endsWith('/finish') && r.request().method() === 'POST');
  await page.getByRole('button', { name: 'Confirmar conclusão' }).click();const response = await pending;expect(response.status()).toBe(200);
  const value = await response.json();safe(value, false);await expect(area(page).getByRole('heading', { name: 'Resultado do simulado' })).toBeFocused();return value;
}
async function control(request: APIRequestContext, subject: string) { expect((await request.post(provider + '/fixture/control', { form: { subject, mode: 'valid' } })).ok()).toBe(true); }
async function login(page: Page) { await page.goto('/');await page.getByRole('link', { name: 'Entrar com Google' }).click();await expect(page.getByText('Você está conectado.')).toBeVisible(); }
async function emptyStorage(page: Page) {
  expect(await page.evaluate(async () => [localStorage.length, sessionStorage.length, 'caches' in window ? (await caches.keys()).length : 0])).toEqual([0, 0, 0]);
}
test.beforeAll(async ({ request }) => {
  await expect.poll(async () => (await (await request.get(api + '/api/v1/question-bank/questions?disciplineId=matematica-raciocinio&roleId=assistente-administrativo')).json()).total).toBe(4);
});
for (const width of [390, 1280]) test(`visitante mistura três geradas e uma fixa, recupera criação e revisa com teclado em ${width}px`, async ({ page }, info) => {
  await page.setViewportSize({ width, height: 850 });await page.goto('/');await settings(page);
  const bodies: unknown[] = [];let committed!: AttemptView;
  await page.route('**/api/v1/attempts', async route => {
    if (route.request().method() !== 'POST') { await route.continue();return; }
    bodies.push(route.request().postDataJSON());
    if (bodies.length === 1) { const response = await route.fetch();expect(response.status()).toBe(201);committed = await response.json();await route.abort('failed'); }
    else await route.continue();
  });
  await area(page).getByRole('button', { name: 'Iniciar simulado' }).click();
  const retry = area(page).getByRole('button', { name: 'Tentar criar novamente' });await expect(retry).toBeEnabled();
  await expect(area(page).getByRole('checkbox', { name: option })).toBeChecked();await retry.press('Enter');
  await expect(area(page).getByRole('heading', { name: 'Questão 1 de 4' })).toBeFocused();
  expect(bodies).toHaveLength(2);expect(bodies[1]).toEqual(bodies[0]);expect(bodies[0]).toMatchObject({ useParameterizedQuestions: true });safe(committed);
  expect(new Set(committed.items.map(i => i.questionId)).size).toBe(4);
  expect(committed.items.filter(i => i.statement.text.includes('R$'))).toHaveLength(3);
  expect(() => safe({ ...committed, manifestDigest: 'PRIVATE_NEGATIVE_CONTROL' })).toThrow();
  const radio = area(page).getByRole('radio').first();await radio.focus();await radio.press('Space');await expect(area(page).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
  const state = await (await page.request.get(`${api}/api/v1/attempts/${committed.id}`)).json();safe(state);
  for (const key of [...privateKeys, 'correctAlternativeId']) expect(await area(page).innerHTML()).not.toContain(key);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);await emptyStorage(page);
  await page.screenshot({ path: info.outputPath(`numeric-practice-${width}.png`), fullPage: true });
  const result = await finish(page);expect(result.result.items).toHaveLength(4);
  expect(result.result.items.filter((c: { resolution: { text: string } }) => c.resolution.text.includes('R$'))).toHaveLength(3);
  await expect(area(page).getByRole('heading', { name: 'Resolução', exact: true })).toHaveCount(4);await emptyStorage(page);
  await page.screenshot({ path: info.outputPath(`numeric-result-${width}.png`), fullPage: true });
});
test('conta retoma conteúdo gerado em outra sessão após restart e alteração da base', async ({ browser, request }) => {
  test.setTimeout(150000);const first = await browser.newContext();const same = await browser.newContext();const other = await browser.newContext();
  try {
    await control(request, 'numeric-resume');const one = await first.newPage();await login(one);const view = await start(one);
    await area(one).getByRole('radio').first().check();await expect(area(one).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
    const saved = await (await first.request.get(`${api}/api/v1/attempts/${view.id}`)).json();
    expect((await request.post(provider + '/fixture/restart')).ok()).toBe(true);
    await control(request, 'numeric-other');const stranger = await other.newPage();await login(stranger);expect((await other.request.get(`${api}/api/v1/attempts/${view.id}`)).status()).toBe(404);
    await control(request, 'numeric-resume');const resumed = await same.newPage();await login(resumed);await area(resumed).getByRole('button', { name: /Abrir simulado/ }).first().click();
    await expect(area(resumed).getByRole('radio').first()).toBeChecked();expect(await (await same.request.get(`${api}/api/v1/attempts/${view.id}`)).json()).toEqual(saved);
    const generated = view.items.find(i => i.statement.text.includes('R$'))!;
    expect((await request.post(provider + '/fixture/editorial', { form: { id: generated.questionId } })).ok()).toBe(true);
    expect(await (await same.request.get(`${api}/api/v1/attempts/${view.id}`)).json()).toEqual(saved);
    const result = await finish(resumed);expect(result.result.items.find((c: { item: { id: string } }) => c.item.id === generated.id).item.statement).toEqual(generated.statement);
    expect(JSON.stringify(result)).not.toContain('NEW_BROWSER_RESOLUTION_SENTINEL');await emptyStorage(resumed);
  } finally { await first.close();await same.close();await other.close(); }
});
test('duas abas recuperam conflito sem perder escolha numa tentativa com variações', async ({ page, context, request }) => {
  await control(request, 'numeric-two-tabs');await login(page);const view = await start(page);const second = await context.newPage();
  try {
    await second.goto('/');await area(second).getByRole('button', { name: /Abrir simulado/ }).first().click();await expect(area(second).getByRole('heading', { name: 'Questão 1 de 4' })).toBeVisible();
    await area(page).getByRole('radio').first().check();await expect(area(page).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
    await area(second).getByRole('radio').last().check();await area(second).getByRole('button', { name: 'Recarregar tentativa' }).click();
    await expect(area(second).getByRole('radio').last()).toBeChecked();await area(second).getByRole('button', { name: 'Salvar escolhas preservadas' }).click();
    await expect(area(second).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();const saved = await (await second.request.get(`${api}/api/v1/attempts/${view.id}`)).json();safe(saved);expect(saved.version).toBe(2);
  } finally { await second.close(); }
});
test('expiração visitante limpa também o conteúdo numérico capturado', async ({ page }) => {
  await page.goto('/');const view = await start(page);await page.waitForTimeout(21500);await page.getByRole('button', { name: 'Atualizar sessão' }).click();
  await expect(area(page).getByRole('button', { name: 'Iniciar simulado' })).toBeVisible();
  expect((await page.request.get(`${api}/api/v1/attempts/${view.id}`)).status()).toBe(404);await emptyStorage(page);
});
