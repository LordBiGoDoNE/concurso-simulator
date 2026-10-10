import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/identity.real.spec.ts',
  workers: 1,
  timeout: 60000,
  use: { baseURL: 'http://127.0.0.1:5173', browserName: 'chromium' },
  webServer: [
    {
      command: '../backend/gradlew -p ../backend --no-daemon --console=plain identityBrowserFixture',
      url: 'http://127.0.0.1:18080/api/v1/status',
      timeout: 180000,
      reuseExistingServer: false,
      gracefulShutdown: { signal: 'SIGTERM', timeout: 10000 },
    },
    {
      command: 'npm run dev',
      env: { VITE_API_URL: 'http://127.0.0.1:18080' },
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: false,
    },
  ],
});
