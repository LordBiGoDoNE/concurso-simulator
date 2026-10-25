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
test('prática de aula não substitui gravação real já confirmada no servidor com resposta suspensa', async ({ page }) => {
  await page.goto('/');await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
  const view = await start(page);let release!: () => void;const paused = new Promise<void>(resolve => { release = resolve; });let committed = false, confirmations = 0, newAttempts = 0;
  page.on('dialog', dialog => { confirmations++;void dialog.accept(); });
  page.on('request', request => { if (request.method() === 'POST' && request.url().endsWith('/attempts')) newAttempts++; });
  await page.route('**/api/v1/attempts/*/answers/*', async route => {
    const response = await route.fetch();expect(response.status()).toBe(200);committed = true;await paused;await route.fulfill({ response });
  });
  try {
    await area(page).getByRole('radio').first().check();await expect.poll(() => committed).toBe(true);
    await page.getByText('Ir para outra área', { exact: true }).click();
    await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Aulas', exact: true }).click();
    await page.getByRole('button', { name: /P01.*Interpretação de Textos/ }).click();await page.getByRole('button', { name: 'Praticar P01', exact: true }).click();
    await expect(page.getByRole('heading', { name:'Prática das aulas',exact:true })).toBeVisible();
    await expect(page.getByRole('heading', {name:'Questão 1 de 5'})).toBeVisible();expect(confirmations).toBe(0);expect(newAttempts).toBe(0);
    expect((await (await page.request.get('/api/v1/attempts/' + view.id)).json()).version).toBe(1);
    await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
    await expect(area(page).getByRole('radio').first()).toBeChecked();await expect(area(page).getByRole('button', { name: 'Concluir simulado', exact: true })).toBeDisabled();
    release();await expect(area(page).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();expect(newAttempts).toBe(0);
  } finally { release(); }
});
for (const width of [390, 1280]) test(`biblioteca e leitura integral no pacote Docker em ${width}px`, async ({ page }, info) => {
  const paths: Record<string, string> = JSON.parse(readFileSync('src/lesson-paths.json', 'utf8'));
  for (const path of Object.values(paths)) {
    const response = await page.request.get('/material/' + path.split('/').map(encodeURIComponent).join('/'));
    expect(response.status()).toBe(200);expect(response.headers()['content-type']).toContain('text/html');
  }
  await page.setViewportSize({ width, height: 900 });await page.goto('/');
  await page.getByRole('button', { name: 'Explorar as aulas' }).click();
  await expect(page.getByRole('status').filter({ hasText: '51 aulas encontradas.' })).toBeVisible();
  await page.getByLabel('Buscar aula').fill('M01');
  await page.getByRole('button', { name: /M01.*Operações com Números Reais/ }).click();
  await expect(page.getByRole('link', { name: 'Abrir aula original (nova aba)' })).toHaveAttribute('href', origin + '/material/' + paths.M01.split('/').map(encodeURIComponent).join('/'));
  const frame = page.frameLocator('iframe[title="Aula completa: Operações com Números Reais"]');
  await expect(frame.getByRole('heading', { name: 'Matemática 01 - Operações com Números Reais', exact: true })).toBeVisible();
  expect(await frame.locator('math').count()).toBeGreaterThan(0);
  await frame.getByRole('heading', { name: 'Checklist de domínio', exact: true }).scrollIntoViewIfNeeded();
  await expect(frame.getByRole('heading', { name: 'Checklist de domínio', exact: true })).toBeVisible();
  await expect(frame.locator('details.question-solution')).toHaveCount(10);
  await frame.locator('img').first().scrollIntoViewIfNeeded();await expect(frame.locator('img').first()).toBeVisible();
  expect(await frame.locator('img').first().evaluate((image: HTMLImageElement) => image.naturalWidth)).toBeGreaterThan(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath(`packaged-reader-${width}.png`), fullPage: true });
  let posts = 0;page.on('request', request => { if (request.method() === 'POST' && request.url().endsWith('/attempts')) posts++; });
  await frame.getByRole('link', { name: 'Próximo módulo →', exact: true }).click();
  await expect(frame.getByRole('heading', { name: 'Matemática 02 - MMC e MDC', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Praticar M01', exact: true })).toHaveAccessibleDescription('Assunto da prática: M01 — Operações com Números Reais');
  await page.getByRole('button', { name: 'Praticar M01', exact: true }).click();
  await expect(page.getByRole('heading', {name:'Prática das aulas',exact:true})).toBeVisible();
  await expect(page.getByRole('heading', {name:'Questão 1 de 5'})).toBeVisible();
  await expect(page.getByRole('button', {name:'Encerrar prática'})).toBeVisible();expect(posts).toBe(0);
});
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

for(const width of [390,1280])test(`prática exclusiva: retry, correção final e retorno em ${width}px`,async({page,context},info)=>{
  await page.setViewportSize({width,height:900});await session(context,data.cookie);await page.goto('/');
  await page.getByRole('button',{name:'Explorar as aulas'}).click();await page.getByRole('button',{name:/P01.*Interpretação de Textos/}).click();
  const bodies:unknown[]=[];let createdId='';
  await page.route('**/api/v1/lesson-practices',async route=>{
    bodies.push(route.request().postDataJSON());const response=await route.fetch();expect(response.status()).toBe(bodies.length===1?201:200);
    if(bodies.length===1){createdId=(await response.json()).id;await route.abort('failed');}else await route.fulfill({response});
  });
  await page.getByRole('button',{name:'Praticar P01',exact:true}).click();
  await page.getByRole('button',{name:'Tentar iniciar prática novamente'}).click();
  await expect(page.getByRole('heading',{name:'Questão 1 de 5'})).toBeVisible();expect(bodies).toHaveLength(2);expect(bodies[0]).toEqual(bodies[1]);
  const live=await(await page.request.get('/api/v1/lesson-practices/'+createdId)).json();activeContract(live);
  expect(live.items).toHaveLength(5);expect(new Set(live.items.map((i:{questionId:string})=>i.questionId)).size).toBe(5);
  expect((await page.request.get('/api/v1/attempts/'+createdId)).status()).toBe(404);
  expect((await page.request.get('/api/v1/lesson-practices/'+data.activeId)).status()).toBe(404);
  expect((await page.request.get('/api/v1/lesson-practices/'+createdId+'/result')).status()).toBe(409);
  const mock=await(await page.request.get('/api/v1/attempts/'+data.activeId)).json();
  expect(live.items.some((i:{questionId:string})=>mock.items.some((m:{questionId:string})=>m.questionId===i.questionId))).toBe(false);
  await page.getByRole('radio').first().check();await expect(page.getByText('Respostas confirmadas.',{exact:true})).toBeVisible();
  await expect(page.getByRole('heading',{name:'Resolução',exact:true})).toHaveCount(0);
  await page.getByRole('button',{name:'Encerrar prática'}).click();await expect(page.getByRole('dialog')).toContainText('1 de 5 questões respondidas; 4 em branco.');
  await page.keyboard.press('Escape');await expect(page.getByRole('button',{name:'Encerrar prática'})).toBeFocused();
  await page.getByRole('button',{name:'Encerrar prática'}).click();await page.getByRole('button',{name:'Confirmar encerramento'}).click();
  await expect(page.getByRole('heading',{name:'Resultado da prática'})).toBeVisible();await expect(page.getByRole('heading',{name:'Resolução',exact:true})).toHaveCount(5);
  const own=await(await page.request.get('/api/v1/me/lesson-practices')).json();expect(own.items.some((i:{id:string})=>i.id===createdId)).toBe(true);
  const history=await(await page.request.get('/api/v1/me/attempts')).json();expect(history.items.some((i:{id:string})=>i.id===createdId)).toBe(false);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);await page.screenshot({path:info.outputPath(`lesson-practice-${width}.png`),fullPage:true});
  await page.getByRole('button',{name:'Voltar à aula P01'}).click();await expect(page.getByRole('button',{name:'Praticar P01',exact:true})).toBeVisible();
});
