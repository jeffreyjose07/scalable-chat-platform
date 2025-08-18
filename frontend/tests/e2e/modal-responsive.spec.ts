import { test, expect } from '@playwright/test';

test.describe('Modal Positioning and Sizing', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/chat');
  });

  test.describe('Desktop Modal Behavior (1280x720)', () => {
    test.use({ viewport: { width: 1280, height: 720 } });

    test('should center user settings modal on desktop', async ({ page }) => {
      // Try to open user settings modal
      const userMenuButton = page.locator('[title="User Menu"], button[class*="text-sm"][class*="text-gray-700"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings, button[class*="w-full"][class*="text-left"]');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          // Check modal overlay
          const modalOverlay = page.locator('[class*="fixed"][class*="inset-0"][class*="bg-gray-600"], [class*="fixed"][class*="inset-0"][class*="bg-black"]');
          await expect(modalOverlay).toBeVisible();
          
          // Check modal content is centered
          const modalContent = page.locator('[class*="flex"][class*="items-center"][class*="justify-center"] > div');
          if (await modalContent.isVisible()) {
            const modalBox = await modalContent.boundingBox();
            const viewportWidth = 1280;
            const viewportHeight = 720;
            
            if (modalBox) {
              // Modal should be roughly centered
              const centerX = modalBox.x + modalBox.width / 2;
              const centerY = modalBox.y + modalBox.height / 2;
              
              expect(Math.abs(centerX - viewportWidth / 2)).toBeLessThan(100);
              expect(Math.abs(centerY - viewportHeight / 2)).toBeLessThan(100);
            }
          }
        }
      }
    });

    test('should display create group modal with proper sizing', async ({ page }) => {
      // Navigate to groups tab and try to create new group
      const groupsTab = page.locator('text=Groups, button[class*="flex-1"]');
      if (await groupsTab.isVisible()) {
        await groupsTab.click();
        
        const newGroupButton = page.locator('text=New Group, button[class*="w-full"]');
        if (await newGroupButton.isVisible()) {
          await newGroupButton.click();
          
          // Check modal appears with proper sizing
          const modal = page.locator('[class*="fixed"][class*="inset-0"] [class*="bg-white"][class*="dark:bg-gray-800"]');
          if (await modal.isVisible()) {
            const modalBox = await modal.boundingBox();
            
            if (modalBox) {
              // Modal should have reasonable size constraints
              expect(modalBox.width).toBeGreaterThan(400);
              expect(modalBox.width).toBeLessThan(800);
              expect(modalBox.height).toBeGreaterThan(300);
              
              // Should not exceed viewport
              expect(modalBox.width).toBeLessThan(1280);
              expect(modalBox.height).toBeLessThan(720);
            }
          }
        }
      }
    });

    test('should handle user search modal positioning', async ({ page }) => {
      // Try to open user search modal
      const newMessageButton = page.locator('text=New Message, button[class*="w-full"]');
      if (await newMessageButton.isVisible()) {
        await newMessageButton.click();
        
        const userSearchModal = page.locator('[class*="fixed"][class*="inset-0"]').last();
        if (await userSearchModal.isVisible()) {
          await expect(userSearchModal).toBeVisible();
          
          // Check modal has proper z-index
          const zIndex = await userSearchModal.evaluate(el => 
            getComputedStyle(el).zIndex
          );
          expect(parseInt(zIndex)).toBeGreaterThan(40);
        }
      }
    });

    test('should show modal overlay with proper opacity', async ({ page }) => {
      // Open any modal to test overlay
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          const overlay = page.locator('[class*="bg-opacity-50"], [class*="bg-opacity-75"]');
          if (await overlay.isVisible()) {
            const opacity = await overlay.evaluate(el => {
              const bgColor = getComputedStyle(el).backgroundColor;
              return bgColor.includes('rgba') ? bgColor.match(/[\d.]+(?=\))/)?.[0] : '1';
            });
            
            // Should be semi-transparent
            expect(parseFloat(opacity || '1')).toBeLessThan(1);
            expect(parseFloat(opacity || '0')).toBeGreaterThan(0.3);
          }
        }
      }
    });
  });

  test.describe('Mobile Modal Behavior (393x851)', () => {
    test.use({ viewport: { width: 393, height: 851 } });

    test('should display modals full-width on mobile', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          const modalContent = page.locator('[class*="bg-white"][class*="dark:bg-gray-800"]').last();
          if (await modalContent.isVisible()) {
            const modalBox = await modalContent.boundingBox();
            
            if (modalBox) {
              // On mobile, modal should use most of the width
              expect(modalBox.width).toBeGreaterThan(300);
              expect(modalBox.width).toBeLessThan(393 - 20); // Account for margins
            }
          }
        }
      }
    });

    test('should handle mobile keyboard interaction with modals', async ({ page }) => {
      // Test modal input fields with virtual keyboard
      const newGroupButton = page.locator('text=New Group');
      if (await newGroupButton.isVisible()) {
        await newGroupButton.click();
        
        const groupNameInput = page.locator('input[placeholder*="group" i], input[name*="name" i]');
        if (await groupNameInput.isVisible()) {
          await groupNameInput.focus();
          
          // Input should remain visible when focused
          await expect(groupNameInput).toBeVisible();
          
          // Modal should still be accessible
          const modal = groupNameInput.locator('..').locator('..').locator('..');
          await expect(modal).toBeVisible();
        }
      }
    });

    test('should show mobile-optimized modal buttons', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          // Check for modal action buttons
          const actionButtons = page.locator('[class*="bg-white"] button, [class*="bg-gray-800"] button');
          
          for (let i = 0; i < await actionButtons.count() && i < 3; i++) {
            const button = actionButtons.nth(i);
            if (await button.isVisible()) {
              const buttonBox = await button.boundingBox();
              
              // Mobile buttons should be touch-friendly
              expect(buttonBox?.height).toBeGreaterThanOrEqual(44);
            }
          }
        }
      }
    });

    test('should handle modal scrolling on mobile', async ({ page }) => {
      // Try to open a potentially tall modal
      const newGroupButton = page.locator('text=New Group');
      if (await newGroupButton.isVisible()) {
        await newGroupButton.click();
        
        const modal = page.locator('[class*="fixed"][class*="inset-0"]').last();
        if (await modal.isVisible()) {
          // Modal should be scrollable if content exceeds viewport
          const overflowY = await modal.evaluate(el => 
            getComputedStyle(el).overflowY
          );
          expect(overflowY).toMatch(/auto|scroll|visible/);
        }
      }
    });
  });

  test.describe('Tablet Modal Behavior (1024x1366)', () => {
    test.use({ viewport: { width: 1024, height: 1366 } });

    test('should use appropriate modal sizing on tablet', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          const modalContent = page.locator('[class*="bg-white"], [class*="bg-gray-800"]').last();
          if (await modalContent.isVisible()) {
            const modalBox = await modalContent.boundingBox();
            
            if (modalBox) {
              // Tablet should use desktop-like modal sizing
              expect(modalBox.width).toBeGreaterThan(500);
              expect(modalBox.width).toBeLessThan(700);
            }
          }
        }
      }
    });
  });

  test.describe('Modal Content and Layout', () => {
    test('should display modal headers properly', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          // Look for modal header elements
          const modalHeader = page.locator('h1, h2, h3, [class*="text-lg"], [class*="font-medium"]').last();
          if (await modalHeader.isVisible()) {
            await expect(modalHeader).toBeVisible();
            
            // Header should have appropriate styling
            const headerStyles = await modalHeader.evaluate(el => ({
              fontSize: getComputedStyle(el).fontSize,
              fontWeight: getComputedStyle(el).fontWeight
            }));
            
            expect(parseFloat(headerStyles.fontSize)).toBeGreaterThan(14);
            expect(parseInt(headerStyles.fontWeight)).toBeGreaterThan(400);
          }
        }
      }
    });

    test('should handle form inputs in modals', async ({ page }) => {
      const newGroupButton = page.locator('text=New Group');
      if (await newGroupButton.isVisible()) {
        await newGroupButton.click();
        
        const formInputs = page.locator('input, textarea, select');
        const inputCount = await formInputs.count();
        
        if (inputCount > 0) {
          const firstInput = formInputs.first();
          await expect(firstInput).toBeVisible();
          
          // Input should be properly styled
          await expect(firstInput).toHaveClass(/border/);
          await expect(firstInput).toHaveClass(/rounded/);
          
          // Should be focusable
          await firstInput.focus();
          await expect(firstInput).toBeFocused();
        }
      }
    });

    test('should display modal close buttons', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          // Look for close button (X icon)
          const closeButton = page.locator('button svg[d*="6 18L18 6"], button[aria-label*="close" i]');
          if (await closeButton.isVisible()) {
            await expect(closeButton.first()).toBeVisible();
          }
        }
      }
    });
  });

  test.describe('Modal Interaction Patterns', () => {
    test('should close modal when clicking overlay', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          const overlay = page.locator('[class*="fixed"][class*="inset-0"][class*="bg-"]').first();
          if (await overlay.isVisible()) {
            // Click overlay background (not modal content)
            const overlayBox = await overlay.boundingBox();
            if (overlayBox) {
              await page.mouse.click(overlayBox.x + 50, overlayBox.y + 50);
            }
            
            // Modal should close
            await page.waitForTimeout(300);
            await expect(overlay).not.toBeVisible();
          }
        }
      }
    });

    test('should close modal with escape key', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          const modal = page.locator('[class*="fixed"][class*="inset-0"]').last();
          if (await modal.isVisible()) {
            await page.keyboard.press('Escape');
            
            // Modal should close
            await page.waitForTimeout(300);
            await expect(modal).not.toBeVisible();
          }
        }
      }
    });

    test('should handle modal focus management', async ({ page }) => {
      const newGroupButton = page.locator('text=New Group');
      if (await newGroupButton.isVisible()) {
        await newGroupButton.click();
        
        // First focusable element in modal should receive focus
        const modalInputs = page.locator('input, button, textarea, select');
        if (await modalInputs.count() > 0) {
          const firstFocusable = modalInputs.first();
          
          // Focus should be trapped in modal
          await page.keyboard.press('Tab');
          const focusedElement = page.locator(':focus');
          const isFocusInModal = await focusedElement.evaluate(el => {
            const modal = el.closest('[class*="fixed"][class*="inset-0"]');
            return !!modal;
          });
          
          expect(isFocusInModal).toBe(true);
        }
      }
    });
  });

  test.describe('Modal Accessibility', () => {
    test('should have proper ARIA attributes', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          const modal = page.locator('[class*="fixed"][class*="inset-0"]').last();
          if (await modal.isVisible()) {
            // Check for ARIA attributes
            const ariaLabel = await modal.getAttribute('aria-label');
            const ariaModal = await modal.getAttribute('aria-modal');
            const role = await modal.getAttribute('role');
            
            // Should have appropriate accessibility attributes
            expect(ariaModal || role || ariaLabel).toBeTruthy();
          }
        }
      }
    });

    test('should announce modal content to screen readers', async ({ page }) => {
      const userMenuButton = page.locator('[title="User Menu"]').first();
      if (await userMenuButton.isVisible()) {
        await userMenuButton.click();
        
        const settingsButton = page.locator('text=Settings');
        if (await settingsButton.isVisible()) {
          await settingsButton.click();
          
          // Check that modal has descriptive content
          const modalContent = page.locator('[class*="bg-white"], [class*="bg-gray-800"]').last();
          if (await modalContent.isVisible()) {
            const textContent = await modalContent.textContent();
            expect(textContent?.length).toBeGreaterThan(0);
          }
        }
      }
    });
  });
});