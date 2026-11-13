import { test, expect, type Page } from '@playwright/test';
import { catalog, emptyPage } from '../test/question-fixtures';
import { view } from '../test/attempt-fixtures';

// focus-visual-refresh, achados da revisão final: tema escuro em início/montagem/realização sem gabarito (FVR-L3),
// tema manual persistente e aplicado antes do app (FVR-L3), acesso ao mapa no topo no celular (FVR-M2)
// e anel de foco do painel de prática sem recorte (FVR-M1). API simulada, visitante conectado.
const items = [1, 2, 3].map(n => ({ ...view.items[0], id: `80000000-0000-0000-0000-00000000000${n}`, questionId: `10000000-0000-0000-0000-00000000000${n}` }));
let started = { ...view, items };

async function api(page: Page) {
  await page.route('**/api/v1/**', route => {
    const url = new URL(route.request().url()); const path = url.pathname; const method = route.request().method();
    if (path.endsWith('/status')) return route.fulfill({ json: { status: 'UP' } });
    if (path.endsWith('/auth/config')) return route.fulfill({ json: { googleEnabled: false } });
    if (path.endsWith('/me')) return route.fulfill({ json: { id: '90000000-0000-0000-0000-000000000001' } });
    if (path.endsWith('/csrf')) return route.fulfill({ json: { token: 'test', headerName: 'X-CSRF-TOKEN' } });
    if (path.endsWith('/catalog')) return route.fulfill({ json: catalog });
    if (path.includes('/question-bank/questions')) return route.fulfill({ json: emptyPage });
    if (path.endsWith('/me/attempts')) return route.fulfill({ json: { page: 0, size: 20, items: [] } });
    if (path.endsWith('/lesson-practices/availability')) return route.fulfill({ json: { topicId: url.searchParams.get('topicId'), available: 5, maximum: 5 } });
    if (path.includes('lesson-practices')) return route.fulfill({ json: { page: 0, size: 20, items: [] } });
    if (path.endsWith('/attempts') && method === 'POST') { started = { ...view, items }; return route.fulfill({ status: 201, json: started }); }
    if (path.includes(`/attempts/${view.id}`) && method === 'PUT') {
      const id = path.split('/').at(-1); const answer = route.request().postDataJSON();
      started = { ...started, version: started.version + 1, items: started.items.map(item => item.id === id ? { ...item, selectedAlternativeId: answer.alternativeId } : item) };
      return route.fulfill({ json: started });
    }
    if (path.includes(`/attempts/${view.id}`)) return route.fulfill({ json: started });
    return route.fulfill({ status: 503, json: { error: 'unavailable' } });
  });
}

// Pares texto/fundo abaixo de 4,5:1 dentro dos seletores (texto próprio visível, fundo opaco mais próximo).
const lowContrast = (page: Page, selector: string) => page.evaluate(sel => {
  const rgb = (value: string) => (value.match(/[\d.]+/g) || []).map(Number);
  const lum = ([r, g, b]: number[]) => [r, g, b].map(v => v / 255).map(v => v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4).reduce((sum, v, i) => sum + v * [0.2126, 0.7152, 0.0722][i], 0);
  const background = (element: Element | null): number[] => {
    for (let node = element; node; node = node.parentElement) { const value = rgb(getComputedStyle(node).backgroundColor); if (value.length === 3 || value[3] > 0.9) return value.slice(0, 3); }
    return [255, 255, 255];
  };
  const out: string[] = [];
  for (const root of document.querySelectorAll(sel)) for (const element of [root, ...root.querySelectorAll('*')]) {
    const own = [...element.childNodes].some(node => node.nodeType === Node.TEXT_NODE && node.textContent!.trim());
    if (!own || !(element as HTMLElement).offsetParent) continue;
    const [fg, bg] = [lum(rgb(getComputedStyle(element).color)), lum(background(element))];
    const ratio = (Math.max(fg, bg) + 0.05) / (Math.min(fg, bg) + 0.05);
    if (ratio < 4.5) out.push(`${element.tagName.toLowerCase()}.${element.className} ${ratio.toFixed(2)}`);
  }
  return out;
}, selector);

for (const width of [390, 1440]) test(`tema escuro em início, montagem e realização, sem gabarito, em ${width}px`, async ({ page }, info) => {
  await api(page); await page.emulateMedia({ colorScheme: 'dark' }); await page.setViewportSize({ width, height: 900 }); await page.goto('/');
  expect(await page.evaluate(() => getComputedStyle(document.body).backgroundColor)).not.toBe('rgb(255, 255, 255)');
  expect(await lowContrast(page, '#study-content')).toEqual([]);
  await page.getByRole('button', { name: 'Montar um simulado' }).click();
  await expect(page.getByRole('button', { name: 'Iniciar simulado' })).toBeVisible();
  expect(await lowContrast(page, '#study-content')).toEqual([]);
  await page.getByLabel('Quantidade de Português').fill('3'); await page.getByRole('button', { name: 'Iniciar simulado' }).click();
  await expect(page.getByRole('heading', { name: 'Questão 1 de 3' })).toBeFocused();
  await page.getByRole('radio').first().check(); await expect(page.getByText('Respostas confirmadas.', { exact: true })).toBeVisible();
  // A realização marca só a seleção: nenhum rótulo, classe ou texto de acerto/erro.
  const practice = page.locator('section.practice');
  await expect(practice.locator('.result-outcome, [class*="correct"], [class*="wrong"]')).toHaveCount(0);
  await expect(practice.getByText(/Correta|Errada|Resposta correta|Gabarito/)).toHaveCount(0);
  expect(await lowContrast(page, 'section.practice')).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.evaluate(() => scrollTo(0, 0)); await page.screenshot({ path: info.outputPath(`practice-dark-${width}.png`) });

  const jump = page.getByRole('button', { name: 'Ver mapa de questões' });
  if (width < 700) {
    // O atalho fica no topo, antes da questão, e leva ao mapa (que fica abaixo da questão no celular).
    const [jumpBox, cardBox] = [await jump.boundingBox(), await page.locator('.question-card').boundingBox()];
    expect(jumpBox!.y).toBeLessThan(cardBox!.y);
    await jump.click();
    const mapTitle = page.getByRole('heading', { name: 'Seu progresso' });
    await expect(mapTitle).toBeFocused(); await expect(mapTitle).toBeInViewport();
    await expect(page.getByRole('navigation', { name: 'Mapa de questões' })).toBeInViewport();
  } else await expect(jump).toBeHidden();
});

test('escolha manual de tema persiste e é aplicada antes do app carregar', async ({ page }) => {
  await api(page); await page.emulateMedia({ colorScheme: 'light' }); await page.goto('/');
  await page.getByLabel('Tema').selectOption('dark');
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
  const dark = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
  // Segura o módulo do app: o fundo escuro precisa vir do script inline do index.html, sem piscar claro.
  let release!: () => void; const held = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/src/main.tsx', async route => { await held; await route.continue(); });
  await page.reload({ waitUntil: 'domcontentloaded' });
  expect(await page.evaluate(() => [document.documentElement.dataset.theme, getComputedStyle(document.body).backgroundColor])).toEqual(['dark', dark]);
  release(); await expect(page.getByLabel('Tema')).toHaveValue('dark');
});

for (const colorScheme of ['light', 'dark'] as const) test(`anel de foco do painel de prática não é recortado em ${colorScheme}`, async ({ page }) => {
  await api(page); await page.emulateMedia({ colorScheme }); await page.setViewportSize({ width: 1440, height: 900 }); await page.goto('/#/aulas/P07');
  await expect(page.getByRole('article', { name: /Conteúdo da aula/ })).toBeVisible();
  const summary = page.locator('details.lesson-practice-panel > summary');
  await summary.scrollIntoViewIfNeeded();
  for (const open of [false, true]) {
    if (open) await summary.click();
    await page.keyboard.press('Shift'); await summary.focus();
    await expect(summary).toBeFocused();
    const clipped = await summary.evaluate(element => {
      const style = getComputedStyle(element); const grow = parseFloat(style.outlineWidth) + parseFloat(style.outlineOffset);
      if (style.outlineStyle === 'none' || parseFloat(style.outlineWidth) < 2) return ['sem anel'];
      const box = element.getBoundingClientRect(); const ring = { left: box.left - grow, right: box.right + grow, top: box.top - grow, bottom: box.bottom + grow };
      const out: string[] = [];
      for (let node = element.parentElement; node && node !== document.body; node = node.parentElement) {
        const own = getComputedStyle(node); if (own.overflowX === 'visible' && own.overflowY === 'visible') continue;
        const clip = node.getBoundingClientRect();
        if (ring.left < clip.left || ring.right > clip.right || ring.top < clip.top || ring.bottom > clip.bottom) out.push(node.className);
      }
      return out;
    });
    expect(clipped).toEqual([]);
  }
});
