import { test, expect } from '@playwright/test';
import { catalog, emptyPage } from '../test/question-fixtures';

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/**', route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith('/status')) return route.fulfill({ json: { status: 'UP' } });
    if (path.endsWith('/auth/config')) return route.fulfill({ json: { googleEnabled: false } });
    if (path.endsWith('/me')) return route.fulfill({ status: 401, json: { error: 'unauthenticated' } });
    if (path.endsWith('/catalog')) return route.fulfill({ json: catalog });
    if (path.includes('/question-bank/questions')) return route.fulfill({ json: emptyPage });
    return route.fulfill({ status: 503, json: { error: 'unavailable' } });
  });
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
