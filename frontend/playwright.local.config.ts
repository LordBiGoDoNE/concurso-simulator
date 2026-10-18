import { defineConfig } from '@playwright/test';

if (!process.env.LOCAL_APP_FIXTURE || !process.env.LOCAL_APP_ORIGIN) throw new Error('Use scripts/check_local_browser.py');
export default defineConfig({
  testDir: './e2e', testMatch: '**/local-delivery.real.spec.ts', workers: 1, timeout: 60000,
  outputDir: '../backend/build/local-app/browser-results',
  use: { baseURL: process.env.LOCAL_APP_ORIGIN, browserName: 'chromium', trace: 'off' },
  // No webServer: this suite must use the compiled SPA/proxy/JAR/PG containers.
});
