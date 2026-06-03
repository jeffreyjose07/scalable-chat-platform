import { test, expect } from '@playwright/test';

test('app loads without runtime JavaScript errors', async ({ page }) => {
  const runtimeErrors: string[] = [];

  // pageerror fires for uncaught exceptions — this is what React's version
  // mismatch (error #527) and other startup crashes become.
  page.on('pageerror', (err) => runtimeErrors.push(err.message));

  await page.goto('/');

  // React must have mounted and rendered something into #root.
  // An empty root means the app never initialized (blank page scenario).
  await expect(page.locator('#root')).not.toBeEmpty({ timeout: 15_000 });

  if (runtimeErrors.length > 0) {
    throw new Error(
      `Uncaught JS errors detected on load:\n${runtimeErrors.join('\n')}`
    );
  }
});
