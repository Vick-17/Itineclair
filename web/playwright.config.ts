import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.ITINECLAIR_E2E_BASE_URL
  ?? 'http://localhost:8080'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  timeout: 60_000,
  expect: {
    timeout: 12_000,
  },
  reporter: process.env.CI
    ? [
        ['line'],
        ['html', { open: 'never' }],
      ]
    : [['list']],
  outputDir: 'test-results',
  use: {
    baseURL,
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
