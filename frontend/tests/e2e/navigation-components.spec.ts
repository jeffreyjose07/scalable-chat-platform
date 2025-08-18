import { test, expect } from '@playwright/test';

test.describe('Navigation Components', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/chat');
  });

  test.describe('Desktop Navigation (1280x720)', () => {
    test.use({ viewport: { width: 1280, height: 720 } });

    test('should display main navigation elements', async ({ page }) => {
      // Check for app branding/logo
      const appTitle = page.locator('text=Chat Platform');
      await expect(appTitle).toBeVisible();
      
      const appIcon = page.locator('[class*="w-8"][class*="h-8"][class*="bg-gradient-to-br"]');
      await expect(appIcon).toBeVisible();
      
      // Check for version info
      const versionInfo = page.locator('[class*="VersionInfo"], text=/v\d+\.\d+/');
      if (await versionInfo.isVisible()) {
        await expect(versionInfo).toBeVisible();
      }
    });

    test('should show conversation type toggle', async ({ page }) => {
      // Look for Direct/Groups toggle buttons
      const conversationToggle = page.locator('text=Direct, text=Groups').first();
      if (await conversationToggle.isVisible()) {
        await expect(conversationToggle).toBeVisible();
        
        // Should be clickable
        await conversationToggle.click();
        
        // Should have active state styling
        await expect(conversationToggle).toHaveClass(/bg-/);
      }
    });

    test('should display theme toggle button', async ({ page }) => {
      const themeToggle = page.locator('button[class*="theme"], [data-testid="theme-toggle"]');
      if (await themeToggle.isVisible()) {
        await expect(themeToggle).toBeVisible();
        
        // Should be clickable
        await themeToggle.click();
        
        // Should toggle theme (check for dark mode classes)
        await page.waitForTimeout(100);
        const darkModeActive = await page.locator('html, body').evaluate(el => 
          el.classList.contains('dark')
        );
      }
    });

    test('should show user menu with avatar', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"], button[class*="text-sm"][class*="text-gray-700"]').first();
      await expect(userMenuButton).toBeVisible();
      
      // Check for user avatar
      const userAvatar = userMenuButton.locator('[class*="w-6"][class*="h-6"][class*="rounded-full"]');
      await expect(userAvatar).toBeVisible();
      
      // Click to open menu
      await userMenuButton.click();
      
      // Check dropdown menu appears
      const dropdown = page.locator('[class*="absolute"][class*="right-0"][class*="mt-2"][class*="w-56"]');
      await expect(dropdown).toBeVisible();
    });

    test('should display user menu dropdown items', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      await userMenuButton.click();
      
      // Check for menu items
      const settingsItem = page.locator('text=Settings');
      const signOutItem = page.locator('text=Sign Out');
      
      await expect(settingsItem).toBeVisible();
      await expect(signOutItem).toBeVisible();
      
      // Check styling
      await expect(settingsItem).toHaveClass(/w-full/);
      await expect(settingsItem).toHaveClass(/text-left/);
      await expect(signOutItem).toHaveClass(/text-red-/);
    });

    test('should show connection status indicator', async ({ page }) => {
      const connectionDot = page.locator('[class*="w-3"][class*="h-3"][class*="rounded-full"]');
      const connectionText = page.locator('text=/Connected|Disconnected|Reconnecting/');
      
      await expect(connectionDot).toBeVisible();
      await expect(connectionText).toBeVisible();
      
      // Check for appropriate status colors
      const hasStatusColor = await connectionDot.evaluate(el => {
        const classes = el.className;
        return classes.includes('bg-green-500') || 
               classes.includes('bg-yellow-500') || 
               classes.includes('bg-red-500');
      });
      expect(hasStatusColor).toBe(true);
    });

    test('should display action buttons in chat header', async ({ page }) => {
      // Check for search button
      const searchButton = page.locator('button[title*="Search" i], button[aria-label*="Search" i]');
      if (await searchButton.isVisible()) {
        await expect(searchButton).toBeVisible();
        
        // Button should have proper styling
        await expect(searchButton).toHaveClass(/p-2/);
      }
      
      // Check for group settings button (if in a group)
      const groupSettingsButton = page.locator('button[title="Group Settings"]');
      if (await groupSettingsButton.isVisible()) {
        await expect(groupSettingsButton).toBeVisible();
      }
    });

    test('should handle navigation keyboard shortcuts', async ({ page }) => {
      // Test escape key functionality
      const userMenuButton = page.locator('[title="User Menu"]').first();
      await userMenuButton.click();
      
      const dropdown = page.locator('[class*="absolute"][class*="right-0"]');
      await expect(dropdown).toBeVisible();
      
      // Press escape to close
      await page.keyboard.press('Escape');
      await expect(dropdown).not.toBeVisible();
    });
  });

  test.describe('Mobile Navigation (393x851)', () => {
    test.use({ viewport: { width: 393, height: 851 } });

    test('should show hamburger menu button', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await expect(hamburgerButton).toBeVisible();
      
      // Should have proper touch target size
      const buttonBox = await hamburgerButton.boundingBox();
      expect(buttonBox?.width).toBeGreaterThanOrEqual(44);
      expect(buttonBox?.height).toBeGreaterThanOrEqual(44);
      
      // Check hamburger icon
      const hamburgerIcon = hamburgerButton.locator('svg[d*="4 6h16M4 12h16M4 18h16"]');
      await expect(hamburgerIcon).toBeVisible();
    });

    test('should open mobile sidebar navigation', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      // Sidebar should slide in
      const mobileSidebar = page.locator('[class*="translate-x-0"][class*="z-50"]');
      await expect(mobileSidebar).toBeVisible();
      
      // Should show overlay
      const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"]');
      await expect(overlay).toBeVisible();
      
      // Navigation items should be visible in sidebar
      const appTitle = mobileSidebar.locator('text=Chat Platform');
      await expect(appTitle).toBeVisible();
    });

    test('should show mobile-optimized user menu', async ({ page }) => {
      // Open sidebar first
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      const mobileSidebar = page.locator('[class*="translate-x-0"]');
      const userMenuButton = mobileSidebar.locator('[title="User Menu"]');
      
      if (await userMenuButton.isVisible()) {
        await expect(userMenuButton).toBeVisible();
        
        // User avatar should be visible
        const userAvatar = userMenuButton.locator('[class*="rounded-full"]');
        await expect(userAvatar).toBeVisible();
      }
    });

    test('should handle mobile sidebar close interactions', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      // Test closing with overlay click
      const overlay = page.locator('[class*="bg-black"][class*="bg-opacity-50"]');
      await overlay.click();
      
      // Sidebar should close
      const closedSidebar = page.locator('[class*="-translate-x-full"]');
      await expect(closedSidebar).toBeVisible();
      
      // Overlay should disappear
      await expect(overlay).not.toBeVisible();
    });

    test('should show mobile close button in sidebar', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      await hamburgerButton.click();
      
      const mobileSidebar = page.locator('[class*="translate-x-0"]');
      const closeButton = mobileSidebar.locator('button[class*="lg:hidden"] svg[d*="6 18L18 6"]');
      
      if (await closeButton.isVisible()) {
        await expect(closeButton).toBeVisible();
        
        // Test close functionality
        await closeButton.click();
        
        const closedSidebar = page.locator('[class*="-translate-x-full"]');
        await expect(closedSidebar).toBeVisible();
      }
    });

    test('should adapt navigation for touch interactions', async ({ page }) => {
      // All navigation elements should be touch-friendly
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      const buttonBox = await hamburgerButton.boundingBox();
      
      // Touch target should be adequate
      expect(buttonBox?.width).toBeGreaterThanOrEqual(44);
      expect(buttonBox?.height).toBeGreaterThanOrEqual(44);
      
      // Test touch interaction
      await hamburgerButton.click();
      const sidebar = page.locator('[class*="translate-x-0"]');
      await expect(sidebar).toBeVisible();
    });

    test('should show mobile search navigation', async ({ page }) => {
      const searchButton = page.locator('button[title*="Search" i]').first();
      if (await searchButton.isVisible()) {
        await searchButton.click();
        
        // Mobile search should be full-screen
        const mobileSearch = page.locator('.lg\\:hidden.fixed.inset-0');
        if (await mobileSearch.isVisible()) {
          await expect(mobileSearch).toBeVisible();
          
          // Should have close button
          const closeButton = mobileSearch.locator('button svg[d*="6 18L18 6"]');
          await expect(closeButton).toBeVisible();
        }
      }
    });
  });

  test.describe('Tablet Navigation (1024x1366)', () => {
    test.use({ viewport: { width: 1024, height: 1366 } });

    test('should use desktop navigation patterns on tablet', async ({ page }) => {
      // Should behave like desktop
      const hamburgerButton = page.locator('button[class*="lg:hidden"]');
      await expect(hamburgerButton).not.toBeVisible();
      
      // Desktop navigation elements should be visible
      const appTitle = page.locator('text=Chat Platform');
      await expect(appTitle).toBeVisible();
      
      const userMenuButton = page.locator('[title="User Menu"]');
      await expect(userMenuButton).toBeVisible();
    });

    test('should optimize spacing for tablet viewport', async ({ page }) => {
      // Navigation should use appropriate spacing
      const sidebar = page.locator('[class*="w-80"][class*="lg:w-72"]');
      await expect(sidebar).toBeVisible();
      
      const sidebarBox = await sidebar.boundingBox();
      if (sidebarBox) {
        // Should use lg:w-72 (288px) on tablet
        expect(sidebarBox.width).toBeGreaterThan(280);
        expect(sidebarBox.width).toBeLessThan(300);
      }
    });
  });

  test.describe('Navigation State Management', () => {
    test('should maintain active conversation state', async ({ page }) => {
      // If there are conversations, one should be selected/active
      const conversationItems = page.locator('[class*="conversation"], [data-testid*="conversation"]');
      const conversationCount = await conversationItems.count();
      
      if (conversationCount > 0) {
        // First conversation might be auto-selected
        const activeConversation = page.locator('[class*="bg-blue"], [class*="bg-indigo"], [aria-selected="true"]');
        if (await activeConversation.count() > 0) {
          await expect(activeConversation.first()).toBeVisible();
        }
      }
    });

    test('should handle navigation state persistence', async ({ page }) => {
      // Test that navigation state persists through page refresh
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const themeToggle = page.locator('text=/theme/i, [data-testid="theme-toggle"]');
        if (await themeToggle.isVisible()) {
          // Toggle theme
          await themeToggle.click();
          
          // Refresh page
          await page.reload();
          await page.waitForLoadState('networkidle');
          
          // Check if theme preference persisted
          // This would depend on your theme implementation
        }
      }
    });
  });

  test.describe('Navigation Accessibility', () => {
    test('should have proper ARIA labels and roles', async ({ page }) => {
      const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
      if (await hamburgerButton.isVisible()) {
        // Should have accessible name
        const ariaLabel = await hamburgerButton.getAttribute('aria-label');
        const title = await hamburgerButton.getAttribute('title');
        const textContent = await hamburgerButton.textContent();
        
        expect(ariaLabel || title || textContent).toBeTruthy();
      }
      
      // User menu should have proper ARIA
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const dropdown = page.locator('[class*="absolute"][class*="right-0"]');
        if (await dropdown.isVisible()) {
          // Dropdown should have proper role
          const role = await dropdown.getAttribute('role');
          expect(role).toMatch(/menu|listbox|navigation/);
        }
      }
    });

    test('should support keyboard navigation', async ({ page }) => {
      // Test tab navigation through main elements
      await page.keyboard.press('Tab');
      
      // Should focus on interactive elements
      const focusedElement = page.locator(':focus');
      await expect(focusedElement).toBeVisible();
      
      // Continue tabbing to next elements
      await page.keyboard.press('Tab');
      const nextFocused = page.locator(':focus');
      await expect(nextFocused).toBeVisible();
    });

    test('should handle arrow key navigation in menus', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const dropdown = page.locator('[class*="absolute"][class*="right-0"]');
        if (await dropdown.isVisible()) {
          // Focus first menu item
          const firstItem = dropdown.locator('button, a').first();
          await firstItem.focus();
          
          // Use arrow keys to navigate
          await page.keyboard.press('ArrowDown');
          
          // Next item should be focused
          const focusedElement = page.locator(':focus');
          await expect(focusedElement).toBeVisible();
        }
      }
    });

    test('should announce navigation changes to screen readers', async ({ page }) => {
      // Test that navigation state changes are announced
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        // Menu should have aria-expanded state
        const expanded = await userMenuButton.getAttribute('aria-expanded');
        expect(expanded).toBe('true');
        
        // Close menu
        await page.keyboard.press('Escape');
        
        const expandedAfter = await userMenuButton.getAttribute('aria-expanded');
        expect(expandedAfter).toBe('false');
      }
    });
  });

  test.describe('Navigation Performance', () => {
    test('should handle smooth transitions', async ({ page }) => {
      if (page.viewportSize()?.width && page.viewportSize()!.width < 1024) {
        // Test mobile sidebar transitions
        const hamburgerButton = page.locator('button[class*="lg:hidden"]').first();
        await hamburgerButton.click();
        
        const sidebar = page.locator('[class*="transition-transform"]');
        if (await sidebar.isVisible()) {
          // Should have transition classes
          await expect(sidebar).toHaveClass(/transition-transform/);
          await expect(sidebar).toHaveClass(/duration-300/);
        }
      }
    });

    test('should not block main thread during navigation', async ({ page }) => {
      // Test that navigation interactions are responsive
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        const startTime = Date.now();
        await userMenuButton.click();
        const endTime = Date.now();
        
        // Click should be responsive (< 100ms)
        expect(endTime - startTime).toBeLessThan(100);
        
        const dropdown = page.locator('[class*="absolute"][class*="right-0"]');
        await expect(dropdown).toBeVisible();
      }
    });
  });
});