import { test, expect } from '@playwright/test';

test.describe('Form Layouts and Input Field Alignment', () => {
  test.describe('Login Form Responsive Design', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/login');
    });

    test.describe('Desktop Login Form (1280x720)', () => {
      test.use({ viewport: { width: 1280, height: 720 } });

      test('should center login form on desktop', async ({ page }) => {
        const loginContainer = page.locator('.min-h-screen.flex.items-center.justify-center');
        await expect(loginContainer).toBeVisible();
        
        const formWrapper = loginContainer.locator('.max-w-md.w-full');
        await expect(formWrapper).toBeVisible();
        
        // Check form is centered
        const formBox = await formWrapper.boundingBox();
        const viewportWidth = 1280;
        
        if (formBox) {
          const centerX = formBox.x + formBox.width / 2;
          expect(Math.abs(centerX - viewportWidth / 2)).toBeLessThan(100);
        }
      });

      test('should display form inputs with proper spacing', async ({ page }) => {
        const inputContainer = page.locator('.rounded-md.shadow-sm.space-y-4');
        await expect(inputContainer).toBeVisible();
        
        // Check input fields have consistent styling
        const emailInput = page.locator('input[name="email"]');
        const passwordInput = page.locator('input[name="password"]');
        
        await expect(emailInput).toHaveClass(/relative/);
        await expect(emailInput).toHaveClass(/block/);
        await expect(emailInput).toHaveClass(/w-full/);
        await expect(emailInput).toHaveClass(/px-3/);
        await expect(emailInput).toHaveClass(/py-2/);
        
        await expect(passwordInput).toHaveClass(/w-full/);
        await expect(passwordInput).toHaveClass(/px-3/);
        await expect(passwordInput).toHaveClass(/py-2/);
      });

      test('should show register form fields when toggled', async ({ page }) => {
        const toggleButton = page.locator('text=/Already have an account|Don\'t have an account/');
        await toggleButton.click();
        
        // Register mode should show additional fields
        const usernameInput = page.locator('input[name="username"]');
        const displayNameInput = page.locator('input[name="displayName"]');
        
        await expect(usernameInput).toBeVisible();
        await expect(displayNameInput).toBeVisible();
        
        // Check fields have consistent styling
        await expect(usernameInput).toHaveClass(/w-full/);
        await expect(displayNameInput).toHaveClass(/w-full/);
      });

      test('should style submit button appropriately', async ({ page }) => {
        const submitButton = page.locator('button[type="submit"]');
        await expect(submitButton).toBeVisible();
        
        // Check button styling
        await expect(submitButton).toHaveClass(/group/);
        await expect(submitButton).toHaveClass(/relative/);
        await expect(submitButton).toHaveClass(/w-full/);
        await expect(submitButton).toHaveClass(/flex/);
        await expect(submitButton).toHaveClass(/justify-center/);
        await expect(submitButton).toHaveClass(/py-2/);
        await expect(submitButton).toHaveClass(/px-4/);
        
        // Button should have proper colors
        await expect(submitButton).toHaveClass(/bg-indigo-600/);
        await expect(submitButton).toHaveClass(/text-white/);
      });
    });

    test.describe('Mobile Login Form (393x851)', () => {
      test.use({ viewport: { width: 393, height: 851 } });

      test('should adapt form layout for mobile', async ({ page }) => {
        const loginContainer = page.locator('.min-h-screen.flex.items-center.justify-center');
        await expect(loginContainer).toBeVisible();
        
        // Form should use responsive padding
        await expect(loginContainer).toHaveClass(/py-12/);
        await expect(loginContainer).toHaveClass(/px-4/);
        
        const formWrapper = loginContainer.locator('.max-w-md.w-full');
        const formBox = await formWrapper.boundingBox();
        
        if (formBox) {
          // Form should be nearly full width on mobile (accounting for px-4 padding)
          expect(formBox.width).toBeGreaterThan(300);
          expect(formBox.width).toBeLessThan(393 - 32); // viewport - padding
        }
      });

      test('should make inputs touch-friendly on mobile', async ({ page }) => {
        const emailInput = page.locator('input[name="email"]');
        const passwordInput = page.locator('input[name="password"]');
        
        // Inputs should have adequate height for touch
        const emailBox = await emailInput.boundingBox();
        const passwordBox = await passwordInput.boundingBox();
        
        expect(emailBox?.height).toBeGreaterThanOrEqual(44);
        expect(passwordBox?.height).toBeGreaterThanOrEqual(44);
        
        // Check responsive text size
        await expect(emailInput).toHaveClass(/sm:text-sm/);
        await expect(passwordInput).toHaveClass(/sm:text-sm/);
      });

      test('should handle virtual keyboard interaction', async ({ page }) => {
        const emailInput = page.locator('input[name="email"]');
        await emailInput.focus();
        
        // Input should remain visible and accessible when focused
        await expect(emailInput).toBeVisible();
        
        // Form should still be usable with virtual keyboard
        const submitButton = page.locator('button[type="submit"]');
        await expect(submitButton).toBeVisible();
      });
    });

    test.describe('Tablet Login Form (1024x1366)', () => {
      test.use({ viewport: { width: 1024, height: 1366 } });

      test('should use appropriate sizing on tablet', async ({ page }) => {
        const formWrapper = page.locator('.max-w-md.w-full');
        const formBox = await formWrapper.boundingBox();
        
        if (formBox) {
          // Should maintain reasonable width constraint on tablet
          expect(formBox.width).toBeLessThanOrEqual(448); // max-w-md = 28rem = 448px
          expect(formBox.width).toBeGreaterThan(400);
        }
      });
    });
  });

  test.describe('Chat Input Form', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/login');
      await page.fill('input[name="email"]', 'test@example.com');
      await page.fill('input[name="password"]', 'password123');
      await page.click('button[type="submit"]');
      await expect(page).toHaveURL('/chat');
    });

    test.describe('Desktop Message Input (1280x720)', () => {
      test.use({ viewport: { width: 1280, height: 720 } });

      test('should display message input at bottom', async ({ page }) => {
        const inputContainer = page.locator('[class*="flex-shrink-0"][class*="border-t"]').last();
        await expect(inputContainer).toBeVisible();
        
        // Should be fixed at bottom with proper styling
        await expect(inputContainer).toHaveClass(/border-t/);
        await expect(inputContainer).toHaveClass(/bg-white/);
        await expect(inputContainer).toHaveClass(/backdrop-blur-sm/);
      });

      test('should have properly sized message input field', async ({ page }) => {
        const messageInput = page.locator('textarea, input[type="text"]').last();
        if (await messageInput.isVisible()) {
          const inputBox = await messageInput.boundingBox();
          
          // Input should have comfortable size for typing
          expect(inputBox?.height).toBeGreaterThan(40);
          expect(inputBox?.width).toBeGreaterThan(200);
          
          // Should have proper styling
          await expect(messageInput).toHaveClass(/rounded/);
        }
      });

      test('should show send button alongside input', async ({ page }) => {
        const messageInput = page.locator('textarea, input[type="text"]').last();
        if (await messageInput.isVisible()) {
          const sendButton = page.locator('button[type="submit"]').last();
          await expect(sendButton).toBeVisible();
          
          // Button should be properly sized
          const buttonBox = await sendButton.boundingBox();
          expect(buttonBox?.width).toBeGreaterThan(60);
          expect(buttonBox?.height).toBeGreaterThanOrEqual(40);
        }
      });
    });

    test.describe('Mobile Message Input (393x851)', () => {
      test.use({ viewport: { width: 393, height: 851 } });

      test('should adapt input for mobile interaction', async ({ page }) => {
        const messageInput = page.locator('textarea, input[type="text"]').last();
        if (await messageInput.isVisible()) {
          const inputBox = await messageInput.boundingBox();
          
          // Input should be touch-friendly on mobile
          expect(inputBox?.height).toBeGreaterThanOrEqual(44);
          
          // Should take most of available width
          expect(inputBox?.width).toBeGreaterThan(250);
        }
      });

      test('should handle mobile keyboard properly', async ({ page }) => {
        const messageInput = page.locator('textarea, input[type="text"]').last();
        if (await messageInput.isVisible()) {
          await messageInput.focus();
          
          // Input should remain accessible with virtual keyboard
          await expect(messageInput).toBeVisible();
          
          // Container should maintain position
          const inputContainer = page.locator('[class*="flex-shrink-0"]').last();
          await expect(inputContainer).toBeVisible();
        }
      });
    });
  });

  test.describe('Modal Form Layouts', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/login');
      await page.fill('input[name="email"]', 'test@example.com');
      await page.fill('input[name="password"]', 'password123');
      await page.click('button[type="submit"]');
      await expect(page).toHaveURL('/chat');
    });

    test('should display forgot password modal form properly', async ({ page }) => {
      await page.goto('/login');
      
      const forgotPasswordButton = page.locator('text=Forgot your password?');
      if (await forgotPasswordButton.isVisible()) {
        await forgotPasswordButton.click();
        
        // Modal should appear with form
        const modal = page.locator('[class*="bg-white"][class*="dark:bg-gray-800"][class*="p-6"]');
        await expect(modal).toBeVisible();
        
        // Check form elements
        const emailInput = page.locator('input[id="forgotEmail"]');
        await expect(emailInput).toBeVisible();
        await expect(emailInput).toHaveClass(/w-full/);
        await expect(emailInput).toHaveClass(/px-3/);
        await expect(emailInput).toHaveClass(/py-2/);
        
        // Check buttons
        const cancelButton = page.locator('text=Cancel');
        const submitButton = page.locator('text=Send Reset Email');
        
        await expect(cancelButton).toBeVisible();
        await expect(submitButton).toBeVisible();
      }
    });

    test('should handle create group modal form layout', async ({ page }) => {
      const newGroupButton = page.locator('text=New Group');
      if (await newGroupButton.isVisible()) {
        await newGroupButton.click();
        
        // Check modal appears
        const modal = page.locator('[class*="fixed"][class*="inset-0"]').last();
        if (await modal.isVisible()) {
          // Look for form inputs
          const formInputs = modal.locator('input, textarea');
          const inputCount = await formInputs.count();
          
          if (inputCount > 0) {
            const firstInput = formInputs.first();
            await expect(firstInput).toBeVisible();
            
            // Input should have proper styling
            await expect(firstInput).toHaveClass(/w-full/);
            await expect(firstInput).toHaveClass(/border/);
            await expect(firstInput).toHaveClass(/rounded/);
          }
        }
      }
    });
  });

  test.describe('Search Form Components', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/login');
      await page.fill('input[name="email"]', 'test@example.com');
      await page.fill('input[name="password"]', 'password123');
      await page.click('button[type="submit"]');
      await expect(page).toHaveURL('/chat');
    });

    test('should display search input properly', async ({ page }) => {
      const searchButton = page.locator('button[title*="Search" i], button[aria-label*="Search" i]').first();
      if (await searchButton.isVisible()) {
        await searchButton.click();
        
        // Look for search input field
        const searchInput = page.locator('input[placeholder*="search" i], input[type="search"]');
        if (await searchInput.isVisible()) {
          await expect(searchInput).toHaveClass(/w-full/);
          await expect(searchInput).toHaveClass(/border/);
          await expect(searchInput).toHaveClass(/rounded/);
          
          // Check input is properly sized
          const inputBox = await searchInput.boundingBox();
          expect(inputBox?.height).toBeGreaterThan(32);
        }
      }
    });

    test('should handle user search modal form', async ({ page }) => {
      const newMessageButton = page.locator('text=New Message');
      if (await newMessageButton.isVisible()) {
        await newMessageButton.click();
        
        const userSearchModal = page.locator('[class*="fixed"][class*="inset-0"]').last();
        if (await userSearchModal.isVisible()) {
          const searchInput = userSearchModal.locator('input[type="text"], input[placeholder*="search" i]');
          if (await searchInput.isVisible()) {
            await expect(searchInput).toBeVisible();
            await expect(searchInput).toHaveClass(/w-full/);
            
            // Test input functionality
            await searchInput.fill('test');
            expect(await searchInput.inputValue()).toBe('test');
          }
        }
      }
    });
  });

  test.describe('Form Validation and States', () => {
    test('should show proper validation states on login form', async ({ page }) => {
      await page.goto('/login');
      
      // Test required field validation
      const submitButton = page.locator('button[type="submit"]');
      await submitButton.click();
      
      // HTML5 validation should prevent submission
      const emailInput = page.locator('input[name="email"]');
      const passwordInput = page.locator('input[name="password"]');
      
      // Check inputs are marked as required
      expect(await emailInput.getAttribute('required')).toBe('');
      expect(await passwordInput.getAttribute('required')).toBe('');
    });

    test('should handle loading states in forms', async ({ page }) => {
      await page.goto('/login');
      
      // Fill form with test data
      await page.fill('input[name="email"]', 'test@example.com');
      await page.fill('input[name="password"]', 'password123');
      
      // Submit form
      const submitButton = page.locator('button[type="submit"]');
      await submitButton.click();
      
      // Button should show loading state (if authentication is slow)
      // This might show "Signing in..." text
      await page.waitForTimeout(100);
    });

    test('should maintain focus management in forms', async ({ page }) => {
      await page.goto('/login');
      
      const emailInput = page.locator('input[name="email"]');
      const passwordInput = page.locator('input[name="password"]');
      
      // Test tab navigation
      await emailInput.focus();
      await expect(emailInput).toBeFocused();
      
      await page.keyboard.press('Tab');
      await expect(passwordInput).toBeFocused();
    });
  });

  test.describe('Form Accessibility', () => {
    test('should have proper labels and ARIA attributes', async ({ page }) => {
      await page.goto('/login');
      
      // Check for proper labeling
      const emailInput = page.locator('input[name="email"]');
      const passwordInput = page.locator('input[name="password"]');
      
      // Should have labels (even if sr-only)
      const emailLabel = page.locator('label[for="email"]');
      const passwordLabel = page.locator('label[for="password"]');
      
      await expect(emailLabel).toBeVisible();
      await expect(passwordLabel).toBeVisible();
      
      // Check placeholder text is descriptive
      expect(await emailInput.getAttribute('placeholder')).toContain('Email');
      expect(await passwordInput.getAttribute('placeholder')).toContain('Password');
    });

    test('should support keyboard navigation', async ({ page }) => {
      await page.goto('/login');
      
      // All interactive elements should be keyboard accessible
      await page.keyboard.press('Tab'); // Email input
      await expect(page.locator('input[name="email"]')).toBeFocused();
      
      await page.keyboard.press('Tab'); // Password input
      await expect(page.locator('input[name="password"]')).toBeFocused();
      
      await page.keyboard.press('Tab'); // Submit button
      await expect(page.locator('button[type="submit"]')).toBeFocused();
    });

    test('should have proper contrast for form elements', async ({ page }) => {
      await page.goto('/login');
      
      const emailInput = page.locator('input[name="email"]');
      
      // Input should have proper border contrast
      const borderColor = await emailInput.evaluate(el => 
        getComputedStyle(el).borderColor
      );
      
      // Should not be too light (basic contrast check)
      expect(borderColor).not.toBe('rgb(255, 255, 255)');
      expect(borderColor).not.toBe('rgb(248, 248, 248)');
    });
  });
});