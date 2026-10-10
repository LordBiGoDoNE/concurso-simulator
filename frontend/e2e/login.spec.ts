import { test, expect } from '@playwright/test';

for (const width of [390, 1280]) {
  test(`login opcional, falha e saída por teclado em ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 1000 });
    let connected = false;
    await page.route('**/api/v1/status', route => route.fulfill({ json: { status: 'UP' } }));
    await page.route('**/api/v1/auth/config', route => route.fulfill({ json: { googleEnabled: true } }));
    await page.route('**/api/v1/me', route => route.fulfill({ status: connected ? 200 : 401,
      json: connected ? { id: '12345678-1234-1234-1234-123456789abc' } : { error: 'unauthenticated' } }));
    await page.route('**/api/v1/csrf', route => route.fulfill({ json: { token: 'browser-csrf', headerName: 'X-CSRF-TOKEN' } }));
    await page.route('**/api/v1/auth/logout', route => {
      expect(route.request().headers()['x-csrf-token']).toBe('browser-csrf');
      connected = false;
      return route.fulfill({ status: 204 });
    });
    await page.goto('/?auth=failed');
    await expect(page.getByRole('alert')).toContainText('Não foi possível entrar');
    await expect(page).not.toHaveURL(/auth=failed/);
    await expect(page.getByRole('link', { name: 'Entrar com Google' })).toBeVisible();
    await page.keyboard.press('Tab');
    await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.getByRole('link', { name: 'Entrar com Google' })).toBeFocused();
    connected = true;
    await page.getByRole('button', { name: 'Atualizar sessão' }).click();
    await expect(page.getByText('Você está conectado.')).toBeVisible();
    await page.getByRole('button', { name: 'Sair', exact: true }).focus();
    await page.keyboard.press('Enter');
    await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  });
}
