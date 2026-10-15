import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import type { AttemptView } from '../src/attempt-api';
const api = 'http://127.0.0.1:18080'; const provider = 'http://127.0.0.1:18081';
const workspace = (page: Page) => page.getByRole('region', { name: 'Simulados', exact: true });
const resolution = 'PRIVATE_BROWSER_RESOLUTION_SENTINEL';
function activeContract(value: unknown) {
  const text = JSON.stringify(value);expect(text).not.toContain(resolution);
  const visit = (v: unknown) => { if (Array.isArray(v)) v.forEach(visit);else if (v && typeof v === 'object') for (const [key, child] of Object.entries(v)) { expect(['correctAlternativeId', 'resolution', 'provenance', 'percentage']).not.toContain(key);visit(child); } };visit(value);
}
async function control(request: APIRequestContext, subject: string) { expect((await request.post(provider + '/fixture/control', { form: { subject, mode: 'valid' } })).ok()).toBe(true); }
async function login(page: Page) { await page.goto('/');await page.getByRole('link', { name: 'Entrar com Google' }).click();await expect(page.getByText('Você está conectado.')).toBeVisible(); }
async function start(page: Page, quantity = 2): Promise<AttemptView> {
  const area = workspace(page);await area.getByLabel('Quantidade de Português').fill(String(quantity));
  const created = page.waitForResponse(r => r.url() === api + '/api/v1/attempts' && r.request().method() === 'POST');
  await area.getByRole('button', { name: 'Iniciar simulado' }).click();const response = await created;expect(response.status()).toBe(201);
  const view = await response.json();activeContract(view);await expect(area.getByRole('heading', { name: `Questão 1 de ${quantity}` })).toBeVisible();return view;
}
async function finish(page: Page) {
  const area = workspace(page);await area.getByRole('button', { name: 'Concluir simulado', exact: true }).click();
  const closed = page.waitForResponse(r => r.url().endsWith('/finish') && r.request().method() === 'POST');
  await page.getByRole('button', { name: 'Confirmar conclusão' }).click();const response = await closed;expect(response.status()).toBe(200);
  await expect(area.getByRole('heading', { name: 'Resultado do simulado' })).toBeVisible();return response.json();
}
async function storage(page: Page) {
  return page.evaluate(async () => ({ local: localStorage.length, session: sessionStorage.length,
    caches: 'caches' in window ? await caches.keys() : [], workers: 'serviceWorker' in navigator ? (await navigator.serviceWorker.getRegistrations()).length : 0 }));
}
test('resultado independe de novo download do renderer depois da conclusao', async ({ page }) => {
  const rendererRequests: string[] = [];
  await page.goto('/');const view = await start(page, 1);let finishes = 0;
  await page.route('**/*AttemptResult*', async route => { rendererRequests.push(route.request().url());await route.abort('failed'); });
  page.on('request', request => { if (request.url().endsWith('/finish') && request.method() === 'POST') finishes++; });
  await finish(page);await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();
  await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();expect(rendererRequests).toEqual([]);
  expect(finishes).toBe(1);expect((await (await page.request.get(`${api}/api/v1/attempts/${view.id}`)).json()).finishedAt).not.toBeNull();
});
for (const width of [390, 1280]) test(`visitante realiza por teclado, confirma e revisa sem vazamento antecipado em ${width}px`, async ({ page }, info) => {
  await page.setViewportSize({ width, height: 850 });await page.goto('/');const area = workspace(page);const view = await start(page);
  await expect(area.getByRole('heading', { name: 'Questão 1 de 2' })).toBeFocused();
  const radio = area.getByRole('radio').first();await radio.focus();await radio.press('Space');await expect(area.getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
  const saved = await page.request.get(`${api}/api/v1/attempts/${view.id}`);activeContract(await saved.json());
  await area.getByRole('button', { name: 'Próxima questão' }).press('Enter');await expect(area.getByRole('heading', { name: 'Questão 2 de 2' })).toBeFocused();
  await area.getByRole('button', { name: 'Questão anterior' }).press('Enter');await expect(area.getByRole('radio').first()).toBeChecked();
  expect(await area.textContent()).not.toContain(resolution);expect(await area.locator('[data-correct], [data-answer]').count()).toBe(0);
  await page.screenshot({ path: info.outputPath(`practice-${width}.png`), fullPage: true });
  const close = area.getByRole('button', { name: 'Concluir simulado', exact: true });await close.focus();await close.press('Enter');
  await expect(page.getByRole('button', { name: 'Continuar respondendo' })).toBeFocused();await page.keyboard.press('Escape');await expect(close).toBeFocused();
  const result = await finish(page);expect(result.result.items).toHaveLength(2);await expect(area.getByRole('heading', { name: 'Resultado do simulado' })).toBeFocused();
  await expect(area.getByText(resolution + ' — não transmitir', { exact: true }).first()).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();
  expect(await storage(page)).toEqual({ local: 0, session: 0, caches: [], workers: 0 });
  await page.screenshot({ path: info.outputPath(`result-${width}.png`), fullPage: true });
});
test('resposta confirmada no servidor com confirmação perdida é recuperada com mesmo comando', async ({ page }) => {
  await page.goto('/');const view = await start(page, 1);const bodies: unknown[] = [];
  await page.route('**/api/v1/attempts/*/answers/*', async route => {
    bodies.push(route.request().postDataJSON());
    if (bodies.length === 1) { expect((await route.fetch()).status()).toBe(200);await route.abort('failed'); } else await route.continue();
  });
  const area = workspace(page);await area.getByRole('radio').first().check();await expect(area.getByRole('button', { name: 'Repetir salvamento' })).toBeVisible();
  await expect(area.getByRole('radio').first()).toBeChecked();await expect(area.getByRole('button', { name: 'Concluir simulado', exact: true })).toBeDisabled();
  await area.getByRole('button', { name: 'Repetir salvamento' }).click();await expect(area.getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
  expect(bodies).toHaveLength(2);expect(bodies[0]).toEqual(bodies[1]);expect((await (await page.request.get(`${api}/api/v1/attempts/${view.id}`)).json()).version).toBe(1);
});
test('mesma conta retoma após reinício, outra conta não acessa e mudança editorial não altera correção capturada', async ({ browser, request }) => {
  test.setTimeout(150000);const first = await browser.newContext();const same = await browser.newContext();const other = await browser.newContext();
  try {
    await control(request, 'attempt-resume-user');const one = await first.newPage();await login(one);const view = await start(one);
    await workspace(one).getByRole('radio').first().check();await expect(workspace(one).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
    const confirmed = await (await first.request.get(`${api}/api/v1/attempts/${view.id}`)).json();
    expect((await request.post(provider + '/fixture/restart')).ok()).toBe(true);
    await control(request, 'attempt-other-user');const stranger = await other.newPage();await login(stranger);
    await expect(workspace(stranger).getByText('Nenhuma tentativa nesta página.')).toBeVisible();expect((await other.request.get(`${api}/api/v1/attempts/${view.id}`)).status()).toBe(404);
    await control(request, 'attempt-resume-user');const resumed = await same.newPage();await login(resumed);
    await workspace(resumed).getByRole('button', { name: /Abrir simulado/ }).first().click();await expect(workspace(resumed).getByRole('radio').first()).toBeChecked();
    expect(await (await same.request.get(`${api}/api/v1/attempts/${view.id}`)).json()).toEqual(confirmed);
    expect((await request.post(provider + '/fixture/editorial', { form: { id: view.items[0].questionId } })).ok()).toBe(true);
    const current = await (await same.request.get(`${api}/api/v1/question-bank/questions/${view.items[0].questionId}`)).json();expect(current.revisionId).not.toBe(view.items[0].revisionId);
    const historic = await (await same.request.get(`${api}/api/v1/attempts/${view.id}`)).json();expect(historic).toEqual(confirmed);
    const result = await finish(resumed);expect(result.result.items[0].item.revisionId).toBe(view.items[0].revisionId);
    expect(result.result.items[0].correctAlternativeId).toBe('10000000-0000-0000-0000-000000000001');
    expect(JSON.stringify(result)).toContain(resolution);expect(JSON.stringify(result)).not.toContain('NEW_BROWSER_RESOLUTION_SENTINEL');
    expect(await storage(resumed)).toEqual({ local: 0, session: 0, caches: [], workers: 0 });
  } finally { await first.close();await same.close();await other.close(); }
});
test('duas abas preservam escolha conflitante e só reenviam após recarga explícita', async ({ page, context, request }) => {
  await control(request, 'attempt-two-tabs');await login(page);const view = await start(page, 1);const second = await context.newPage();await second.goto('/');
  await workspace(second).getByRole('button', { name: /Abrir simulado/ }).first().click();await expect(workspace(second).getByRole('heading', { name: 'Questão 1 de 1' })).toBeVisible();
  await workspace(page).getByRole('radio').first().check();await expect(workspace(page).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
  await workspace(second).getByRole('radio').last().check();await expect(workspace(second).getByRole('button', { name: 'Recarregar tentativa' })).toBeVisible();
  await workspace(second).getByRole('button', { name: 'Recarregar tentativa' }).click();await expect(workspace(second).getByRole('button', { name: 'Salvar escolhas preservadas' })).toBeVisible();
  await expect(workspace(second).getByRole('radio').last()).toBeChecked();await workspace(second).getByRole('button', { name: 'Salvar escolhas preservadas' }).click();
  await expect(workspace(second).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();expect((await (await second.request.get(`${api}/api/v1/attempts/${view.id}`)).json()).version).toBe(2);await second.close();
});
test('login durante visitante exige aviso e não transfere a tentativa nem a recupera após logout', async ({ page, request }) => {
  await control(request, 'attempt-no-transfer');await page.goto('/');const view = await start(page, 1);
  page.once('dialog', async dialog => { expect(dialog.message()).toContain('não será transferida');await dialog.accept(); });
  await page.getByRole('link', { name: 'Entrar com Google' }).click();await expect(page.getByText('Você está conectado.')).toBeVisible();
  await expect(workspace(page).getByText('Nenhuma tentativa nesta página.')).toBeVisible();expect((await page.request.get(`${api}/api/v1/attempts/${view.id}`)).status()).toBe(404);
  await page.getByRole('button', { name: 'Sair', exact: true }).click();await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();
  expect((await page.request.get(`${api}/api/v1/attempts/${view.id}`)).status()).toBe(404);expect(await storage(page)).toEqual({ local: 0, session: 0, caches: [], workers: 0 });
});
test('expiração real da sessão visitante limpa a realização na revalidação e controle negativo detecta gabarito', async ({ page }) => {
  await page.goto('/');const view = await start(page, 1);expect(() => activeContract({ ...view, correctAlternativeId: 'PRIVATE_NEGATIVE_CONTROL' })).toThrow();
  await page.waitForTimeout(21500);await page.getByRole('button', { name: 'Atualizar sessão' }).click();
  await expect(workspace(page).getByRole('button', { name: 'Iniciar simulado' })).toBeVisible();expect((await page.request.get(`${api}/api/v1/attempts/${view.id}`)).status()).toBe(404);
});
