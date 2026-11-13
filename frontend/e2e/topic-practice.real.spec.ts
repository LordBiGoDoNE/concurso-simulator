import { test, expect, type Page, type BrowserContext } from '@playwright/test';
const expansion = process.env.PRACTICE_EXPANSION === '1';
const api = `http://127.0.0.1:${expansion ? 19370 : 19160}`, control = `http://127.0.0.1:${expansion ? 19371 : 19161}`;
async function account(context: BrowserContext, cookie: string) { await context.addCookies([{ name: 'SESSION', value: cookie, url: api, httpOnly: true, sameSite: 'Lax' }]); }
async function panel(page: Page) { const details = page.locator('details.lesson-practice-panel'); if (!await details.evaluate(e => (e as HTMLDetailsElement).open)) await details.locator('summary').click(); }
async function route(page: Page, hash: string) { await page.evaluate(hash => { window.location.hash = hash; }, hash); }
async function begin(page: Page) { await panel(page); await page.getByRole('button', { name: 'Começar prática', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Questão 1 de 5' })).toBeVisible(); }
async function finish(page: Page) { await page.getByRole('button', { name: 'Encerrar prática', exact: true }).click(); await page.getByRole('button', { name: 'Confirmar encerramento' }).click(); await expect(page.getByRole('heading', { name: 'Resultado da prática' })).toBeVisible(); }
for (const width of [390, 1440]) test(`visitante: assunto, cargo público e fila preservada em ${width}px`, async ({ page }, info) => {
  await page.setViewportSize({ width, height: 900 });
  const bodies: Record<string, unknown>[] = [];
  page.on('request', r => { if (r.method() === 'POST' && r.url() === api + '/api/v1/lesson-practices') bodies.push(r.postDataJSON()); });
  await page.goto('/#/aulas/P01'); await expect(page.getByText('Você está estudando como visitante.')).toBeVisible(); await expect(page.getByRole('heading', { name: 'Interpretação de Textos', exact: true })).toBeVisible();
  await panel(page); await expect(page.getByRole('button', { name: 'Começar prática', exact: true })).toBeEnabled();
  expect(bodies).toHaveLength(0); await expect(page.getByLabel('Cargo para praticar esta aula')).toHaveCount(0);
  await begin(page); expect(Object.keys(bodies[0]).sort()).toEqual(['commandId', 'topicId']);
  const map = page.getByRole('navigation', { name: 'Mapa da prática' }).getByRole('button');
  await expect(map).toHaveCount(5);
  for (let n = 0; n < 4; n++) {
    await map.nth(n).click(); await expect(page.getByRole('heading', { name: `Questão ${n + 1} de 5`, exact: true })).toBeVisible();
  }
  await map.nth(4).focus(); await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Questão 5 de 5', exact: true })).toBeFocused();
  await expect(page.getByRole('button', { name: 'Próxima questão', exact: true })).toBeDisabled();
  await page.screenshot({ path: info.outputPath(`five-last-${width}.png`) });
  let release!: () => void; const wait = new Promise<void>(r => release = r); let committed = false;
  await page.route('**/lesson-practices/*/answers/*', async r => { const response = await r.fetch(); expect(response.status()).toBe(200); committed = true; await wait; await r.fulfill({ response }); });
  try {
    await page.getByRole('radio').first().check(); await expect.poll(() => committed).toBe(true);
    await page.getByRole('button', { name: 'Recolher prática e voltar à leitura' }).click();
    await route(page, '#/simulados'); await expect(page.getByRole('button', { name: 'Iniciar simulado', exact: true })).toBeVisible();
    await route(page, '#/aulas/P01?cargo=secretario-de-escola'); await panel(page); release();
    await expect(page.locator('.save-status:visible')).toHaveText('Respostas confirmadas.'); await expect(page.getByRole('radio').first()).toBeChecked(); expect(bodies).toHaveLength(1);
    await finish(page); await expect(page.getByRole('heading', { name: 'Resolução', exact: true })).toHaveCount(5);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.locator('details.lesson-practice-panel').screenshot({ path: info.outputPath(`topic-${width}.png`) });
  } finally { release(); }
});
for (const width of [390, 1440]) test(`conta: replay perdido, legado e retorno à aula em ${width}px`, async ({ page, context, request }) => {
  const fixture = await (await request.get(control + '/fixture')).json(); await account(context, fixture.cookie);
  await page.setViewportSize({ width, height: 900 }); await page.goto('/#/aulas/P01');
  const bodies: unknown[] = []; let createdId = '';
  await page.route('**/api/v1/lesson-practices', async r => {
    bodies.push(r.request().postDataJSON()); const response = await r.fetch(); expect(response.status()).toBe(bodies.length === 1 ? 201 : 200);
    if (bodies.length === 1) { createdId = (await response.json()).id; await r.abort('failed'); } else await r.fulfill({ response });
  });
  await panel(page); await page.getByRole('button', { name: 'Começar prática', exact: true }).click();
  await page.getByRole('button', { name: 'Tentar iniciar prática novamente' }).click(); await expect(page.getByRole('heading', { name: 'Questão 1 de 5' })).toBeVisible();
  expect(bodies[0]).toEqual(bodies[1]); const captured = await (await page.request.get(api + '/api/v1/lesson-practices/' + createdId)).json();
  expect(captured.topicId).toBe('P01'); expect(captured).not.toHaveProperty('roleId'); expect(captured).not.toHaveProperty('composition');
  expect(JSON.stringify(captured)).not.toMatch(/correctAlternativeId|resolution/); expect(new Set(captured.items.map((i: { questionId: string }) => i.questionId)).size).toBe(5);
  for (let n = 0; n < 5; n++) {
    await page.getByRole('navigation', { name: 'Mapa da prática' }).getByRole('button').nth(n).click();
    await page.getByRole('radio').first().check();
    await expect(page.locator('.save-status:visible')).toHaveText('Respostas confirmadas.');
  }
  await finish(page); await route(page, '#/praticas?cargo=secretario-de-escola');
  const result = await (await page.request.get(api + '/api/v1/lesson-practices/' + createdId + '/result')).json();
  expect(result.total).toBe(5); expect(result.blank).toBe(0);
  const own = await (await page.request.get(api + '/api/v1/me/lesson-practices')).json(); expect(own.items.every((i: object) => !('roleId' in i))).toBe(true);
  expect(own.items.find((i: { id: string }) => i.id === createdId).total).toBe(5);
  const legacy = own.items.find((i: { id: string }) => i.id === fixture.legacyId); expect(legacy).toBeTruthy();
  await page.getByRole('button', { name: `Abrir prática de P01 de ${new Date(legacy.createdAt).toLocaleString('pt-BR')}` }).click();
  await expect(page.getByRole('heading', { name: 'Questão 1 de 1' })).toBeVisible();
  await expect(page.locator('.practice-context').getByText('Assistente Administrativo', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Voltar à aula P01', exact: true }).click(); await expect(page).toHaveURL(/aulas\/P01\?cargo=secretario-de-escola/);
  await panel(page); await finish(page);
  await page.getByRole('button', { name: 'Sair', exact: true }).click(); await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Resultado da prática' })).toHaveCount(0);
});
test('simulado, prática, conflito, troca de conta e resposta tardia ficam isolados', async ({ page, context, request }) => {
  const fixture = await (await request.get(control + '/fixture')).json(); await account(context, fixture.cookie); await page.goto('/#/simulados');
  await page.getByLabel('Quantidade de Português').fill('1'); await page.getByRole('button', { name: 'Iniciar simulado', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Questão 1 de 1' })).toBeVisible();
  const mockRadio = page.getByRole('radio').first(); await mockRadio.check(); await expect(page.locator('.save-status:visible')).toHaveText('Respostas confirmadas.');
  await route(page, '#/aulas/P01'); await begin(page);
  let released = false; await page.route('**/lesson-practices/*/answers/*', async r => {
    if (!released) { released = true; return r.fulfill({ status: 409, json: { error: 'conflict' } }); } await r.continue();
  });
  await page.getByRole('radio').first().check(); await expect(page.getByText(/Outra alteração avançou/)).toBeVisible();
  await page.getByRole('button', { name: 'Recarregar tentativa', exact: true }).click(); await expect(page.getByRole('button', { name: 'Salvar escolhas preservadas' })).toBeVisible();
  await page.getByRole('button', { name: 'Salvar escolhas preservadas' }).click(); await expect(page.locator('.save-status:visible')).toHaveText('Respostas confirmadas.');
  let release!: () => void; const paused = new Promise<void>(r => release = r); let committed = false;
  await page.route('**/lesson-practices/*/answers/*', async r => { const response = await r.fetch(); committed = true; await paused; await r.fulfill({ response }).catch(() => {}); });
  try {
    await page.getByRole('radio').nth(1).check(); await expect.poll(() => committed).toBe(true);
    await route(page, '#/simulados'); await expect(page.getByRole('radio').first()).toBeChecked();
    await account(context, fixture.otherCookie); await page.getByRole('button', { name: 'Atualizar sessão' }).click();
    await expect(page.getByRole('button', { name: 'Iniciar simulado', exact: true })).toBeVisible(); release();
    await route(page, '#/praticas'); await expect(page.getByRole('heading', { name: 'Qual assunto vamos praticar?' })).toBeVisible();
    await expect(page.getByRole('radio')).toHaveCount(0);
    expect((await page.request.get(api + '/api/v1/lesson-practices/' + fixture.legacyId)).status()).toBe(404);
  } finally { release(); }
});
test('expiração limpa prática e simulado antes de aceitar confirmações tardias', async ({ page, context, request }) => {
  const fixture = await (await request.get(control + '/fixture')).json(); await account(context, fixture.cookie); await page.goto('/#/aulas/P01'); await begin(page);
  expect((await request.post(control + '/expire', { headers: { 'X-Fixture-Session': fixture.cookie } })).status()).toBe(204);
  await page.getByRole('button', { name: 'Atualizar sessão' }).click(); await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();
  await expect(page.getByRole('radio')).toHaveCount(0);
});
