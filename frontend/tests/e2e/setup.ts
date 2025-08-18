import { expect } from '@playwright/test';

// Custom matchers and utilities for the test suite

export const TEST_USER = {
  email: 'test@example.com',
  password: 'password123',
  username: 'testuser',
  displayName: 'Test User'
};

export async function loginUser(page: any, user = TEST_USER) {
  await page.goto('/login');
  await page.fill('input[name="email"]', user.email);
  await page.fill('input[name="password"]', user.password);
  await page.click('button[type="submit"]');
  
  // Wait for successful navigation
  await expect(page).toHaveURL('/chat');
}

export async function waitForChatToLoad(page: any) {
  // Wait for essential chat elements to load
  await expect(page.locator('text=Chat Platform')).toBeVisible();
  
  // Wait for connection status to appear
  await expect(page.locator('[class*="w-3"][class*="h-3"][class*="rounded-full"]')).toBeVisible();
}

export function getViewportBreakpoints() {
  return {
    mobile: { width: 393, height: 851 },
    tablet: { width: 1024, height: 1366 },
    desktop: { width: 1280, height: 720 },
    desktopLarge: { width: 1920, height: 1080 }
  };
}

export async function takeResponsiveScreenshots(page: any, name: string) {
  const breakpoints = getViewportBreakpoints();
  
  for (const [device, viewport] of Object.entries(breakpoints)) {
    await page.setViewportSize(viewport);
    await page.waitForTimeout(500); // Allow for responsive transitions
    await page.screenshot({ 
      path: `test-results/${name}-${device}.png`,
      fullPage: true
    });
  }
}

// Utility to check if element is properly sized for touch
export async function isElementTouchFriendly(element: any, minSize = 44) {
  const box = await element.boundingBox();
  return box && box.width >= minSize && box.height >= minSize;
}

// Utility to verify responsive class usage
export async function hasResponsiveClasses(element: any, expectedClasses: string[]) {
  const className = await element.getAttribute('class');
  return expectedClasses.every(cls => className?.includes(cls));
}