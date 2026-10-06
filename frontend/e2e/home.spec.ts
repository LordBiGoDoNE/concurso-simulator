import { test, expect } from '@playwright/test';

for (const width of [390, 1280]) {
  test(`entrada acessível com API indisponível em ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await page.route('**/api/v1/status', route => route.abort('connectionrefused'));
    await page.goto('/');
    await expect(page.getByRole('status')).toContainText('API indisponível');
    await page.keyboard.press('Tab');
    await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.getByRole('button')).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('status')).toContainText('API indisponível');
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  });
}

test('apresenta disponibilidade da API', async ({ page }) => {
  await page.route('**/api/v1/status', route => route.fulfill({ json: { status: 'UP' } }));
  await page.goto('/');
  await expect(page.getByRole('status')).toHaveText('API disponível.');
});
