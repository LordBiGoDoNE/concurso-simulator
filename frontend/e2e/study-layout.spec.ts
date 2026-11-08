import { test, expect } from '@playwright/test';
import { catalog, emptyPage } from '../test/question-fixtures';
import { view } from '../test/attempt-fixtures';

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/**', route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith('/status')) return route.fulfill({ json: { status: 'UP' } });
    if (path.endsWith('/auth/config')) return route.fulfill({ json: { googleEnabled: false } });
    if (path.endsWith('/me')) return route.fulfill({ status: 401, json: { error: 'unauthenticated' } });
    if(path.endsWith('/csrf'))return route.fulfill({json:{token:'test',headerName:'X-CSRF-TOKEN'}});
    if(path.endsWith('/lesson-practices/availability')) {
      const params = new URL(route.request().url()).searchParams;
      return route.fulfill({json:{topicId:params.get('topicId'),roleId:params.get('roleId'),available:5,maximum:5}});
    }
    if(path.includes('/lesson-practices')) {
      const body=route.request().method()==='POST'?route.request().postDataJSON():{topicId:'P01',roleId:view.roleId};
      const topicId=body.topicId;const disciplineId=topicId.startsWith('M')?'matematica-raciocinio':'portugues';
      const practice={...view,id:'90000000-0000-0000-0000-000000000001',roleId:body.roleId,items:[{...view.items[0],classification:{...view.items[0].classification,topicId,disciplineId}}]};
      // Preserve topic on the initial read used to recheck access.
      return route.fulfill({status:route.request().method()==='POST'?201:200,json:practice});
    }
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
for (const width of [390, 1440]) test(`prática separada preserva gravação do simulado em ${width}px`, async ({ page }) => {
  let current = { ...view, items: [...view.items, { ...view.items[0], id: '80000000-0000-0000-0000-000000000002', questionId: '10000000-0000-0000-0000-000000000002' }] };
  let release!: () => void;const paused = new Promise<void>(resolve => { release = resolve; });let writes = 0, creates = 0, confirmations = 0;
  page.on('dialog', dialog => { confirmations++;void dialog.accept(); });
  await page.route('**/api/v1/csrf', route => route.fulfill({ json: { token: 'test', headerName: 'X-CSRF-TOKEN' } }));
  await page.route('**/api/v1/attempts**', async route => {
    if (route.request().method() === 'PUT') {
      writes++;await paused;const answer = route.request().postDataJSON();const id = new URL(route.request().url()).pathname.split('/').at(-1);
      current = { ...current, version: current.version + 1, items: current.items.map(item => item.id === id ? { ...item, selectedAlternativeId: answer.alternativeId } : item) };
    }
    if (route.request().method() === 'POST') creates++;
    return route.fulfill({ status: route.request().method() === 'POST' ? 201 : 200, json: current });
  });
  await page.setViewportSize({ width, height: 900 });await page.goto('/');await page.getByRole('button', { name: 'Montar um simulado' }).click();
  await page.getByLabel('Quantidade de Português').fill('2');await page.getByRole('button', { name: 'Iniciar simulado' }).click();
  try {
    await page.getByRole('radio').first().check();await page.getByRole('button', { name: 'Próxima questão' }).click();await page.getByRole('radio').first().check();
    await expect.poll(() => writes).toBe(1);
    const nav = page.getByRole('navigation', { name: 'Navegação principal' });await expect(nav.getByRole('button', { name: 'Aulas', exact: true })).toBeHidden();
    await page.getByText('Ir para outra área', { exact: true }).focus();await page.keyboard.press('Enter');
    await nav.getByRole('button', { name: 'Aulas', exact: true }).click();
    await page.getByRole('button', { name: /P01.*Interpretação de Textos/ }).click();await page.locator('details.lesson-practice-panel > summary').click();await page.getByRole('button', { name: 'Começar prática', exact: true }).click();
    await expect(page.locator('details.lesson-practice-panel')).toHaveAttribute('open');await expect(page).toHaveURL(/#\/aulas\/P01/);expect(confirmations).toBe(0);expect(creates).toBe(1);
    await nav.getByRole('button', { name: 'Simulados', exact: true }).click();await expect(page.getByRole('heading', { name: 'Questão 2 de 2' })).toBeVisible();
    await expect(page.getByRole('radio').first()).toBeChecked();release();await expect(page.getByRole('region',{name:'Simulados',exact:true}).getByText('Respostas confirmadas.', { exact: true })).toBeVisible();expect(writes).toBe(2);
  } finally { release(); }
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
  let created = 0;
  page.on('request', request => { if (request.method() === 'POST' && request.url().endsWith('/attempts')) created++; });
  await page.setViewportSize({ width, height: 900 });await page.goto('/');
  await page.getByRole('button', { name: 'Explorar todas as aulas' }).click();
  await expect(page.getByRole('status').filter({ hasText: '51 aulas encontradas.' })).toBeVisible();
  await page.getByLabel('Buscar aula').fill('interpretacao');await expect(page.getByRole('status').filter({ hasText: '1 aula encontrada.' })).toBeVisible();
  await page.getByLabel('Filtrar disciplina').selectOption('informatica');await expect(page.getByRole('heading', { name: 'Nenhuma aula por aqui' })).toBeVisible();
  await page.getByRole('button', { name: 'Limpar busca e filtros' }).click();
  await page.screenshot({ path: info.outputPath(`lessons-${width}.png`), fullPage: true });
  await page.getByLabel('Buscar aula').fill('M01');await page.getByRole('button', { name: /M01.*Operações com Números Reais/ }).click();
  await expect(page.getByRole('heading', { name: 'Operações com Números Reais', exact: true })).toBeFocused();
  const article = page.getByRole('article', { name: 'Conteúdo da aula: Operações com Números Reais' });
  await expect(article).toBeVisible();
  await expect(article.getByRole('heading', { name: '1. Números e sinais, sem decorar primeiro' })).toBeVisible();
  await expect(article.getByRole('img')).toBeVisible();
  expect(await article.locator('math').count()).toBeGreaterThan(0);
  await expect(page.locator('iframe')).toHaveCount(0);
  await expect(page.locator('h1:visible')).toHaveCount(1);
  await expect(article.getByRole('link', { name: /^(← Índice V3|Gabarito e revisão|Próximo módulo →)$/ })).toHaveCount(0);
  const before = page.url();
  await article.getByRole('button', { name: '2. Frações e decimais: partes do mesmo tamanho' }).click();
  await expect(article.locator('#lesson-M01-s2')).toBeFocused();expect(page.url()).toBe(before);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath(`reader-${width}.png`), fullPage: true });
  await page.locator('details.lesson-practice-panel > summary').click();
  await expect(page.getByRole('button', { name: 'Começar prática', exact: true })).toHaveAccessibleDescription('Assunto da prática: M01 — Operações com Números Reais');
  await expect(page.getByText('Para mudar o assunto da prática, escolha outra aula no índice acima.')).toBeVisible();
  await page.getByRole('button', { name: 'Começar prática', exact: true }).click();
  await expect(page).toHaveURL(/#\/aulas\/M01/);
  await expect(page.getByRole('heading', { name:'Questão 1 de 1' })).toBeVisible();
  await expect(page.getByRole('button', {name:'Encerrar prática'})).toBeVisible();expect(created).toBe(0);
});
