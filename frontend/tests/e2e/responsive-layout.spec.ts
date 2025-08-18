import { test, expect } from '@playwright/test';

test.describe('Responsive Layout Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Mock authentication for testing
    await page.goto('/login');
    
    // Fill login form - adjust these credentials based on your test setup
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    
    // Wait for navigation to chat page
    await expect(page).toHaveURL('/chat');
  });

  test.describe('Desktop Layout (1280x720)', () => {
    test.use({ viewport: { width: 1280, height: 720 } });

    test('should show fixed sidebar on desktop', async ({ page }) => {
      // Check sidebar is visible and positioned correctly
      const sidebar = page.locator('[class*="w-80"][class*="lg:w-72"][class*="xl:w-80"]').first();
      await expect(sidebar).toBeVisible();
      
      // Verify sidebar is not using mobile overlay styles
      await expect(sidebar).not.toHaveClass(/fixed/);
      await expect(sidebar).toHaveClass(/lg:relative/);
      
      // Check that mobile hamburger menu is hidden
      const hamburgerButton = page.locator('button[class*="lg:hidden"]');
      await expect(hamburgerButton).not.toBeVisible();
    });

    test('should display chat header with proper alignment', async ({ page }) => {
      const chatHeader = page.locator('[class*="px-4"][class*="sm:px-6"][class*="py-3"][class*="border-b"]');
      await expect(chatHeader).toBeVisible();
      
      // Check header elements are properly aligned
      const headerContent = chatHeader.locator('.flex.items-center.justify-between');
      await expect(headerContent).toBeVisible();
    });

    test('should show search panel side-by-side when active', async ({ page }) => {
      // Open search mode
      const searchButton = page.locator('button[title*="search" i], button[aria-label*="search" i]').first();
      if (await searchButton.isVisible()) {
        await searchButton.click();
        
        // Check chat area takes half width
        const chatColumn = page.locator('[class*="lg:w-1/2"]');
        await expect(chatColumn).toBeVisible();
        
        // Check search panel is visible as side panel
        const searchPanel = page.locator('.hidden.lg\\:flex.lg\\:w-1\\/2');
        await expect(searchPanel).toBeVisible();
      }
    });
  });

  test.describe('Mobile Layout (393x851)', () => {
    test.use({ viewport: { width: 393, height: 851 } });

    test('should hide sidebar initially on mobile', async ({ page }) => {
      // Sidebar should be hidden by default (translated off-screen)
      const sidebar = page.locator('[class*="-translate-x-full"]');
      await expect(sidebar).toBeVisible(); // Element exists but translated
      
      // Mobile hamburger should be visible
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await expect(hamburgerButton).toBeVisible();
    });

    test('should show overlay sidebar when hamburger is clicked', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      // Check overlay background appears
      const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"][class*="z-40"]');
      await expect(overlay).toBeVisible();
      
      // Check sidebar slides in (translate-x-0)
      const sidebar = page.locator('[class*="translate-x-0"][class*="z-50"]');
      await expect(sidebar).toBeVisible();
    });

    test('should close sidebar when overlay is clicked', async ({ page }) => {
      // Open sidebar first
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      // Click overlay to close
      const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"]');
      await overlay.click();
      
      // Verify sidebar is hidden again
      const hiddenSidebar = page.locator('[class*="-translate-x-full"]');
      await expect(hiddenSidebar).toBeVisible();
    });

    test('should show full-screen search overlay on mobile', async ({ page }) => {
      // Open search mode
      const searchButton = page.locator('button[title*="search" i], button[aria-label*="search" i]').first();
      if (await searchButton.isVisible()) {
        await searchButton.click();
        
        // Check for full-screen mobile search overlay
        const mobileSearchOverlay = page.locator('.lg\\:hidden.fixed.inset-0.bg-white.z-50');
        await expect(mobileSearchOverlay).toBeVisible();
        
        // Verify desktop side panel is hidden on mobile
        const desktopSearchPanel = page.locator('.hidden.lg\\:flex');
        await expect(desktopSearchPanel).not.toBeVisible();
      }
    });

    test('should have proper touch-friendly button sizes', async ({ page }) => {
      // Check hamburger button size
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      const buttonBox = await hamburgerButton.boundingBox();
      
      // Button should be at least 44px (touch-friendly size)
      expect(buttonBox?.width).toBeGreaterThanOrEqual(44);
      expect(buttonBox?.height).toBeGreaterThanOrEqual(44);
    });
  });

  test.describe('Tablet Layout (1024x1366)', () => {
    test.use({ viewport: { width: 1024, height: 1366 } });

    test('should use desktop layout on tablets', async ({ page }) => {
      // On tablet size, should behave like desktop
      const sidebar = page.locator('[class*="lg:relative"]');
      await expect(sidebar).toBeVisible();
      
      // Mobile hamburger should be hidden
      const hamburgerButton = page.locator('button[class*="lg:hidden"]');
      await expect(hamburgerButton).not.toBeVisible();
    });

    test('should properly scale content on tablet', async ({ page }) => {
      // Check that content is properly sized and not too cramped
      const chatHeader = page.locator('[class*="text-lg"][class*="lg:text-xl"]');
      await expect(chatHeader).toBeVisible();
      
      // Verify responsive padding
      const mainContent = page.locator('[class*="px-4"][class*="sm:px-6"]');
      await expect(mainContent).toBeVisible();
    });
  });

  test.describe('Cross-Device Consistency', () => {
    test('should maintain consistent branding across devices', async ({ page }) => {
      // Check logo/branding elements are consistent
      const appTitle = page.locator('text=Chat Platform');
      await expect(appTitle).toBeVisible();
      
      const logoIcon = page.locator('[class*="w-8"][class*="h-8"][class*="bg-gradient-to-br"]');
      await expect(logoIcon).toBeVisible();
    });

    test('should preserve functionality across breakpoints', async ({ page }) => {
      // Test that core functionality works regardless of viewport size
      const messageInput = page.locator('textarea[placeholder*="message" i], input[placeholder*="message" i]');
      
      if (await messageInput.isVisible()) {
        await messageInput.fill('Test message');
        const sendButton = page.locator('button[type="submit"]').last();
        await expect(sendButton).toBeEnabled();
      }
    });
  });

  test.describe('Responsive Breakpoint Transitions', () => {
    test('should handle viewport resize gracefully', async ({ page }) => {
      // Start at desktop size
      await page.setViewportSize({ width: 1280, height: 720 });
      
      // Verify desktop layout
      const desktopSidebar = page.locator('[class*="lg:relative"]');
      await expect(desktopSidebar).toBeVisible();
      
      // Resize to mobile
      await page.setViewportSize({ width: 393, height: 851 });
      
      // Wait for transition and verify mobile layout
      await page.waitForTimeout(500); // Allow for transitions
      const hamburger = page.locator('button[class*="lg:hidden"]').first();
      await expect(hamburger).toBeVisible();
    });
  });
});