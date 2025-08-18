import { test, expect } from '@playwright/test';

test.describe('Chat Interface Responsiveness', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/chat');
  });

  test.describe('Desktop Chat Interface (1280x720)', () => {
    test.use({ viewport: { width: 1280, height: 720 } });

    test('should display chat header with proper spacing', async ({ page }) => {
      const chatHeader = page.locator('[class*="px-4"][class*="sm:px-6"][class*="py-3"][class*="border-b"]');
      await expect(chatHeader).toBeVisible();
      
      // Check header uses desktop padding (sm:px-6)
      await expect(chatHeader).toHaveClass(/sm:px-6/);
      
      // Verify header content alignment
      const headerFlex = chatHeader.locator('.flex.items-center.justify-between');
      await expect(headerFlex).toBeVisible();
      
      // Check for user avatar in header
      const conversationAvatar = chatHeader.locator('[class*="w-10"][class*="h-10"][class*="rounded-full"]');
      await expect(conversationAvatar).toBeVisible();
    });

    test('should show message list with proper scrolling', async ({ page }) => {
      const messageContainer = page.locator('[class*="flex-1"][class*="overflow-y-auto"][class*="px-4"][class*="sm:px-6"]');
      await expect(messageContainer).toBeVisible();
      
      // Check desktop padding
      await expect(messageContainer).toHaveClass(/sm:px-6/);
      
      // Verify smooth scrolling is enabled
      const scrollBehavior = await messageContainer.evaluate(el => 
        getComputedStyle(el).scrollBehavior
      );
      expect(scrollBehavior).toBe('smooth');
    });

    test('should display message input with proper sizing', async ({ page }) => {
      const messageInput = page.locator('textarea, input[type="text"]').last();
      if (await messageInput.isVisible()) {
        // Check input is properly sized for desktop
        const inputBox = await messageInput.boundingBox();
        expect(inputBox?.height).toBeGreaterThan(40); // Should be comfortable height
        
        // Verify input container styling
        const inputContainer = messageInput.locator('..');
        await expect(inputContainer).toHaveClass(/border-t/);
        await expect(inputContainer).toHaveClass(/bg-white/);
      }
    });

    test('should show search panel side-by-side when active', async ({ page }) => {
      // Try to activate search mode
      const searchButton = page.locator('button[title*="Search" i], button[aria-label*="Search" i]').first();
      if (await searchButton.isVisible()) {
        await searchButton.click();
        
        // Chat area should resize to half width
        const chatColumn = page.locator('[class*="lg:w-1/2"]');
        await expect(chatColumn).toBeVisible();
        
        // Search panel should be visible on desktop
        const desktopSearchPanel = page.locator('.hidden.lg\\:flex.lg\\:w-1\\/2');
        await expect(desktopSearchPanel).toBeVisible();
        
        // Verify border between panels
        await expect(desktopSearchPanel).toHaveClass(/border-l/);
      }
    });

    test('should maintain proper aspect ratios for avatars', async ({ page }) => {
      // Check conversation avatar in header
      const headerAvatar = page.locator('[class*="w-10"][class*="h-10"][class*="rounded-full"]').first();
      if (await headerAvatar.isVisible()) {
        const avatarBox = await headerAvatar.boundingBox();
        expect(avatarBox?.width).toBe(avatarBox?.height); // Should be square
      }
    });
  });

  test.describe('Mobile Chat Interface (393x851)', () => {
    test.use({ viewport: { width: 393, height: 851 } });

    test('should use mobile-optimized header spacing', async ({ page }) => {
      const chatHeader = page.locator('[class*="px-4"][class*="sm:px-6"]');
      await expect(chatHeader).toBeVisible();
      
      // Should use base px-4 on mobile, not sm:px-6
      const computedPadding = await chatHeader.evaluate(el => 
        getComputedStyle(el).paddingLeft
      );
      expect(parseInt(computedPadding)).toBeLessThan(24); // Less than 1.5rem (sm:px-6)
    });

    test('should show hamburger button in chat header', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await expect(hamburgerButton).toBeVisible();
      
      // Check it's positioned in the header
      const chatHeader = page.locator('[class*="px-4"][class*="sm:px-6"][class*="py-3"]');
      const headerHamburger = chatHeader.locator('button[class*="lg:hidden"]');
      await expect(headerHamburger).toBeVisible();
    });

    test('should display full-width message container', async ({ page }) => {
      const messageContainer = page.locator('[class*="flex-1"][class*="overflow-y-auto"]');
      await expect(messageContainer).toBeVisible();
      
      // Should take full available width
      const containerBox = await messageContainer.boundingBox();
      const viewportWidth = page.viewportSize()?.width || 0;
      
      if (containerBox) {
        // Allow some tolerance for padding/borders
        expect(containerBox.width).toBeGreaterThan(viewportWidth * 0.8);
      }
    });

    test('should show full-screen search overlay when active', async ({ page }) => {
      const searchButton = page.locator('button[title*="Search" i], button[aria-label*="Search" i]').first();
      if (await searchButton.isVisible()) {
        await searchButton.click();
        
        // Mobile search should be full-screen overlay
        const mobileSearchOverlay = page.locator('.lg\\:hidden.fixed.inset-0.bg-white.z-50');
        await expect(mobileSearchOverlay).toBeVisible();
        
        // Desktop side panel should be hidden
        const desktopSearchPanel = page.locator('.hidden.lg\\:flex');
        await expect(desktopSearchPanel).not.toBeVisible();
        
        // Should have close button in search header
        const searchCloseButton = mobileSearchOverlay.locator('button svg[d*="6 18L18 6"]');
        await expect(searchCloseButton).toBeVisible();
      }
    });

    test('should have touch-friendly message input', async ({ page }) => {
      const messageInput = page.locator('textarea, input[type="text"]').last();
      if (await messageInput.isVisible()) {
        const inputBox = await messageInput.boundingBox();
        
        // Input should be large enough for touch interaction
        expect(inputBox?.height).toBeGreaterThan(44);
        
        // Check for proper mobile styling
        await expect(messageInput).toHaveClass(/rounded/);
      }
    });

    test('should handle virtual keyboard properly', async ({ page }) => {
      const messageInput = page.locator('textarea, input[type="text"]').last();
      if (await messageInput.isVisible()) {
        // Focus input (simulates virtual keyboard)
        await messageInput.focus();
        
        // Input should remain visible and accessible
        await expect(messageInput).toBeVisible();
        
        // The input container should be properly positioned
        const inputContainer = page.locator('[class*="flex-shrink-0"][class*="border-t"]');
        await expect(inputContainer).toBeVisible();
      }
    });
  });

  test.describe('Tablet Chat Interface (1024x1366)', () => {
    test.use({ viewport: { width: 1024, height: 1366 } });

    test('should use desktop layout patterns on tablet', async ({ page }) => {
      // Should behave like desktop at 1024px width
      const chatHeader = page.locator('[class*="sm:px-6"]');
      await expect(chatHeader).toHaveClass(/sm:px-6/);
      
      // No hamburger menu on tablet
      const hamburgerButton = page.locator('button[class*="lg:hidden"]');
      await expect(hamburgerButton).not.toBeVisible();
    });

    test('should optimize content density for tablet', async ({ page }) => {
      // Message container should use appropriate spacing
      const messageContainer = page.locator('[class*="px-4"][class*="sm:px-6"][class*="py-4"]');
      await expect(messageContainer).toBeVisible();
      
      // Check that content is well-spaced for tablet viewing
      const contentBox = await messageContainer.boundingBox();
      expect(contentBox?.width).toBeGreaterThan(800); // Should use most of tablet width
    });
  });

  test.describe('Chat Message Rendering', () => {
    test('should display messages with proper responsive spacing', async ({ page }) => {
      // Look for message elements (this will depend on your actual message structure)
      const messageElements = page.locator('[class*="message"], [data-testid*="message"]');
      
      if (await messageElements.count() > 0) {
        const firstMessage = messageElements.first();
        await expect(firstMessage).toBeVisible();
        
        // Check message has proper spacing
        const messageBox = await firstMessage.boundingBox();
        expect(messageBox?.height).toBeGreaterThan(20); // Minimum readable height
      }
    });

    test('should handle long messages appropriately', async ({ page }) => {
      // This test would need actual message content or mock data
      // For now, we'll test the container behavior
      const messageList = page.locator('[class*="overflow-y-auto"]').first();
      await expect(messageList).toBeVisible();
      
      // Should have word-wrap enabled
      const overflowWrap = await messageList.evaluate(el => 
        getComputedStyle(el).overflowWrap
      );
      expect(overflowWrap).toMatch(/break-word|anywhere/);
    });
  });

  test.describe('Connection Status Display', () => {
    test('should show connection status indicator', async ({ page }) => {
      const connectionDot = page.locator('[class*="w-3"][class*="h-3"][class*="rounded-full"]');
      const connectionText = page.locator('text=/Connected|Disconnected|Reconnecting/');
      
      await expect(connectionDot).toBeVisible();
      await expect(connectionText).toBeVisible();
    });

    test('should display appropriate status colors', async ({ page }) => {
      const connectionDot = page.locator('[class*="w-3"][class*="h-3"][class*="rounded-full"]');
      
      if (await connectionDot.isVisible()) {
        // Should have one of the status color classes
        const hasStatusColor = await connectionDot.evaluate(el => {
          const classes = el.className;
          return classes.includes('bg-green-500') || 
                 classes.includes('bg-yellow-500') || 
                 classes.includes('bg-red-500');
        });
        expect(hasStatusColor).toBe(true);
      }
    });
  });

  test.describe('Loading States', () => {
    test('should show loading indicators when appropriate', async ({ page }) => {
      // Look for loading indicators in the message area
      const loadingIndicators = page.locator('[class*="animate-pulse"], [class*="animate-spin"], text=/Loading|loading/');
      
      // If loading indicators are present, they should be visible
      const indicatorCount = await loadingIndicators.count();
      if (indicatorCount > 0) {
        await expect(loadingIndicators.first()).toBeVisible();
      }
    });

    test('should handle empty state appropriately', async ({ page }) => {
      // Check for empty state messaging or placeholders
      const messageContainer = page.locator('[class*="overflow-y-auto"]').first();
      await expect(messageContainer).toBeVisible();
      
      // Container should be accessible even when empty
      const containerBox = await messageContainer.boundingBox();
      expect(containerBox?.height).toBeGreaterThan(100);
    });
  });

  test.describe('Performance Considerations', () => {
    test('should enable smooth scrolling for message container', async ({ page }) => {
      const messageContainer = page.locator('[class*="overflow-y-auto"]').first();
      
      const styles = await messageContainer.evaluate(el => ({
        scrollBehavior: getComputedStyle(el).scrollBehavior,
        webkitOverflowScrolling: getComputedStyle(el).webkitOverflowScrolling
      }));
      
      expect(styles.scrollBehavior).toBe('smooth');
      expect(styles.webkitOverflowScrolling).toBe('touch');
    });

    test('should use appropriate backdrop blur for headers', async ({ page }) => {
      const header = page.locator('[class*="backdrop-blur-sm"]');
      if (await header.isVisible()) {
        const backdropFilter = await header.evaluate(el => 
          getComputedStyle(el).backdropFilter
        );
        expect(backdropFilter).toContain('blur');
      }
    });
  });
});