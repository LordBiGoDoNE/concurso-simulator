import { test, expect, type Page, type APIRequestContext } from '@playwright/test';

const api = 'http://127.0.0.1:18080';
const provider = 'http://127.0.0.1:18081';

async function setProvider(request: APIRequestContext, subject: string, mode = 'valid') {
  expect((await request.post(`${provider}/fixture/control`, { form: { subject, mode } })).ok()).toBe(true);
}

async function login(page: Page) {
  await page.goto('/');
  await page.getByRole('link', { name: 'Entrar com Google' }).click();
  await expect(page.getByText('Você está conectado.')).toBeVisible();
  await expect(page).toHaveURL('http://127.0.0.1:5173/');
  const me = await page.request.get(`${api}/api/v1/me`);
  expect(me.status()).toBe(200);
  const body = await me.json();
  expect(Object.keys(body)).toEqual(['id']);
  expect(body.id).toMatch(/^[0-9a-f-]{36}$/);
  return body.id as string;
}

test('visitante navega sem iniciar login e usa contratos públicos reais', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();
  expect((await page.request.get(`${api}/api/v1/me`)).status()).toBe(401);
  expect((await page.request.get(`${api}/api/v1/auth/config`)).status()).toBe(200);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});

test('login real, duas identidades e dispositivos isolados, CSRF e saída', async ({ browser, request }) => {
  const first = await browser.newContext();
  const second = await browser.newContext();
  const sameUser = await browser.newContext();
  try {
    const one = await first.newPage();
    const two = await second.newPage();
    const three = await sameUser.newPage();
    await setProvider(request, 'browser-user-one');
    const firstId = await login(one);
    // Reinicia o contexto Spring real mantendo PostgreSQL e os cookies do navegador.
    expect((await request.post(`${provider}/fixture/restart`)).ok()).toBe(true);
    const recovered = await first.request.get(`${api}/api/v1/me`);
    expect(recovered.status()).toBe(200);
    expect((await recovered.json()).id).toBe(firstId);
    const cookies = await first.cookies(api);
    const session = cookies.find(cookie => cookie.name === 'SESSION');
    expect(session).toMatchObject({ httpOnly: true, sameSite: 'Lax', secure: false, path: '/' });
    await setProvider(request, 'browser-user-two');
    const secondId = await login(two);
    expect(secondId).not.toBe(firstId);
    await setProvider(request, 'browser-user-one');
    expect(await login(three)).toBe(firstId);
    expect((await first.request.post(`${api}/api/v1/auth/logout`)).status()).toBe(403);
    expect((await first.request.get(`${api}/api/v1/me`)).status()).toBe(200);
    await one.getByRole('button', { name: 'Sair', exact: true }).click();
    await expect(one.getByText('Você está estudando como visitante.')).toBeVisible();
    expect((await first.request.get(`${api}/api/v1/me`)).status()).toBe(401);
    expect((await second.request.get(`${api}/api/v1/me`)).status()).toBe(200);
    expect((await sameUser.request.get(`${api}/api/v1/me`)).status()).toBe(200);
    // Same subject after logout still resolves the same internal identity.
    expect(await login(one)).toBe(firstId);
    expect(await one.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  } finally {
    await first.close(); await second.close(); await sameUser.close();
  }
});

test('inatividade real expira sessão e cancelamento retorna falha genérica', async ({ page, request }) => {
  await setProvider(request, 'browser-expiring-user');
  await login(page);
  // Não consultar me durante a espera: consultas renovariam o tempo de atividade.
  await page.waitForTimeout(21500);
  expect((await page.request.get(`${api}/api/v1/me`)).status()).toBe(401);
  await page.getByRole('button', { name: 'Atualizar sessão' }).click();
  await expect(page.getByText('Você está estudando como visitante.')).toBeVisible();
  await setProvider(request, 'browser-cancel-user', 'cancel');
  await page.getByRole('link', { name: 'Entrar com Google' }).click();
  await expect(page.getByRole('alert')).toContainText('Não foi possível entrar com Google');
  await expect(page).not.toHaveURL(/auth=failed|code=|state=/);
  await expect(page.getByRole('link', { name: 'Acessar material de estudo' })).toBeVisible();
  expect((await page.request.get(`${api}/api/v1/me`)).status()).toBe(401);
});
