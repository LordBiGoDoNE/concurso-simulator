import { defineConfig } from '@playwright/test';
import base from './playwright.identity.config';

const servers = Array.isArray(base.webServer) ? base.webServer : [base.webServer!];
export default defineConfig({
  ...base,
  testMatch: ['**/parameterized.real.spec.ts'],
  webServer: servers.map((server, index) => index === 0
    ? { ...server, env: { ...server.env, PARAMETERIZED_BROWSER_FIXTURE: 'true' } } : server),
});
