import { defineConfig } from '@playwright/test';
const expansion = process.env.PRACTICE_EXPANSION === '1';
const custom = process.env.TOPIC_PRACTICE_PORTS?.split(',').map(Number); // opcional "ui,api,controle"
const [uiPort, apiPort, fixturePort] = custom?.length === 3 ? custom : expansion ? [5393, 19370, 19371] : [5371, 19160, 19161];
export default defineConfig({
  testDir: './e2e', testMatch: ['**/topic-practice.real.spec.ts', '**/new-student.spec.ts'], workers: 1, timeout: 60000,
  outputDir: expansion ? 'test-results/practice-expansion' : 'test-results/topic-practice',
  use: { baseURL: `http://127.0.0.1:${uiPort}`, trace: 'retain-on-failure' },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }, { name: 'edge', use: { browserName: 'chromium', channel: 'msedge' } }],
  webServer: [
    { command: process.platform === 'win32' ? '..\\backend\\gradlew.bat -p ../backend --offline --no-daemon --console=plain topicPracticeBrowserFixture' : '../backend/gradlew -p ../backend --offline --no-daemon --console=plain topicPracticeBrowserFixture',
      url: `http://127.0.0.1:${fixturePort}/fixture`, timeout: 180000, reuseExistingServer: false, gracefulShutdown: { signal: 'SIGTERM', timeout: 10000 } },
    { command: `npx vite --host 127.0.0.1 --port ${uiPort} --strictPort`, env: { VITE_API_URL: `http://127.0.0.1:${apiPort}` }, url: `http://127.0.0.1:${uiPort}`, reuseExistingServer: false },
  ],
});
