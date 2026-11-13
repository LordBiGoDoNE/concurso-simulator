import { test, expect, type Page } from '@playwright/test';
import { catalog, emptyPage } from '../test/question-fixtures';
import { view } from '../test/attempt-fixtures';

// focus-visual-refresh 4.x: resultado com barra e legenda, filtro, atalhos para a aula e histórico,
// nos dois temas, em celular e desktop. API simulada: uma conta conectada com um simulado de 3 questões.
const [a, b] = view.items[0].alternatives;
const item = (n: number, topicId: string, selectedAlternativeId: string | null) => ({ ...view.items[0], id: `80000000-0000-0000-0000-00000000000${n}`,
  questionId: `10000000-0000-0000-0000-00000000000${n}`, classification: { ...view.items[0].classification, topicId }, selectedAlternativeId });
const items = [item(1, 'P01', a.id), item(2, 'P07', b.id), item(3, 'P01', null)];
const started = { ...view, items };
const finished = { ...started, version: 1, finishedAt: '2026-11-13T12:00:00Z' };
const result = { total: 3, points: 1, correct: 1, wrong: 1, blank: 1, percentage: 33.33,
  items: items.map((i, n) => ({ item: i, correctAlternativeId: a.id, resolution: { text: `Resolução da questão ${n + 1}.`, resources: [] } })) };
const history = { page: 0, size: 20, items: [
  { id: view.id, roleId: view.roleId, createdAt: view.createdAt, finishedAt: finished.finishedAt, total: 3, percentage: 33.33 },
  { id: '70000000-0000-0000-0000-000000000002', roleId: 'secretario-de-escola', createdAt: '2026-11-12T09:30:00Z', finishedAt: null, total: 10, percentage: null },
] };

async function api(page: Page) {
  let practiceCreates = 0;
  await page.route('**/api/v1/**', route => {
    const url = new URL(route.request().url()); const path = url.pathname; const method = route.request().method();
    if (path.endsWith('/status')) return route.fulfill({ json: { status: 'UP' } });
    if (path.endsWith('/auth/config')) return route.fulfill({ json: { googleEnabled: false } });
    if (path.endsWith('/me')) return route.fulfill({ json: { id: '90000000-0000-0000-0000-000000000001' } });
    if (path.endsWith('/csrf')) return route.fulfill({ json: { token: 'test', headerName: 'X-CSRF-TOKEN' } });
    if (path.endsWith('/catalog')) return route.fulfill({ json: catalog });
    if (path.includes('/question-bank/questions')) return route.fulfill({ json: emptyPage });
    if (path.endsWith('/me/attempts')) return route.fulfill({ json: history });
    if (path.endsWith('/lesson-practices/availability')) return route.fulfill({ json: { topicId: url.searchParams.get('topicId'), available: 5, maximum: 5 } });
    if (path.includes('/lesson-practices') && method === 'POST') practiceCreates++;
    if (path.endsWith('/lesson-practices') || path.includes('/me/lesson-practices')) return route.fulfill({ json: { page: 0, size: 20, items: [] } });
    if (path.endsWith('/finish')) return route.fulfill({ json: { attempt: finished, result } });
    if (path.endsWith('/attempts') && method === 'POST') return route.fulfill({ status: 201, json: started });
    if (path.endsWith(`/attempts/${view.id}`)) return route.fulfill({ json: started });
    return route.fulfill({ status: 503, json: { error: 'unavailable' } });
  });
  return () => practiceCreates;
}

const contrast = (page: Page, selector: string) => page.evaluate(sel => {
  const rgb = (value: string) => (value.match(/[\d.]+/g) || []).map(Number);
  const lum = ([r, g, bl]: number[]) => [r, g, bl].map(v => v / 255).map(v => v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4).reduce((sum, v, i) => sum + v * [0.2126, 0.7152, 0.0722][i], 0);
  const background = (element: Element | null): number[] => {
    for (let node = element; node; node = node.parentElement) { const value = rgb(getComputedStyle(node).backgroundColor); if (value.length === 3 || value[3] > 0.9) return value.slice(0, 3); }
    return [255, 255, 255];
  };
  const out: string[] = [];
  for (const root of document.querySelectorAll(sel)) for (const element of [root, ...root.querySelectorAll('*')]) {
    const own = [...element.childNodes].some(node => node.nodeType === Node.TEXT_NODE && node.textContent!.trim());
    if (!own || !(element as HTMLElement).offsetParent) continue;
    const style = getComputedStyle(element); const [fg, bg] = [lum(rgb(style.color)), lum(background(element))];
    const ratio = (Math.max(fg, bg) + 0.05) / (Math.min(fg, bg) + 0.05);
    if (ratio < 4.5) out.push(`${element.tagName.toLowerCase()}.${element.className} ${ratio.toFixed(2)} "${element.textContent!.trim().slice(0, 30)}"`);
  }
  return out;
}, selector);

for (const colorScheme of ['light', 'dark'] as const) for (const width of [390, 1440]) test(`resultado e histórico em ${colorScheme}, ${width}px`, async ({ page }, info) => {
  const practiceCreates = await api(page);
  await page.emulateMedia({ colorScheme }); await page.setViewportSize({ width, height: 900 }); await page.goto('/');
  await page.getByRole('button', { name: 'Montar um simulado' }).click();
  await page.getByLabel('Quantidade de Português').fill('3'); await page.getByRole('button', { name: 'Iniciar simulado' }).click();
  await page.getByRole('button', { name: 'Concluir simulado', exact: true }).click(); await page.getByRole('button', { name: 'Confirmar conclusão' }).click();
  await expect(page.getByRole('heading', { name: 'Resultado do simulado' })).toBeFocused();
  await expect(page.getByRole('img', { name: '1 acertos, 1 erros e 1 em branco de 3 questões' })).toBeVisible();
  for (const label of ['Correta', 'Errada', 'Em branco']) await expect(page.locator('.result-outcome', { hasText: label })).toBeVisible();
  expect(await contrast(page, '.attempt-result, .history-panel')).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath(`result-${colorScheme}-${width}.png`), fullPage: true });

  await page.getByRole('button', { name: 'Erros e em branco (2)' }).click();
  await expect(page.getByRole('heading', { name: 'Revisão da questão 1' })).toHaveCount(0);
  await expect(page.getByRole('heading', { name: 'Revisão da questão 3' })).toBeVisible();
  await page.getByRole('button', { name: 'Rever a aula P07, questão 2' }).click();
  await expect(page).toHaveURL(/#\/aulas\/P07/); await expect(page.getByRole('article', { name: 'Conteúdo da aula: Concordância Verbal e Nominal' })).toBeVisible();
  await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Revisão da questão 2' })).toBeVisible();
  await page.getByRole('button', { name: 'Praticar este assunto, P01, questão 3' }).click();
  await expect(page).toHaveURL(/#\/aulas\/P01/); await expect(page.locator('details.lesson-practice-panel')).toHaveAttribute('open');
  // O foco e a rolagem ficam no painel aberto, não no título oculto da área (FVR3-L1).
  const practiceSummary = page.locator('details.lesson-practice-panel > summary');
  await expect(practiceSummary).toBeFocused(); await expect(practiceSummary).toBeInViewport();
  await expect(page.getByRole('button', { name: 'Começar prática', exact: true })).toBeVisible(); expect(practiceCreates()).toBe(0);

  await page.getByRole('navigation', { name: 'Navegação principal' }).getByRole('button', { name: 'Simulados', exact: true }).click();
  const panel = page.getByRole('region', { name: 'Histórico de simulados' });
  await expect(panel.getByText('Em andamento — sem nota parcial.')).toBeVisible(); await expect(panel.getByText('Concluído — 33,33%.')).toBeVisible();
  await panel.scrollIntoViewIfNeeded(); expect(await contrast(page, '.history-panel')).toEqual([]);
  await panel.screenshot({ path: info.outputPath(`history-${colorScheme}-${width}.png`) });
});
