import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  testMatch: '**/smoke.spec.ts',
  retries: 0,
  reporter: 'line',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'off',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npx serve -s build -l 3000',
    url: 'http://localhost:3000',
    reuseExistingServer: false,
    timeout: 30 * 1000,
  },
});
