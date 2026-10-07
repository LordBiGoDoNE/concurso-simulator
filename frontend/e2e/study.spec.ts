import { test, expect } from '@playwright/test';
import { readdirSync } from 'node:fs';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';

test('nove módulos e 90 resoluções funcionam offline, sem JavaScript', async ({ browser }) => {
  test.skip(!process.env.STUDY_ROOT, 'Defina STUDY_ROOT com o pacote estático montado.');
  const directory = join(process.env.STUDY_ROOT!, '02_Matematica_Logica');
  const files = readdirSync(directory).filter(name => name.startsWith('Matemática') && !name.includes('Gabarito'));
  expect(files).toHaveLength(9);
  const context = await browser.newContext({ offline: true, javaScriptEnabled: false });
  const page = await context.newPage();
  for (const file of files) {
    await page.goto(pathToFileURL(join(directory, file)).href);
    const solutions = page.locator('details.question-solution');
    await expect(solutions).toHaveCount(10);
    for (let i = 0; i < 10; i++) {
      const solution = solutions.nth(i);
      await expect(solution).not.toHaveAttribute('open', '');
      await solution.locator('summary').click();
      await expect(solution).toHaveAttribute('open', '');
      await solution.locator('summary').click();
      await expect(solution).not.toHaveAttribute('open', '');
    }
    if (!file.startsWith('Matemática 04')) await expect(page.locator('math').first()).toBeVisible();
    const answer = file.replace('.html', ' - Gabarito e Revisão.html');
    await page.goto(pathToFileURL(join(directory, answer)).href);
    await expect(page.locator('body')).toContainText(/Resposta|Gabarito/);
  }
  await context.close();
});
