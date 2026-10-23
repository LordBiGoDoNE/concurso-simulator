import { test, expect, type BrowserContext, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
const data = JSON.parse(readFileSync(process.env.LOCAL_APP_FIXTURE!, 'utf8'));
const origin = process.env.LOCAL_APP_ORIGIN!;
const area = (page: Page) => page.getByRole('region', { name: 'Simulados', exact: true });
async function session(context: BrowserContext, cookie: string) {
  await context.addCookies([{ name: 'SESSION', value: cookie.split('=')[1], url: origin, httpOnly: true, sameSite: 'Lax' }]);
}
function activeContract(view: unknown) {
  const text = JSON.stringify(view);
  expect(text).not.toMatch(/correctAlternativeId|resolution|provenance|PRIVATE_|definitionDigest/);
}
async function start(page: Page) {
  await page.getByLabel('Quantidade de Português').fill('1');
  const response = page.waitForResponse(r => r.url() === origin + '/api/v1/attempts' && r.request().method() === 'POST');
  await area(page).getByRole('button', { name: 'Iniciar simulado' }).click();
  const created = await response;expect(created.status()).toBe(201);const view = await created.json();activeContract(view);
  await expect(area(page).getByRole('heading', { name: 'Questão 1 de 1' })).toBeVisible();return view;
}
async function finish(page: Page) {
  await area(page).getByRole('button', { name: 'Concluir simulado', exact: true }).click();
  await page.getByRole('button', { name: 'Confirmar conclusão' }).click();
  await expect(area(page).getByRole('heading', { name: 'Resultado do simulado' })).toBeVisible();
}
test('visitante: CSRF, retry idempotente, conclusão e revisão no pacote real', async ({ page }) => {
  await page.goto('/');await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();
  expect((await page.request.post('/api/v1/attempts', { data: {} })).status()).toBe(403);
  const view = await start(page);const bodies: unknown[] = [];
  await page.route('**/api/v1/attempts/*/answers/*', async route => {
    bodies.push(route.request().postDataJSON());
    if (bodies.length === 1) { expect((await route.fetch()).status()).toBe(200);await route.abort('failed'); }
    else await route.continue();
  });
  await area(page).getByRole('radio').first().check();
  await area(page).getByRole('button', { name: 'Repetir salvamento' }).click();
  await expect(area(page).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();expect(bodies).toHaveLength(2);expect(bodies[0]).toEqual(bodies[1]);
  expect((await (await page.request.get('/api/v1/attempts/' + view.id)).json()).version).toBe(1);
  await finish(page);await expect(page.getByRole('region', { name: 'Revisão por assunto deste simulado', exact: true }).getByRole('row')).toHaveCount(52);
  expect((await page.request.get('/api/v1/me/attempts')).status()).toBe(401);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});
test('conta: duas sessões retomam, conflito é explícito e outra conta recebe404', async ({ browser, page, context }) => {
  await session(context, data.cookie);await page.goto('/');await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();await expect(page.getByText('Você está conectado.')).toBeVisible();await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
  const view = await start(page);const second = await browser.newContext();const other = await browser.newContext();
  try {
    await session(second, data.sameCookie);await session(other, data.otherCookie);
    expect((await other.request.get(origin + '/api/v1/attempts/' + view.id)).status()).toBe(404);
    const resumed = await second.newPage();await resumed.goto('/');await resumed.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
    await area(resumed).getByRole('button', { name: /Abrir simulado/ }).first().click();
    await expect(area(resumed).getByRole('heading', { name: 'Questão 1 de 1' })).toBeVisible();
    await area(page).getByRole('radio').first().check();await expect(area(page).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
    await area(resumed).getByRole('radio').last().check();await area(resumed).getByRole('button', { name: 'Recarregar tentativa' }).click();
    await area(resumed).getByRole('button', { name: 'Salvar escolhas preservadas' }).click();
    await expect(area(resumed).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
    expect((await (await second.request.get(origin + '/api/v1/attempts/' + view.id)).json()).version).toBe(2);
    await finish(resumed);
  } finally { await second.close();await other.close(); }
});
for (const width of [390, 1280]) test(`51 assuntos, aula real e prática dirigida em ${width}px`, async ({ page, context }, info) => {
  await session(context, data.cookie);await page.setViewportSize({ width, height: 900 });await page.goto('/');await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
  const panel = page.getByRole('region', { name: 'Revisão por assunto', exact: true });
  await panel.getByLabel('Cargo para revisão').selectOption('assistente-administrativo');
  await expect(panel.getByRole('row')).toHaveCount(52);await expect(panel.getByRole('link', { name: /Rever aula de/ })).toHaveCount(51);
  const opened = page.waitForEvent('popup');await panel.getByRole('link', { name: 'Rever aula de P01 (nova aba)', exact: true }).click();
  const lesson = await opened;await expect(lesson.locator('h1')).toBeVisible();expect(lesson.url()).toContain(origin + '/material/');await lesson.close();
  await panel.getByRole('button', { name: 'Praticar P01', exact: true }).click();
  await expect(page.getByLabel('Quantidade de Português')).toHaveValue('1');
  const view = await start(page);expect(view.items[0].classification.topicId).toBe('P01');await finish(page);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath(`local-${width}.png`), fullPage: true });
});
