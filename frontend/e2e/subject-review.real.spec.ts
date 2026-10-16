import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
const api = 'http://127.0.0.1:18080', provider = 'http://127.0.0.1:18081';
const panel = (page: Page) => page.getByRole('region', { name: 'Revisão por assunto', exact: true });
const individual = (page: Page) => page.getByRole('region', { name: 'Revisão por assunto deste simulado', exact: true });
async function login(page: Page, request: APIRequestContext, subject: string) {
  expect((await request.post(provider + '/fixture/control', { form: { subject, mode: 'valid' } })).ok()).toBe(true);
  await page.goto('/');await page.getByRole('link', { name: 'Entrar com Google' }).click();await expect(page.getByText('Você está conectado.')).toBeVisible();
}
async function finish(page: Page, quantity = 3) {
  await page.getByLabel('Quantidade de Português').fill(String(quantity));const created = page.waitForResponse(r => r.url() === api + '/api/v1/attempts' && r.request().method() === 'POST');
  await page.getByRole('button', { name: 'Iniciar simulado' }).click();const started = await created;expect(started.status()).toBe(201);const attempt = await started.json();
  await page.getByRole('button', { name: 'Concluir simulado', exact: true }).click();const closed = page.waitForResponse(r => r.url().endsWith('/finish') && r.request().method() === 'POST');
  await page.getByRole('button', { name: 'Confirmar conclusão' }).click();expect((await closed).status()).toBe(200);await expect(page.getByRole('heading', { name: 'Resultado do simulado' })).toBeVisible();return attempt.id as string;
}
async function storage(page: Page) { return page.evaluate(async () => ({ local: localStorage.length, session: sessionStorage.length, caches: await caches.keys(), workers: (await navigator.serviceWorker.getRegistrations()).length })); }

for (const width of [390, 1280]) test(`painel e revisão real por teclado com 51 assuntos em ${width}px`, async ({ page, request }, info) => {
  await page.setViewportSize({ width, height: 900 });await login(page, request, `subject-review-${width}`);
  const area = panel(page);await area.getByLabel('Cargo para revisão').selectOption('assistente-administrativo');
  await expect(area.getByText('Sem dados para este cargo. Nenhuma dificuldade foi inferida.')).toBeVisible();await expect(area.getByRole('row')).toHaveCount(52);
  const table = area.getByRole('region', { name: 'Tabela de desempenho por assunto' });await table.focus();await table.press('ArrowRight');await expect.poll(() => table.evaluate(e => e.scrollLeft)).toBeGreaterThan(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await table.locator('caption').scrollIntoViewIfNeeded();await page.screenshot({ path: info.outputPath(`review-table-${width}.png`) });
  const id = await finish(page);await expect(individual(page).getByRole('row')).toHaveCount(52);
  await expect(area.getByText('Janela: 1 de até 100 conclusões deste cargo.', { exact: false })).toBeVisible();
  const data = await (await page.request.get(api + '/api/v1/me/subject-review?roleId=assistente-administrativo')).json();
  const one = await (await page.request.get(`${api}/api/v1/attempts/${id}/subject-review`)).json();
  expect(data.subjects.reduce((n: number, m: { assessed: number }) => n + m.assessed, 0)).toBe(3);expect(one.subjects).toEqual(data.subjects);
  expect(JSON.stringify(data)).not.toMatch(/PRIVATE_|correctAlternativeId|parameters|resolution|owner|accountId|Digest/);
  await area.getByRole('button', { name: 'Atualizar revisão' }).focus();await page.keyboard.press('Enter');await expect(area.getByRole('heading', { name: 'Revisão por assunto', exact: true })).toBeFocused();
  await page.screenshot({ path: info.outputPath(`review-panel-${width}.png`) });
  await individual(page).getByRole('heading', { name: 'Revisão por assunto deste simulado', exact: true }).scrollIntoViewIfNeeded();await page.screenshot({ path: info.outputPath(`review-individual-${width}.png`) });
  await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toHaveAttribute('href', /concurso-simulator/);
  expect(await storage(page)).toEqual({ local: 0, session: 0, caches: [], workers: 0 });
});
test('visitante revisa somente tentativa concluída sem painel nem histórico implícito', async ({ page }) => {
  let overviewRequests = 0;page.on('request', r => { if (r.url().includes('/me/subject-review')) overviewRequests++; });await page.goto('/');
  await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();const id = await finish(page, 1);
  await expect(individual(page).getByRole('table')).toBeVisible();await expect(panel(page)).toHaveCount(0);expect(overviewRequests).toBe(0);
  expect((await page.request.get(`${api}/api/v1/attempts/${id}/subject-review`)).status()).toBe(200);
  expect((await page.request.get(api + '/api/v1/me/subject-review?roleId=assistente-administrativo')).status()).toBe(401);
  expect(await storage(page)).toEqual({ local: 0, session: 0, caches: [], workers: 0 });
});
test('falha não vira zero, retry preserva cargo e logout descarta resposta real tardia', async ({ page, request }) => {
  await login(page, request, 'subject-review-failure');let failed = false;
  await page.route('**/api/v1/me/subject-review?*', async route => { if (!failed) { failed = true;await route.fulfill({ status: 503, contentType: 'application/json', body: '{"error":"unavailable"}' }); } else await route.continue(); });
  const area = panel(page);await area.getByLabel('Cargo para revisão').selectOption('assistente-administrativo');await expect(area.getByRole('alert')).toContainText('Não significa ausência de dados');await expect(area.getByRole('table')).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();await area.getByRole('button', { name: 'Atualizar revisão' }).click();await expect(area.getByRole('table')).toBeVisible();
  await page.unroute('**/api/v1/me/subject-review?*');let captured = false;let release!: () => void;const held = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/api/v1/me/subject-review?*', async route => { const response = await route.fetch();captured = true;await held;await route.fulfill({ response }).catch(() => {}); });
  await area.getByRole('button', { name: 'Atualizar revisão' }).click();await expect.poll(() => captured).toBe(true);await page.getByRole('button', { name: 'Sair' }).click();
  await expect(panel(page)).toHaveCount(0);release();await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();await expect(panel(page)).toHaveCount(0);expect(await storage(page)).toEqual({ local: 0, session: 0, caches: [], workers: 0 });
});

test('falha explícita seguida de conclusão preserva foco no resultado durante atualização automática', async ({ page, request }) => {
  await login(page, request, 'subject-review-focus-regression');await page.getByLabel('Quantidade de Português').fill('1');
  await page.getByRole('button', { name: 'Iniciar simulado' }).click();await expect(page.getByRole('heading', { name: 'Questão 1 de 1' })).toBeVisible();
  let failed = false;await page.route('**/api/v1/me/subject-review?*', async route => {
    if (!failed) { failed = true;await route.fulfill({ status: 503, contentType: 'application/json', body: '{"error":"unavailable"}' }); } else await route.continue();
  });
  const area = panel(page);await area.getByLabel('Cargo para revisão').selectOption('assistente-administrativo');await expect(area.getByRole('alert')).toBeVisible();
  await page.getByRole('button', { name: 'Concluir simulado', exact: true }).click();await page.getByRole('button', { name: 'Confirmar conclusão' }).click();
  await expect(area.getByRole('table')).toBeVisible();await expect(area.getByText('Poucos dados', { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Resultado do simulado' })).toBeFocused();
});
