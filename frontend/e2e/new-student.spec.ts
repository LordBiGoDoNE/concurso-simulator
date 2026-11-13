import { test, expect, type Page } from '@playwright/test';
import { view } from '../test/practice-fixtures';
import { catalog, emptyPage } from '../test/question-fixtures';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

async function openPracticePanel(page:Page) {
  const panel=page.locator('details.lesson-practice-panel');
  if (!await panel.evaluate(element=>(element as HTMLDetailsElement).open)) await panel.locator('summary').click();
}

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/**', async route => {
    const request = route.request(), url = new URL(request.url());
    const path = url.pathname;
    if (path.endsWith('/status')) return route.fulfill({ json: { status: 'UP' } });
    if (path.endsWith('/auth/config')) return route.fulfill({ json: { googleEnabled: false } });
    if (path.endsWith('/me')) return route.fulfill({ status: 401, json: {} });
    if (path.endsWith('/catalog')) return route.fulfill({ json: catalog });
    if (path.endsWith('/question-bank/questions')) return route.fulfill({ json: emptyPage });
    if (path.endsWith('/availability')) return route.fulfill({ json: { topicId: url.searchParams.get('topicId'), available: 5, maximum: 5 } });
    if (path.endsWith('/csrf')) return route.fulfill({ json: { token: 'fixture', headerName: 'X-CSRF-TOKEN' } });
    if (path.includes('/lesson-practices')) {
      const practice = { ...view, items: [view.items[0]] };
      return route.fulfill({ status: request.method() === 'POST' ? 201 : 200, json: practice });
    }
    return route.fulfill({ status: 503, json: {} });
  });
});

for (const width of [390, 1440]) test(`cargo, aula, histórico e prática por teclado em ${width}px`, async ({ page }, info) => {
  let creates = 0;
  page.on('request', request => { if (request.method() === 'POST' && /\/api\/v1\/(?:lesson-practices|attempts)(?:\/|$)/.test(request.url())) creates++; });
  await page.setViewportSize({ width, height: 900 });await page.goto('/');
  await expect(page.getByText(/Não é um site oficial/)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Encontrar aulas' })).toBeDisabled();
  await page.getByLabel('Para qual cargo você vai estudar?').selectOption('assistente-administrativo');
  await page.screenshot({ path: info.outputPath(`entry-${width}.png`), fullPage: true });
  await page.getByRole('button', { name: 'Encontrar aulas' }).focus();await page.keyboard.press('Enter');
  await expect(page.getByText('Escolha uma disciplina ou busque um assunto para ver as aulas.')).toBeVisible();
  await expect(page.getByRole('button', { name: /S01.*LDB/ })).toHaveCount(0);
  await page.getByLabel('Filtrar disciplina').selectOption('portugues');
  await page.getByLabel('Buscar aula').fill('interpretacao');
  await page.getByRole('button', { name: /P01.*Interpretação de Textos/ }).focus();await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/#\/aulas\/P01\?cargo=assistente-administrativo/);
  await expect(page.getByRole('heading', { name: 'Interpretação de Textos', exact: true })).toBeFocused();
  await expect(page.locator('details.lesson-practice-panel')).not.toHaveAttribute('open');
  await openPracticePanel(page);
  await expect(page.getByRole('button', { name: 'Começar prática' })).toBeEnabled();
  expect(creates).toBe(0);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Interpretação de Textos', exact: true })).toBeVisible();
  await page.goBack();
  await expect(page.getByLabel('Buscar aula')).toHaveValue('interpretacao');
  await expect(page.getByLabel('Filtrar disciplina')).toHaveValue('portugues');
  await page.goForward();await openPracticePanel(page);await expect(page.getByRole('button', { name: 'Começar prática' })).toBeEnabled();
  expect(creates).toBe(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath(`lesson-${width}.png`), fullPage: true });
  await page.getByRole('button', { name: 'Começar prática' }).focus();await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Questão 1 de 1' })).toBeVisible();
  await expect(page).toHaveURL(/#\/aulas\/P01\?/);
  await expect(page.getByRole('button', { name: 'Prática em andamento' })).toBeVisible();
  expect(creates).toBe(1);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0,0]);
});

test('sem API, o novo estudante chega à aula e recebe erro de consulta, sem falsa ausência', async ({ page }) => {
  await page.route('**/api/v1/**', route => route.abort());
  await page.goto('/');
  await page.getByLabel('Para qual cargo você vai estudar?').selectOption('secretario-de-escola');
  await page.getByRole('button', { name: 'Encontrar aulas' }).click();
  await expect(page.getByText('Escolha uma disciplina ou busque um assunto para ver as aulas.')).toBeVisible();
  await page.getByLabel('Filtrar disciplina').selectOption('conhecimentos-escolares');
  await page.getByRole('button', { name: /S02.*Escrituração Escolar/ }).click();
  await expect(page.getByRole('heading', { name: 'Escrituração Escolar', exact: true })).toBeVisible();
  await openPracticePanel(page);
  await expect(page.getByText(/Isso não significa ausência de questões/)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Começar prática' })).toBeDisabled();
  await expect(page.getByRole('article', { name: 'Conteúdo da aula: Escrituração Escolar' })).toBeVisible();
});

test('consulta vazia, falha e nova consulta não criam atividades', async ({ page }) => {
  let phase = 'empty', creates = 0;
  page.on('request', request => { if (request.method() === 'POST' && /\/api\/v1\/(?:lesson-practices|attempts)(?:\/|$)/.test(request.url())) creates++; });
  await page.route('**/lesson-practices/availability?**', route => phase === 'error' ? route.abort() : route.fulfill({ json: { topicId: 'P01', available: phase === 'empty' ? 0 : 5, maximum: 5 } }));
  await page.goto('/#/aulas/P01?cargo=assistente-administrativo');
  await openPracticePanel(page);
  await expect(page.getByText(/ainda não tem questões próprias/)).toBeVisible();
  phase = 'error';await page.reload();await openPracticePanel(page);
  await expect(page.getByText(/Isso não significa ausência de questões/)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Começar prática' })).toBeDisabled();
  phase = 'ready';await page.getByRole('button', { name: 'Consultar prática novamente' }).click();
  await expect(page.getByRole('button', { name: 'Começar prática' })).toBeEnabled();
  expect(creates).toBe(0);
});

for (const width of [390,1440]) test(`carrega abaixo do botão e recolhe durante salvamento em ${width}px`,async({page})=>{
  const errors:string[]=[];page.on('pageerror',error=>errors.push(error.message));
  let releaseCreate!:()=>void,releaseSave!:()=>void;
  const creating=new Promise<void>(resolve=>releaseCreate=resolve),saving=new Promise<void>(resolve=>releaseSave=resolve);
  let current={...view,items:[view.items[0]]},creates=0;
  await page.route('**/api/v1/lesson-practices',async route=>{creates++;await creating;await route.fulfill({status:201,json:current});});
  await page.route(/\/api\/v1\/lesson-practices\/[0-9a-f-]+\/answers\/[0-9a-f-]+$/,async route=>{
    await saving;const answer=route.request().postDataJSON();current={...current,version:1,items:[{...current.items[0],selectedAlternativeId:answer.alternativeId}]};
    await route.fulfill({json:current});
  });
  try {
    await page.setViewportSize({width,height:900});await page.goto('/#/aulas/P01?cargo=assistente-administrativo');
    await expect(page.locator('details.lesson-practice-panel')).not.toHaveAttribute('open');await openPracticePanel(page);
    const start=page.getByRole('button',{name:'Começar prática',exact:true});await expect(start).toBeEnabled();
    expect(creates).toBe(0);await expect(page.getByRole('radio')).toHaveCount(0);await start.click();
    await expect(page.getByText('Carregando questões da prática…')).toBeVisible();await expect(page.getByRole('button',{name:'Carregando questões…',exact:true})).toBeDisabled();
    await expect(page).toHaveURL(/#\/aulas\/P01\?/);releaseCreate();await expect(page.getByRole('heading',{name:'Questão 1 de 1'})).toBeVisible();
    const button=await page.getByRole('button',{name:'Prática em andamento',exact:true}).boundingBox(),question=await page.getByRole('heading',{name:'Questão 1 de 1'}).boundingBox();
    expect(question!.y).toBeGreaterThan(button!.y+button!.height);expect(creates).toBe(1);
    await page.getByRole('radio').first().check();await expect(page.getByText('Salvando fila…',{exact:true})).toBeVisible();
    await page.getByRole('button',{name:'Recolher prática e voltar à leitura'}).click();await expect(page.locator('details.lesson-practice-panel')).not.toHaveAttribute('open');
    releaseSave();await openPracticePanel(page);await expect(page.getByText('Respostas confirmadas.',{exact:true})).toBeVisible();
    await expect(page.getByRole('radio').first()).toBeChecked();expect(creates).toBe(1);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    expect(errors).toEqual([]);
  } finally {releaseCreate();releaseSave();}
});

test('link inválido orienta e nenhuma rota pública cria atividades', async ({ page }) => {
  let creates = 0;page.on('request', request => { if (request.method() === 'POST' && /\/api\/v1\/(?:lesson-practices|attempts)(?:\/|$)/.test(request.url())) creates++; });
  await page.goto('/#/aulas/S01?cargo=assistente-administrativo&token=segredo');
  await expect(page.getByText(/O endereço tinha opções desconhecidas/)).toBeVisible();
  await expect(page).toHaveURL(/#\/aulas\?cargo=assistente-administrativo$/);
  for (const path of ['simulados','praticas','questoes','inicio']) {
    await page.goto('/#/' + path);await expect(page.getByRole('main')).toBeVisible();
  }
  expect(creates).toBe(0);
});

for (const width of [390,1440]) test(`protótipo documental é navegável e não cria atividades em ${width}px`, async ({ page }, info) => {
  await page.setViewportSize({ width, height: 900 });
  await page.goto(pathToFileURL(resolve('../design/prototypes/new-student-experience/index.html')).href);
  await page.getByLabel('Para qual cargo você vai estudar?').selectOption({ label: 'Secretário de Escola' });
  await page.getByRole('button', { name: /Encontrar aulas/ }).click();
  await expect(page.getByText('Aulas para Secretário de Escola')).toBeVisible();
  await page.getByRole('button', { name: /P01 · Interpretação/ }).click();
  await page.getByLabel('Simular estado da consulta').selectOption('empty');
  await expect(page.getByRole('button', { name: 'Praticar P01' })).toBeDisabled();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath(`prototype-${width}.png`), fullPage: true });
});
