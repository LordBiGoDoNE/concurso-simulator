import { test, expect, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';

type Entry = {
  id: string;
  topic: string;
  title: string;
  mode: 'steps' | 'cycle';
  duration: number;
  poster: number;
  steps?: { time: number; caption: string }[];
};
const manifest: { animations: Entry[] } = JSON.parse(readFileSync('../assets/animacoes/manifest.json', 'utf8'));
// SVGSVGElement.getCurrentTime() is float32 in Chromium (3.4 -> 3.4000000953674316): compare times with tolerance.
const time = (page: Page, id: string) =>
  page.evaluate((ident) => {
    const svg = document.getElementById(ident)!.querySelector('svg')!;
    return { time: svg.getCurrentTime(), paused: svg.animationsPaused() };
  }, id);
const domId = (entry: Entry) => `lesson-${entry.topic}-animation-${entry.id}`;

test('o inventário de animações chega ao leitor', () => {
  expect(manifest.animations.length).toBeGreaterThan(0);
});

for (const entry of manifest.animations)
  for (const width of [360, 1280]) {
    test(`${entry.id}: controles, legenda e largura em ${width}px`, async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', (error) => errors.push(error.message));
      await page.route('**/api/v1/**', (route) => route.abort());
      await page.setViewportSize({ width, height: 900 });
      await page.goto('/#/aulas/' + entry.topic);
      const figure = page.getByRole('figure', { name: entry.title, exact: true });
      await figure.scrollIntoViewIfNeeded();
      await expect(figure.getByRole('img')).toBeVisible();
      await expect(figure.getByText(/./).last()).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)).toBe(false);
      // Labels are at least 18 units in a 480-wide drawing: keep them at 15 px or more on screen.
      const scale = await figure
        .locator('svg')
        .evaluate((svg) => svg.getBoundingClientRect().width / (svg as SVGSVGElement).viewBox.baseVal.width);
      expect(18 * scale).toBeGreaterThanOrEqual(15);
      const stage = figure.getByRole('region', { name: 'Figura animada: ' + entry.title });
      if (width < 540) {
        await stage.focus();
        await page.keyboard.press('ArrowRight');
        await page.keyboard.press('ArrowRight');
        await expect.poll(() => stage.evaluate((element) => element.scrollLeft)).toBeGreaterThan(0);
        await stage.evaluate((element) => {
          element.scrollLeft = 0;
        });
      }
      if (entry.mode === 'steps') {
        const steps = entry.steps!;
        await expect
          .poll(() => time(page, domId(entry)))
          .toEqual({ time: expect.closeTo(steps[0].time, 3), paused: true });
        await expect(figure.getByText(`Etapa 1 de ${steps.length}`)).toBeVisible();
        await expect(figure.locator('[aria-live="polite"]')).toHaveText(steps[0].caption);
        const next = figure.getByRole('button', { name: 'Próxima' });
        await next.focus();
        for (let index = 1; index < steps.length; index++) {
          await page.keyboard.press('Enter');
          await expect(figure.getByText(`Etapa ${index + 1} de ${steps.length}`)).toBeVisible();
          await expect(figure.locator('[aria-live="polite"]')).toHaveText(steps[index].caption);
          expect(await time(page, domId(entry))).toEqual({ time: expect.closeTo(steps[index].time, 3), paused: true });
        }
        await expect(next).toBeDisabled();
        await figure.getByRole('button', { name: 'Reproduzir tudo' }).click();
        await expect.poll(async () => (await time(page, domId(entry))).paused).toBe(false);
        await figure.getByRole('button', { name: 'Pausar' }).click();
        expect((await time(page, domId(entry))).paused).toBe(true);
      } else {
        await expect.poll(async () => (await time(page, domId(entry))).paused).toBe(false);
        await figure.getByRole('button', { name: 'Pausar' }).click();
        const paused = await time(page, domId(entry));
        await page.waitForTimeout(300);
        expect(await time(page, domId(entry))).toEqual(paused);
      }
      expect(errors).toEqual([]);
    });
  }

for (const entry of manifest.animations)
  test(`${entry.id}: com movimento reduzido nada se move sozinho`, async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.route('**/api/v1/**', (route) => route.abort());
    await page.goto('/#/aulas/' + entry.topic);
    const figure = page.getByRole('figure', { name: entry.title, exact: true });
    await figure.scrollIntoViewIfNeeded();
    const start = entry.mode === 'steps' ? entry.steps![0].time : entry.poster;
    await expect.poll(() => time(page, domId(entry))).toEqual({ time: expect.closeTo(start, 3), paused: true });
    await page.waitForTimeout(500);
    expect(await time(page, domId(entry))).toEqual({ time: expect.closeTo(start, 3), paused: true });
  });
