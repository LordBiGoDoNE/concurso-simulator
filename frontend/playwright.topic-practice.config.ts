import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e', testMatch: ['**/topic-practice.real.spec.ts', '**/new-student.spec.ts'], workers: 1, timeout: 60000,
  outputDir: 'test-results/topic-practice',
  use: { baseURL: 'http://127.0.0.1:5371', trace: 'retain-on-failure' },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }, { name: 'edge', use: { browserName: 'chromium', channel: 'msedge' } }],
  webServer: [
    { command: process.platform === 'win32' ? '..\\backend\\gradlew.bat -p ../backend --no-daemon --console=plain topicPracticeBrowserFixture' : '../backend/gradlew -p ../backend --no-daemon --console=plain topicPracticeBrowserFixture',
      url: 'http://127.0.0.1:19161/fixture', timeout: 180000, reuseExistingServer: false, gracefulShutdown: { signal: 'SIGTERM', timeout: 10000 } },
    { command: 'npx vite --host 127.0.0.1 --port 5371 --strictPort', env: { VITE_API_URL: 'http://127.0.0.1:19160' }, url: 'http://127.0.0.1:5371', reuseExistingServer: false },
  ],
});
