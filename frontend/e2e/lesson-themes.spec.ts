import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
const catalog: { topics: { id: string; title: string }[] } = JSON.parse(readFileSync('src/lesson-catalog.json', 'utf8'));

// focus-visual-refresh 5.4: as 51 aulas nativas nos dois temas. Mede o contraste calculado de
// cada elemento com texto do leitor (fora dos SVGs, que ficam sobre placa clara própria).
for (const colorScheme of ['light', 'dark'] as const) test(`51 aulas legíveis no tema ${colorScheme}`, async ({ page }, info) => {
  test.setTimeout(120000);
  await page.emulateMedia({ colorScheme });
  await page.route('**/api/v1/**', route => route.abort());
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/#/aulas/P01');
  const failures: string[] = [];
  for (const topic of catalog.topics) {
    await page.evaluate(id => { window.location.hash = '#/aulas/' + id; }, topic.id);
    await expect(page.getByRole('article', { name: 'Conteúdo da aula: ' + topic.title, exact: true }), topic.id).toBeVisible();
    const low = await page.evaluate(() => {
      const rgb = (value: string) => (value.match(/[\d.]+/g) || []).map(Number);
      const lum = ([r, g, b]: number[]) => [r, g, b].map(v => v / 255).map(v => v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4).reduce((sum, v, i) => sum + v * [0.2126, 0.7152, 0.0722][i], 0);
      const background = (element: Element | null): number[] => {
        for (let node = element; node; node = node.parentElement) {
          const value = rgb(getComputedStyle(node).backgroundColor);
          if (value.length === 3 || (value.length === 4 && value[3] > 0.9)) return value.slice(0, 3);
        }
        return [255, 255, 255];
      };
      const root = document.querySelector('.reader-index')!.parentElement!;
      const out: string[] = [];
      for (const element of root.querySelectorAll('*')) {
        if (element.closest('svg, math')) continue;
        const own = [...element.childNodes].some(node => node.nodeType === Node.TEXT_NODE && node.textContent!.trim());
        if (!own || !(element as HTMLElement).offsetParent) continue;
        const style = getComputedStyle(element);
        const [fg, bg] = [lum(rgb(style.color)), lum(background(element))];
        const ratio = (Math.max(fg, bg) + 0.05) / (Math.min(fg, bg) + 0.05);
        const size = parseFloat(style.fontSize), bold = Number(style.fontWeight) >= 700;
        const min = size >= 24 || (bold && size >= 18.66) ? 3 : 4.5;
        if (ratio < min) out.push(`${element.tagName.toLowerCase()}.${element.className} ${ratio.toFixed(2)} "${element.textContent!.trim().slice(0, 30)}"`);
      }
      return out;
    });
    failures.push(...low.map(item => `${topic.id}: ${item}`));
    if (['P01', 'M04', 'I05'].includes(topic.id)) {
      await page.evaluate(() => (document.querySelector('.native-lesson svg') || document.querySelector('.native-lesson'))!.scrollIntoView({ block: 'center' }));
      await page.screenshot({ path: info.outputPath(`${topic.id}-${colorScheme}.png`) });
    }
  }
  expect(failures).toEqual([]);
});
