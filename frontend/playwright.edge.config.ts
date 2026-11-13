import { defineConfig } from '@playwright/test';
import config from './playwright.config';

export default defineConfig({ ...config, testMatch: ['**/new-student.spec.ts', '**/native-lessons.spec.ts'], use: { ...config.use, channel: 'msedge' } });
