import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { resolve, sep } from 'node:path';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
const api = 'http://127.0.0.1:18080', provider = 'http://127.0.0.1:18081';
const panel = (page: Page) => page.getByRole('region', { name: 'Revisão por assunto', exact: true });
const individual = (page: Page) => page.getByRole('region', { name: 'Revisão por assunto deste simulado', exact: true });
async function login(page: Page, request: APIRequestContext, subject: string) {
  expect((await request.post(provider + '/fixture/control', { form: { subject, mode: 'valid' } })).ok()).toBe(true);
  await page.goto('/');await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();await page.getByRole('link', { name: 'Entrar com Google' }).click();await expect(page.getByText('Você está conectado.')).toBeVisible();await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
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
  let overviewRequests = 0;page.on('request', r => { if (r.url().includes('/me/subject-review')) overviewRequests++; });await page.goto('/');await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
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

// Hosting is substituted with the actual public HTML/assets; API, browser and PostgreSQL remain real.
async function lessonHosting(page: Page) {
  const root = fileURLToPath(new URL('../../', import.meta.url)).replace(/[\\/]$/, '');
  await page.context().route('https://lordbigodone.github.io/concurso-simulator/**', async route => {
    const path = decodeURIComponent(new URL(route.request().url()).pathname.replace('/concurso-simulator/', ''));
    const file = resolve(root, path);
    if (!file.startsWith(root + sep) || !/^(0[1-5]_[^/]+|assets)\//.test(path) || !existsSync(file)) { await route.abort();return; }
    await route.fulfill({ path: file });
  });
}
async function closeCurrent(page: Page) {
  await page.getByRole('button', { name: 'Concluir simulado', exact: true }).click();
  const closed = page.waitForResponse(r => r.url().endsWith('/finish') && r.request().method() === 'POST');
  await page.getByRole('button', { name: 'Confirmar conclusão' }).click();const response = await closed;expect(response.status()).toBe(200);
  await expect(page.getByRole('heading', { name: 'Resultado do simulado' })).toBeVisible();return response.json();
}
async function startDirected(page: Page) {
  const pending = page.waitForResponse(r => r.url() === api + '/api/v1/attempts' && r.request().method() === 'POST');
  await page.getByRole('button', { name: 'Iniciar simulado' }).click();const response = await pending;expect(response.status()).toBe(201);
  await expect(page.getByRole('heading', { name: 'Questão 1 de 1' })).toBeVisible();return response.json();
}
for (const width of [390, 1280]) test(`conta estuda e pratica a última base, recupera criação e retoma resultado em outra sessão (${width}px)`, async ({ page, request, browser }, info) => {
  const subject = `directed-flow-${width}`;await page.setViewportSize({ width, height: 900 });await login(page, request, subject);await lessonHosting(page);
  const area = panel(page);await area.getByLabel('Cargo para revisão').selectOption('secretario-de-escola');await expect(area.getByRole('row')).toHaveCount(52);
  await expect(area.getByRole('link', { name: /Rever aula de/ })).toHaveCount(51);
  const opened = page.waitForEvent('popup');await area.getByRole('link', { name: 'Rever aula de M01 (nova aba)', exact: true }).click();const lesson = await opened;
  await expect(lesson.locator('h1')).toContainText('Operações');expect(lesson.url()).not.toContain('Gabarito');await lesson.close();
  let posts = 0;page.on('request', r => { if (r.url() === api + '/api/v1/attempts' && r.method() === 'POST') posts++; });
  await area.getByRole('button', { name: 'Praticar M01', exact: true }).focus();await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Compor simulado' })).toBeFocused();expect(posts).toBe(0);
  await expect(page.getByLabel('Cargo do simulado')).toHaveValue('secretario-de-escola');
  for (const input of await page.getByRole('spinbutton').all()) await expect(input).toHaveValue(await input.getAttribute('id') === 'quantity-matematica-raciocinio' ? '1' : '0');
  await expect(page.getByLabel('Usar variações numéricas quando disponíveis')).not.toBeChecked();
  expect((await page.request.post(api + '/api/v1/attempts', { data: { commandId: crypto.randomUUID(), roleId: 'secretario-de-escola', disciplines: [{ disciplineId: 'matematica-raciocinio', quantity: 1, topicIds: ['M01'] }] } })).status()).toBe(403);
  const first = await startDirected(page);expect(first.items[0].classification.topicId).toBe('M01');const closed = await closeCurrent(page);
  const before = await (await page.request.get(api + '/api/v1/me/subject-review?roleId=secretario-de-escola')).json();expect(before.subjects.find((m: {topicId: string}) => m.topicId === 'M01')).toMatchObject({ assessed: 1, blank: 1, exposures: 1 });
  page.once('dialog', dialog => dialog.accept());await individual(page).getByRole('button', { name: 'Praticar M01', exact: true }).click();await expect(page.getByLabel('Quantidade de Matemática/Raciocínio Lógico')).toHaveValue('1');
  const bodies: unknown[] = [];let committed: any;
  await page.route('**/api/v1/attempts', async route => {
    if (route.request().method() !== 'POST') { await route.continue();return; }
    bodies.push(route.request().postDataJSON());expect(route.request().headers()['x-csrf-token']).toBeTruthy();
    if (bodies.length === 1) { const response = await route.fetch();expect(response.status()).toBe(201);committed = await response.json();await route.abort('failed'); } else await route.continue();
  });
  await page.getByRole('button', { name: 'Iniciar simulado' }).click();await expect(page.getByRole('button', { name: 'Tentar criar novamente' })).toBeEnabled();
  await page.getByRole('button', { name: 'Tentar criar novamente' }).press('Enter');await expect(page.getByRole('heading', { name: 'Questão 1 de 1' })).toBeVisible();
  expect(bodies).toHaveLength(2);expect(bodies[1]).toEqual(bodies[0]);expect(committed.items[0].questionId).toBe(first.items[0].questionId);
  const correct = closed.result.items[0].correctAlternativeId;const index = committed.items[0].alternatives.findIndex((a: {id: string}) => a.id === correct);
  await page.getByRole('radio').nth(index).check();await expect(page.getByText('Respostas confirmadas.', { exact: true })).toBeVisible();const secondClosed = await closeCurrent(page);
  await expect(area.getByText(/Janela: 2 de até 100/)).toBeVisible();
  const after = await (await page.request.get(api + '/api/v1/me/subject-review?roleId=secretario-de-escola')).json();expect(after.subjects.find((m: {topicId: string}) => m.topicId === 'M01')).toMatchObject({ assessed: 1, correct: 1, blank: 0, exposures: 2, percentage: 100 });
  await area.getByRole('region', { name: 'Tabela de desempenho por assunto' }).locator('caption').scrollIntoViewIfNeeded();await page.screenshot({ path: info.outputPath(`directed-${width}.png`) });
  await area.getByRole('button', { name: 'Praticar M01', exact: true }).scrollIntoViewIfNeeded();await page.screenshot({ path: info.outputPath(`directed-actions-${width}.png`) });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  const another = await browser.newContext();try {
    const resumed = await another.newPage();await login(resumed, request, subject);await panel(resumed).getByLabel('Cargo para revisão').selectOption('secretario-de-escola');
    await expect(panel(resumed).getByText(/Janela: 2 de até 100/)).toBeVisible();expect(await (await resumed.request.get(`${api}/api/v1/attempts/${committed.id}/result`)).json()).toEqual(secondClosed.result);
    expect(await storage(resumed)).toEqual({ local: 0, session: 0, caches: [], workers: 0 });
  } finally { await another.close(); }
});
test('visitante estuda e compõe outro assunto pelo resultado, sem criar histórico nem tentativa no clique', async ({ page }) => {
  await page.goto('/');await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();await lessonHosting(page);await finish(page, 1);await expect(individual(page).getByRole('button', { name: 'Praticar P01', exact: true })).toBeVisible();
  const opened = page.waitForEvent('popup');await individual(page).getByRole('link', { name: 'Rever aula de P01 (nova aba)', exact: true }).click();const lesson = await opened;
  await expect(lesson.locator('h1')).toContainText('Interpretação');await lesson.close();let posts = 0;page.on('request', r => { if (r.url() === api + '/api/v1/attempts' && r.method() === 'POST') posts++; });
  page.once('dialog', dialog => dialog.accept());await individual(page).getByRole('button', { name: 'Praticar P01', exact: true }).click();await expect(page.getByRole('heading', { name: 'Compor simulado' })).toBeFocused();expect(posts).toBe(0);
  const next = await startDirected(page);expect(next.items[0].classification.topicId).toBe('P01');await closeCurrent(page);await expect(individual(page).getByRole('table')).toBeVisible();
  await expect(panel(page)).toHaveCount(0);await expect(page.getByRole('heading', { name: 'Histórico de simulados' })).toHaveCount(0);expect(await storage(page)).toEqual({ local: 0, session: 0, caches: [], workers: 0 });
});
test('atalho cancelado e aula preservam rascunho; duas abas exigem recuperação explícita de conflito', async ({ page, request, context }) => {
  await login(page, request, 'directed-draft-two-tabs');await lessonHosting(page);await panel(page).getByLabel('Cargo para revisão').selectOption('assistente-administrativo');
  await panel(page).getByRole('button', { name: 'Praticar P01', exact: true }).click();const attempt = await startDirected(page);
  let failed = true;await page.route('**/api/v1/attempts/*/answers/*', async route => { if (failed) { await route.abort('failed'); } else await route.continue(); });
  await page.getByRole('radio').first().check();await expect(page.getByRole('button', { name: 'Repetir salvamento' })).toBeVisible();
  page.once('dialog', dialog => dialog.dismiss());await panel(page).getByRole('button', { name: 'Praticar M01', exact: true }).click();await expect(page.getByRole('radio').first()).toBeChecked();
  const opened = page.waitForEvent('popup');await panel(page).getByRole('link', { name: 'Rever aula de P01 (nova aba)', exact: true }).click();const lesson = await opened;await expect(lesson.locator('h1')).toBeVisible();await lesson.close();await expect(page.getByRole('radio').first()).toBeChecked();
  const second = await context.newPage();try {
    await second.goto('/');await second.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();await second.getByRole('button', { name: /Abrir simulado/ }).first().click();await second.getByRole('radio').last().check();await expect(second.getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
    failed = false;await page.getByRole('button', { name: 'Repetir salvamento' }).click();await page.getByRole('button', { name: 'Recarregar tentativa' }).click();
    await expect(page.getByRole('radio').first()).toBeChecked();await page.getByRole('button', { name: 'Salvar escolhas preservadas' }).click();await expect(page.getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
    expect((await (await page.request.get(`${api}/api/v1/attempts/${attempt.id}`)).json()).version).toBe(2);
  } finally { await second.close(); }
});
// Last: withdraws the sole school-secretary M01 base in this disposable fixture.
test('retirada preserva resultado capturado e insuficiência com zero, sem fallback nem redução', async ({ page, request }) => {
  await login(page, request, 'directed-withdraw');await panel(page).getByLabel('Cargo para revisão').selectOption('secretario-de-escola');await panel(page).getByRole('button', { name: 'Praticar M01', exact: true }).click();
  const first = await startDirected(page);const closed = await closeCurrent(page);expect((await request.post(provider + '/fixture/withdraw', { form: { id: first.items[0].questionId } })).ok()).toBe(true);
  page.once('dialog', dialog => dialog.accept());await individual(page).getByRole('button', { name: 'Praticar M01', exact: true }).click();const bodies: any[] = [];
  page.on('request', r => { if (r.url() === api + '/api/v1/attempts' && r.method() === 'POST') bodies.push(r.postDataJSON()); });
  await page.getByRole('button', { name: 'Iniciar simulado' }).click();await expect(page.getByRole('alert')).toContainText('0 disponíveis');await page.getByRole('button', { name: 'Tentar criar novamente' }).click();
  await expect(page.getByRole('button', { name: 'Tentar criar novamente' })).toBeEnabled();expect(bodies).toHaveLength(2);expect(bodies[0]).toEqual(bodies[1]);expect(bodies[0]).toMatchObject({ useParameterizedQuestions: false, disciplines: [{ disciplineId: 'matematica-raciocinio', quantity: 1, topicIds: ['M01'] }] });
  await expect(page.getByLabel('Quantidade de Matemática/Raciocínio Lógico')).toHaveValue('1');
  expect((await (await page.request.get(api + '/api/v1/me/attempts')).json()).items).toHaveLength(1);expect(await (await page.request.get(`${api}/api/v1/attempts/${first.id}/result`)).json()).toEqual(closed.result);
});
