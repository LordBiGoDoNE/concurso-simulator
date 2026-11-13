import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
const catalog: { topics: { id: string; title: string }[] } = JSON.parse(readFileSync('src/lesson-catalog.json', 'utf8'));

for (const width of [390, 1440]) test(`51 aulas nativas completas e responsivas sem API em ${width}px`, async ({ page }, info) => {
  test.setTimeout(90000);
  const errors: string[] = [], legacyRequests: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('request', request => { if (/github\.io|\/material\/|Gabarito|\.html(?:\?|$)/i.test(request.url())) legacyRequests.push(request.url()); });
  await page.route('**/api/v1/**', route => route.abort());
  await page.setViewportSize({ width, height: 900 });await page.goto('/#/aulas/P01');
  for (const topic of catalog.topics) {
    await page.evaluate(id => { window.location.hash = '#/aulas/' + id; }, topic.id);
    const article = page.getByRole('article', { name: 'Conteúdo da aula: ' + topic.title, exact: true });
    await expect(article, topic.id).toBeVisible();
    await expect(page.locator('h1:visible')).toHaveText(topic.title);
    await expect(page.locator('iframe')).toHaveCount(0);
    expect((await article.innerText()).length, topic.id).toBeGreaterThan(1500);
    await expect(article.getByRole('heading', { name: /^Referências e/ })).toHaveCount(1);
    await expect(article.locator('[id$="-exercicios"], .question-solution')).toHaveCount(0);
    await expect(page.locator('details.lesson-practice-panel')).not.toHaveAttribute('open');
    const geometry = await page.evaluate(() => ({
      overflow: document.documentElement.scrollWidth > innerWidth,
      reader: document.querySelector('.reader-layout')!.getBoundingClientRect().width,
      content: document.querySelector('.native-lesson')!.getBoundingClientRect().width,
      index: document.querySelector('.reader-index')!.getBoundingClientRect().bottom,
      heading: document.querySelector('#lesson-reader-title')!.getBoundingClientRect().top,
    }));
    expect(geometry.overflow, topic.id).toBe(false);
    expect(Math.abs(geometry.reader - geometry.content), topic.id).toBeLessThan(1);
    expect(geometry.index, topic.id).toBeLessThan(geometry.heading);
    if (topic.id === 'I05' && width === 390) {
      const diagram = article.getByRole('region', { name: 'Esquema da aula' });
      await diagram.focus();await page.keyboard.press('End');await page.keyboard.press('ArrowRight');
      await expect.poll(() => diagram.evaluate(element => element.scrollLeft)).toBeGreaterThan(0);
    }
    if (['P01', 'M04', 'I05'].includes(topic.id)) {
      await article.scrollIntoViewIfNeeded();await page.screenshot({ path: info.outputPath(`${topic.id}-${width}.png`) });
    }
  }
  expect(errors).toEqual([]);expect(legacyRequests).toEqual([]);
});

test('falha ao buscar conteúdo oferece retry da aula sem API ou criação de atividade', async ({ page }) => {
  let reads = 0, posts = 0, failing = true;
  await page.route('**/api/v1/**', route => { if (route.request().method() === 'POST') posts++;return route.abort(); });
  await page.route('**/P01.json*', route => {
    if (route.request().resourceType() !== 'fetch') return route.continue();
    reads++;return failing ? route.abort() : route.continue();
  });
  await page.goto('/#/aulas/P01');await expect(page.getByRole('alert').filter({ hasText: 'Não foi possível carregar esta aula.' })).toBeVisible();
  const failedReads = reads;failing = false;
  await page.getByRole('button', { name: 'Tentar carregar aula novamente' }).click();
  await expect(page.getByRole('article', { name: 'Conteúdo da aula: Interpretação de Textos' })).toBeVisible();
  expect(reads).toBe(failedReads + 1);expect(posts).toBe(0);
});

for (const width of [390, 1440]) test(`Próxima aula e Aula anterior voltam ao topo em ${width}px`, async ({ page }) => {
  await page.route('**/api/v1/**', route => route.abort());
  await page.setViewportSize({ width, height: 900 });await page.goto('/#/aulas/P01');
  await expect(page.getByRole('article', { name: 'Conteúdo da aula: Interpretação de Textos' })).toBeVisible();
  const pagination = page.getByRole('navigation', { name: 'Navegação entre aulas' });
  await pagination.scrollIntoViewIfNeeded();expect(await page.evaluate(() => scrollY)).toBeGreaterThan(1000);
  await pagination.getByRole('button', { name: 'Próxima aula' }).click();
  await expect(page).toHaveURL(/#\/aulas\/P02$/);
  await expect(page.getByRole('heading', { level: 1 })).toBeFocused();
  await expect.poll(() => page.evaluate(() => scrollY)).toBe(0);
  await expect(page.getByRole('article', { name: 'Conteúdo da aula: Sinônimos, Antônimos e Relações de Sentido' })).toBeVisible();
  expect(await page.evaluate(() => scrollY)).toBe(0);
  await pagination.scrollIntoViewIfNeeded();expect(await page.evaluate(() => scrollY)).toBeGreaterThan(1000);
  await pagination.getByRole('button', { name: 'Aula anterior' }).click();
  await expect(page.getByRole('article', { name: 'Conteúdo da aula: Interpretação de Textos' })).toBeVisible();
  await expect(page).toHaveURL(/#\/aulas\/P01$/);
  await expect.poll(() => page.evaluate(() => scrollY)).toBe(0);
});
