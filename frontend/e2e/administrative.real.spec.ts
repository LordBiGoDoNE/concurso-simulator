import { test, expect, type APIRequestContext, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
const api = 'http://127.0.0.1:19086', provider = 'http://127.0.0.1:19087';
const chosen = 'synthetic-chosen-browser-password';
async function post(request: APIRequestContext, path: string, data: unknown) {
  const csrf = await (await request.get(api + '/api/v1/csrf')).json();
  return request.post(api + path, { data, headers: { [csrf.headerName]: csrf.token } });
}
async function openAdmin(page: Page) {
  const details = page.getByText('Acesso administrativo', { exact: true });
  if (!(await details.evaluate(el => (el.parentElement as HTMLDetailsElement).open))) await details.click();
}
async function localLogin(page: Page, password: string) {
  await page.goto('/'); await openAdmin(page);
  await page.getByLabel('Usuário ADM', { exact: true }).fill('admin');await page.getByLabel('Usuário ADM', { exact: true }).focus();
  await page.keyboard.press('Tab');await expect(page.getByLabel('Senha ADM', { exact: true })).toBeFocused();await page.keyboard.type(password);
  await page.keyboard.press('Enter');await expect(page.getByText('Você está conectado.')).toBeVisible();
}
test('senha temporária, conta Google existente, dois logins, ambos os acervos e reinício preservam dados', async ({ browser, page, request }, testInfo) => {
  await request.post(provider + '/fixture/control', { form: { subject: 'administrative-browser-existing', mode: 'valid' } });
  const google = await browser.newContext();
  try {
    const student = await google.newPage();await student.goto('/');await student.getByRole('link', { name: 'Entrar com Google' }).click();
    await expect(student.getByText('Você está conectado.')).toBeVisible();
    const original = await (await google.request.get(api + '/api/v1/me')).json();
    await student.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
    await student.getByLabel('Quantidade de Português').fill('1');
    const creating = student.waitForResponse(r => r.url() === api + '/api/v1/attempts' && r.request().method() === 'POST');
    await student.getByRole('button', { name: 'Iniciar simulado', exact: true }).click();const created = await creating;expect(created.status()).toBe(201);const history = await created.json();
    await localLogin(page, 'synthetic-browser-temporary-password');
    await expect(page.getByText(/Troque a senha temporária/)).toBeVisible();
    expect((await post(page.request, '/api/v1/admin/question-imports/preview', {})).status()).toBe(403);
    await page.getByLabel('Senha atual', { exact: true }).fill('synthetic-browser-temporary-password');await page.getByLabel('Nova senha', { exact: true }).fill(chosen);
    await page.getByRole('button', { name: 'Trocar senha', exact: true }).click();await expect(page.getByText('Senha atualizada.')).toBeVisible();
    await expect(page.getByText(/Troque a senha temporária/)).not.toBeVisible();
    await page.getByLabel('Senha atual', { exact: true }).fill(chosen);await page.getByRole('button', { name: 'Vincular Google', exact: true }).click();
    await expect(page.getByText('Google vinculado. Você pode entrar por Google ou senha.')).toBeVisible();
    expect(await (await page.request.get(api + '/api/v1/me')).json()).toEqual(original);
    expect(await (await page.request.get(api + '/api/v1/attempts/' + history.id)).json()).toEqual(history);
    expect((await google.request.get(api + '/api/v1/admin/account')).status()).toBe(403);
    await page.getByRole('button', { name: 'Sair', exact: true }).click();await localLogin(page, chosen);
    expect(await (await page.request.get(api + '/api/v1/me')).json()).toEqual(original);
    for (const purpose of ['SIMULADO', 'PRATICA_AULA']) {
      const input = JSON.parse(readFileSync('../docs/examples/administrative-question-import.json', 'utf8'));
      input.package.purpose = purpose;input.package.items[0].origin.namespace = purpose === 'SIMULADO' ? 'admin-question-v1' : 'lesson-practice-v1';
      input.package.items[0].origin.key = 'admin.browser-' + purpose;input.keys = [input.package.items[0].origin.key];
      const preview = await post(page.request, '/api/v1/admin/question-imports/preview', input);expect(preview.status()).toBe(200);
      const confirmation = await preview.json();const apply = { ...input, confirmationToken: confirmation.confirmationToken };
      const applied = await post(page.request, '/api/v1/admin/question-imports/apply', apply);expect(applied.status()).toBe(200);const receipt = await applied.json();
      expect(receipt.replayed).toBe(false);expect((await (await post(page.request, '/api/v1/admin/question-imports/apply', apply)).json()).replayed).toBe(true);
      expect((await request.get(api + '/api/v1/question-bank/questions/' + receipt.receipt.results[0].questionId)).status()).toBe(404);
    }
    expect((await request.post(provider + '/fixture/restart')).ok()).toBe(true);
    expect(await (await page.request.get(api + '/api/v1/me')).json()).toEqual(original);
    await page.reload();await openAdmin(page);await expect(page.getByText('Google vinculado. Você pode entrar por Google ou senha.')).toBeVisible();
    await page.screenshot({ path: testInfo.outputPath('admin-account.png'), fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    await expect(page.getByLabel('Senha atual', { exact: true })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
    await page.locator('.admin-account').screenshot({ path: testInfo.outputPath('admin-account-mobile.png') });
    await page.getByRole('button', { name: 'Sair', exact: true }).click();await page.getByRole('link', { name: 'Entrar com Google' }).click();
    await expect(page.getByText('Você está conectado.')).toBeVisible();expect(await (await page.request.get(api + '/api/v1/me')).json()).toEqual(original);
    expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  } finally { await google.close(); }
});
