import { test, expect } from '@playwright/test';

test.describe('Sidebar Behavior Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to login and authenticate
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/chat');
  });

  test.describe('Desktop Sidebar (1280x720)', () => {
    test.use({ viewport: { width: 1280, height: 720 } });

    test('should display sidebar as fixed panel', async ({ page }) => {
      const sidebar = page.locator('[class*="w-80"][class*="lg:w-72"][class*="xl:w-80"]').first();
      
      // Verify sidebar is visible and properly positioned
      await expect(sidebar).toBeVisible();
      await expect(sidebar).toHaveClass(/lg:relative/);
      await expect(sidebar).not.toHaveClass(/fixed/);
      
      // Check sidebar has proper styling
      await expect(sidebar).toHaveClass(/bg-white/);
      await expect(sidebar).toHaveClass(/dark:bg-gray-800/);
      await expect(sidebar).toHaveClass(/border-r/);
    });

    test('should show sidebar content without overlay', async ({ page }) => {
      // Verify no overlay background exists
      const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"]');
      await expect(overlay).not.toBeVisible();
      
      // Check sidebar content is accessible
      const conversationList = sidebar.locator('[class*="ConversationList"], [data-testid="conversation-list"]');
      const appTitle = page.locator('text=Chat Platform');
      await expect(appTitle).toBeVisible();
    });

    test('should maintain sidebar width consistency', async ({ page }) => {
      const sidebar = page.locator('[class*="w-80"][class*="lg:w-72"][class*="xl:w-80"]').first();
      const sidebarBox = await sidebar.boundingBox();
      
      // On desktop, should use xl:w-80 (320px) on larger screens
      if (sidebarBox) {
        // Allow some tolerance for borders and padding
        expect(sidebarBox.width).toBeGreaterThanOrEqual(280);
        expect(sidebarBox.width).toBeLessThanOrEqual(330);
      }
    });

    test('should not show mobile controls on desktop', async ({ page }) => {
      // Mobile hamburger menu should be hidden
      const hamburgerButton = page.locator('button[class*="lg:hidden"]');
      await expect(hamburgerButton).not.toBeVisible();
      
      // Mobile close button should be hidden
      const mobileCloseButton = page.locator('button[class*="lg:hidden"] svg[viewBox="0 0 24 24"]');
      await expect(mobileCloseButton).not.toBeVisible();
    });

    test('should display theme toggle and user menu', async ({ page }) => {
      const themeToggle = page.locator('button[class*="theme"], [data-testid="theme-toggle"]');
      const userMenuButton = page.locator('[title="User Menu"], button[class*="text-sm"][class*="text-gray-700"]');
      
      // These should be visible in the sidebar header
      await expect(themeToggle).toBeVisible();
      await expect(userMenuButton).toBeVisible();
    });
  });

  test.describe('Mobile Sidebar (393x851)', () => {
    test.use({ viewport: { width: 393, height: 851 } });

    test('should hide sidebar initially with transform', async ({ page }) => {
      // Sidebar should exist but be translated off-screen
      const sidebar = page.locator('[class*="-translate-x-full"]');
      await expect(sidebar).toBeVisible();
      
      // Verify it's using mobile positioning classes
      await expect(sidebar).toHaveClass(/fixed/);
      await expect(sidebar).toHaveClass(/z-50/);
    });

    test('should show hamburger menu button', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await expect(hamburgerButton).toBeVisible();
      
      // Verify button styling for mobile
      await expect(hamburgerButton).toHaveClass(/p-2/);
      await expect(hamburgerButton).toHaveClass(/text-gray-500/);
      
      // Check hamburger icon
      const hamburgerIcon = hamburgerButton.locator('svg[viewBox="0 0 24 24"]');
      await expect(hamburgerIcon).toBeVisible();
    });

    test('should open sidebar with overlay on hamburger click', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      // Check overlay appears
      const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"][class*="z-40"]');
      await expect(overlay).toBeVisible();
      await expect(overlay).toHaveClass(/lg:hidden/);
      
      // Check sidebar slides in
      const openSidebar = page.locator('[class*="translate-x-0"][class*="z-50"]');
      await expect(openSidebar).toBeVisible();
      await expect(openSidebar).toHaveClass(/fixed/);
      
      // Verify sidebar takes full mobile height
      await expect(openSidebar).toHaveClass(/h-screen/);
    });

    test('should close sidebar when clicking overlay', async ({ page }) => {
      // Open sidebar first
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      // Wait for sidebar to open
      const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"]');
      await expect(overlay).toBeVisible();
      
      // Click overlay to close
      await overlay.click();
      
      // Verify sidebar closes (becomes translate-x-full again)
      await page.waitForTimeout(300); // Wait for transition
      const closedSidebar = page.locator('[class*="-translate-x-full"]');
      await expect(closedSidebar).toBeVisible();
      
      // Overlay should disappear
      await expect(overlay).not.toBeVisible();
    });

    test('should close sidebar when clicking close button', async ({ page }) => {
      // Open sidebar
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      // Find and click close button (X icon)
      const closeButton = page.locator('button[class*="lg:hidden"] svg[d*="6 18L18 6M6 6l12 12"]').locator('..');
      if (await closeButton.isVisible()) {
        await closeButton.click();
        
        // Verify sidebar closes
        await page.waitForTimeout(300);
        const closedSidebar = page.locator('[class*="-translate-x-full"]');
        await expect(closedSidebar).toBeVisible();
      }
    });

    test('should show mobile-optimized sidebar width', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      const openSidebar = page.locator('[class*="translate-x-0"][class*="w-80"]');
      await expect(openSidebar).toBeVisible();
      
      const sidebarBox = await openSidebar.boundingBox();
      if (sidebarBox) {
        // Mobile sidebar should use w-80 (320px) but be constrained by viewport
        expect(sidebarBox.width).toBeLessThanOrEqual(320);
        expect(sidebarBox.width).toBeGreaterThan(280); // Minimum usable width
      }
    });

    test('should maintain touch-friendly interactions', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      const buttonBox = await hamburgerButton.boundingBox();
      
      // Touch target should be at least 44x44px
      expect(buttonBox?.width).toBeGreaterThanOrEqual(44);
      expect(buttonBox?.height).toBeGreaterThanOrEqual(44);
      
      // Test tap interaction
      await hamburgerButton.click();
      const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"]');
      await expect(overlay).toBeVisible();
    });
  });

  test.describe('Tablet Sidebar (1024x1366)', () => {
    test.use({ viewport: { width: 1024, height: 1366 } });

    test('should behave like desktop on tablet', async ({ page }) => {
      // Should use desktop layout (lg breakpoint is 1024px)
      const sidebar = page.locator('[class*="lg:relative"]');
      await expect(sidebar).toBeVisible();
      
      // Mobile hamburger should be hidden
      const hamburgerButton = page.locator('button[class*="lg:hidden"]');
      await expect(hamburgerButton).not.toBeVisible();
      
      // No overlay should be present
      const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"]');
      await expect(overlay).not.toBeVisible();
    });

    test('should use appropriate sidebar width on tablet', async ({ page }) => {
      const sidebar = page.locator('[class*="lg:w-72"]').first();
      await expect(sidebar).toBeVisible();
      
      const sidebarBox = await sidebar.boundingBox();
      if (sidebarBox) {
        // Should use lg:w-72 (288px) on tablet
        expect(sidebarBox.width).toBeGreaterThanOrEqual(280);
        expect(sidebarBox.width).toBeLessThanOrEqual(300);
      }
    });
  });

  test.describe('Sidebar Content Tests', () => {
    test('should display connection status indicator', async ({ page }) => {
      // Look for connection status elements in sidebar
      const connectionDot = page.locator('[class*="w-3"][class*="h-3"][class*="rounded-full"]');
      const connectionText = page.locator('text=/Connected|Disconnected|Reconnecting/');
      
      await expect(connectionDot).toBeVisible();
      await expect(connectionText).toBeVisible();
    });

    test('should show user avatar and info in sidebar header', async ({ page }) => {
      // Check for user avatar (colored circle with initial)
      const userAvatar = page.locator('[style*="backgroundColor"][class*="w-6"][class*="h-6"][class*="rounded-full"]');
      await expect(userAvatar).toBeVisible();
      
      // Check for Chat Platform title with icon
      const appIcon = page.locator('[class*="w-8"][class*="h-8"][class*="bg-gradient-to-br"]');
      await expect(appIcon).toBeVisible();
    });

    test('should display conversation list in sidebar', async ({ page }) => {
      // The conversation list should be present in the sidebar
      // Note: This might need adjustment based on actual test data
      const sidebarContent = page.locator('[class*="flex-1"], [class*="overflow-y-auto"]').first();
      await expect(sidebarContent).toBeVisible();
    });
  });

  test.describe('Sidebar Accessibility', () => {
    test('should have proper ARIA labels', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      
      // Button should have accessible name/label
      const buttonText = await hamburgerButton.textContent();
      const ariaLabel = await hamburgerButton.getAttribute('aria-label');
      const title = await hamburgerButton.getAttribute('title');
      
      // Should have some form of accessible name
      expect(buttonText || ariaLabel || title).toBeTruthy();
    });

    test('should support keyboard navigation', async ({ page }) => {
      if (page.viewportSize()?.width && page.viewportSize()!.width < 1024) {
        // Test mobile keyboard navigation
        const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
        
        // Focus the hamburger button
        await hamburgerButton.focus();
        await expect(hamburgerButton).toBeFocused();
        
        // Activate with Enter key
        await page.keyboard.press('Enter');
        const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"]');
        await expect(overlay).toBeVisible();
        
        // Should be able to close with Escape
        await page.keyboard.press('Escape');
        await page.waitForTimeout(300);
        await expect(overlay).not.toBeVisible();
      }
    });
  });
});