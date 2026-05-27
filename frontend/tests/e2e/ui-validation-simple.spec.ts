import { test, expect } from '@playwright/test';

test.describe('UI Validation - Login Page Responsiveness', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3000/login');
  });

  test.describe('Desktop Layout (1280x720)', () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 720 });
    });

    test('should display login form with proper desktop layout', async ({ page }) => {
      // Check main container
      const container = page.locator('div.min-h-screen.flex.items-center.justify-center');
      await expect(container).toBeVisible();
      
      // Check form container max-width
      const formContainer = page.locator('div.max-w-md.w-full');
      await expect(formContainer).toBeVisible();
      
      // Check form spacing
      const form = page.locator('form.mt-8.space-y-6');
      await expect(form).toBeVisible();
      
      // Verify input field sizing
      const emailInput = page.locator('input[name="email"]');
      await expect(emailInput).toBeVisible();
      
      const inputBox = await emailInput.boundingBox();
      expect(inputBox?.height).toBeGreaterThanOrEqual(40); // Min height for accessibility
    });

    test('should show forgot password modal with proper positioning', async ({ page }) => {
      // Click forgot password link
      await page.click('button:has-text("Forgot your password?")');
      
      // Check modal overlay
      const modalOverlay = page.locator('div.fixed.inset-0.bg-gray-600');
      await expect(modalOverlay).toBeVisible();
      
      // Check modal content centering
      const modalContent = page.locator('div.bg-white.p-6.rounded-lg');
      await expect(modalContent).toBeVisible();
      
      // Verify modal max-width
      const modalBox = await modalContent.boundingBox();
      expect(modalBox?.width).toBeLessThanOrEqual(500); // max-w-md constraint
    });
  });

  test.describe('Mobile Layout (393x851)', () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 393, height: 851 });
    });

    test('should adapt login form for mobile screens', async ({ page }) => {
      // Check container still centers content
      const container = page.locator('div.min-h-screen.flex.items-center.justify-center');
      await expect(container).toBeVisible();
      
      // Check responsive padding
      const outerContainer = page.locator('div.py-12.px-4.sm\\:px-6');
      await expect(outerContainer).toBeVisible();
      
      // Verify input fields are touch-friendly
      const emailInput = page.locator('input[name="email"]');
      const inputBox = await emailInput.boundingBox();
      expect(inputBox?.height).toBeGreaterThanOrEqual(44); // Minimum touch target
      
      // Check button size
      const submitButton = page.locator('button[type="submit"]');
      const buttonBox = await submitButton.boundingBox();
      expect(buttonBox?.height).toBeGreaterThanOrEqual(44);
    });

    test('should show full-width forgot password modal on mobile', async ({ page }) => {
      // Click forgot password
      await page.click('button:has-text("Forgot your password?")');
      
      // Check modal adapts to mobile
      const modalContent = page.locator('div.bg-white.p-6.rounded-lg');
      await expect(modalContent).toBeVisible();
      
      // Modal should have mobile padding
      const modalWithMargin = page.locator('div.mx-4');
      await expect(modalWithMargin).toBeVisible();
    });

    test('should maintain proper text sizes on mobile', async ({ page }) => {
      // Check heading is readable
      const heading = page.locator('h2.text-3xl');
      await expect(heading).toBeVisible();
      
      // Check subtitle
      const subtitle = page.locator('p.text-sm.text-gray-600');
      await expect(subtitle).toBeVisible();
      
      // Verify form labels are accessible
      const emailInput = page.locator('input[name="email"]');
      await expect(emailInput).toHaveAttribute('placeholder', 'Email address');
    });
  });

  test.describe('Tablet Layout (768x1024)', () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
    });

    test('should use desktop-like layout on tablet', async ({ page }) => {
      // Should behave like desktop
      const container = page.locator('div.min-h-screen.flex.items-center.justify-center');
      await expect(container).toBeVisible();
      
      // Form should be centered with max-width
      const formContainer = page.locator('div.max-w-md.w-full');
      await expect(formContainer).toBeVisible();
      
      const formBox = await formContainer.boundingBox();
      expect(formBox?.width).toBeLessThanOrEqual(448); // max-w-md = 28rem = 448px
    });
  });

  test.describe('Form State Changes', () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 720 });
    });

    test('should properly toggle between login and register modes', async ({ page }) => {
      // Start in login mode
      await expect(page.locator('h2:has-text("Sign in to Chat Platform")')).toBeVisible();
      
      // Switch to register
      await page.click('button:has-text("Don\'t have an account? Create one")');
      
      // Check register mode UI
      await expect(page.locator('h2:has-text("Create Account")')).toBeVisible();
      await expect(page.locator('input[name="username"]')).toBeVisible();
      await expect(page.locator('input[name="displayName"]')).toBeVisible();
      
      // Verify additional fields are properly sized
      const usernameInput = page.locator('input[name="username"]');
      const usernameBox = await usernameInput.boundingBox();
      expect(usernameBox?.height).toBeGreaterThanOrEqual(40);
    });
  });

  test.describe('Dark Mode Compatibility', () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 720 });
      // Simulate dark mode
      await page.emulateMedia({ colorScheme: 'dark' });
    });

    test('should apply dark mode classes correctly', async ({ page }) => {
      // Check dark background
      const container = page.locator('div.bg-gray-50.dark\\:bg-gray-900');
      await expect(container).toBeVisible();
      
      // Check dark text classes
      const heading = page.locator('h2.text-gray-900.dark\\:text-gray-100');
      await expect(heading).toBeVisible();
      
      // Verify input styling in dark mode
      const emailInput = page.locator('input[name="email"]');
      await expect(emailInput).toBeVisible();
    });
  });
});

test.describe('Component Spacing and Alignment', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3000/login');
  });

  test('should have consistent spacing between form elements', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    
    // Check space-y-6 on form
    const form = page.locator('form.space-y-6');
    await expect(form).toBeVisible();
    
    // Check space-y-4 on input container
    const inputContainer = page.locator('div.space-y-4');
    await expect(inputContainer).toBeVisible();
    
    // Verify button spacing
    const buttonContainer = page.locator('div.space-y-4');
    await expect(buttonContainer).toBeVisible();
  });

  test('should maintain proper alignment on various screen sizes', async ({ page }) => {
    const screenSizes = [
      { width: 320, height: 568 }, // iPhone SE
      { width: 375, height: 667 }, // iPhone 8
      { width: 414, height: 896 }, // iPhone XR
      { width: 768, height: 1024 }, // iPad
      { width: 1024, height: 768 }, // iPad landscape
      { width: 1280, height: 720 }, // Desktop
      { width: 1920, height: 1080 } // Large desktop
    ];

    for (const size of screenSizes) {
      await page.setViewportSize(size);
      
      // Check centering is maintained
      const container = page.locator('div.flex.items-center.justify-center');
      await expect(container).toBeVisible();
      
      // Check form is responsive
      const formContainer = page.locator('div.max-w-md.w-full');
      await expect(formContainer).toBeVisible();
      
      // Verify submit button spans full width
      const submitButton = page.locator('button[type="submit"].w-full');
      await expect(submitButton).toBeVisible();
    }
  });
});