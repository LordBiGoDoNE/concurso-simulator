import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e', testMatch: '**/administrative.real.spec.ts', workers: 1, timeout: 90000,
  use: { baseURL: 'http://127.0.0.1:5276', browserName: 'chromium' },
  webServer: [
    { command: process.platform === 'win32' ? '..\\backend\\gradlew.bat -p ../backend --offline --no-daemon --console=plain administrativeBrowserFixture' : '../backend/gradlew -p ../backend --offline --no-daemon --console=plain administrativeBrowserFixture',
      url: 'http://127.0.0.1:19086/api/v1/status', timeout: 180000, reuseExistingServer: false, gracefulShutdown: { signal: 'SIGTERM', timeout: 10000 } },
    { command: 'npm run dev -- --port 5276', env: { VITE_API_URL: 'http://127.0.0.1:19086' }, url: 'http://127.0.0.1:5276', reuseExistingServer: false },
  ],
});
