// Ensaio manual do acervo real, somente leitura. Não faz parte do CI de fixtures.
const { readFileSync, writeFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { chromium, expect } = require('../frontend/node_modules/@playwright/test');

async function main() {
  const root = resolve(__dirname, '..');
  const directory = resolve(root, 'backend/build/curation');
  const coverage = JSON.parse(readFileSync(resolve(directory, 'coverage.json'), 'utf8'));
  const browser = await chromium.launch({ headless: true });
  let details = 0;
  const bodies = [];
  try {
    for (const width of [390, 1280]) {
      const page = await browser.newPage({ viewport: { width, height: 900 } });
      page.on('requestfinished', request => {
        if (request.url().startsWith('http://127.0.0.1:18180/api/v1/question-bank/')) {
          const body = request.response().then(response => response.text());
          void body.catch(() => {});
          bodies.push(body);
        }
      });
      await page.goto('http://127.0.0.1:5173');
      await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();
      await expect(page.getByText('250 questões elegíveis. Página 1.')).toBeVisible();
      await page.getByRole('button', { name: 'Próxima página' }).click();
      await expect(page.getByText('250 questões elegíveis. Página 2.')).toBeVisible();
      await expect(page.getByRole('heading', { name: 'Resultados da consulta' })).toBeFocused();
      for (const [discipline, topic, role] of [
        ['portugues', 'P01', 'assistente-administrativo'],
        ['matematica-raciocinio', 'M01', 'assistente-administrativo'],
        ['informatica', 'I01', 'assistente-administrativo'],
        ['conhecimentos-administrativos', 'A01', 'assistente-administrativo'],
        ['conhecimentos-escolares', 'S01', 'secretario-de-escola'],
      ]) {
        await page.getByLabel('Disciplina', { exact: true }).selectOption(discipline);
        await page.getByLabel('Assunto', { exact: true }).selectOption('');
        await page.getByLabel('Cargo', { exact: true }).selectOption('');
        await expect(page.getByText('50 questões elegíveis. Página 1.')).toBeVisible();
        await page.getByLabel('Assunto', { exact: true }).selectOption(topic);
        await page.getByLabel('Cargo', { exact: true }).selectOption(role);
        await expect(page.getByText(`${coverage.topics[topic]} questões elegíveis. Página 1.`)).toBeVisible();
        const button = page.getByRole('button', { name: /Consultar:/ }).first();
        const label = await button.textContent();
        await button.focus();
        await page.keyboard.press('Enter');
        await expect(page.getByRole('heading', { name: 'Detalhe da questão' })).toBeFocused();
        await expect(page.locator('article ol li')).toHaveCount(4);
        await expect(page.locator('article').getByRole('heading', { name: 'Fontes para estudo' })).toBeVisible();
        expect(await page.locator('article a').count()).toBeGreaterThan(0);
        await expect(page.getByRole('radio')).toHaveCount(0);
        await expect(page.locator('[data-correct], .correct, .answer')).toHaveCount(0);
        expect(await page.content()).not.toMatch(/correctAlternativeId|"resolution"|"checklist"/);
        await page.getByRole('button', { name: 'Fechar detalhe' }).click();
        await expect(page.getByRole('button', { name: label.trim(), exact: true })).toBeFocused();
        details += 1;
      }
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
      await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();
      expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
      await page.screenshot({ path: resolve(directory, `curation-${width}.png`), fullPage: true });
      await page.route('http://127.0.0.1:18180/api/v1/question-bank/**', route => route.abort('failed'));
      await page.getByLabel('Disciplina', { exact: true }).selectOption('portugues');
      await expect(page.getByText('Consulta de questões indisponível. Não é um acervo vazio.')).toBeVisible();
      await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();
      await page.unroute('http://127.0.0.1:18180/api/v1/question-bank/**');
      await page.getByRole('button', { name: 'Tentar questões novamente' }).click();
      await expect(page.getByText('50 questões elegíveis. Página 1.')).toBeVisible();
      await page.close();
    }
    for (const body of await Promise.all(bodies)) {
      expect(body).not.toMatch(/"correctAlternativeId"|"resolution"|"provenance"|"status"|"checklist"/);
    }
    const result = { verifiedAt: new Date().toISOString(), widths: [390, 1280],
      disciplinesPerWidth: 5, detailsChecked: details, publicBodiesChecked: bodies.length,
      keyboardFocus: true, noHorizontalOverflow: true, noCorrectiveFields: true,
      studyLinkDuringFailure: true, retryRecovers: true, storageEmpty: true,
      executedBy: 'coordinator', data: '250 real approved revisions; no fixtures counted' };
    writeFileSync(resolve(directory, 'browser-verification.json'), JSON.stringify(result, null, 2) + '\n');
    console.log(JSON.stringify(result));
  } finally {
    await browser.close();
  }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
