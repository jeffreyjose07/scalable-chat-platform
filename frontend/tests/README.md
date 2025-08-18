# Playwright E2E Testing Setup

## Overview

This directory contains comprehensive end-to-end tests for the React chat application using Playwright. The tests focus on responsive design and UI component behavior across different devices and screen sizes.

## Test Structure

### Test Files

1. **responsive-layout.spec.ts** - Tests overall responsive layout behavior
   - Desktop layout (1280x720)
   - Mobile layout (393x851) 
   - Tablet layout (1024x1366)
   - Cross-device consistency
   - Responsive breakpoint transitions

2. **sidebar-behavior.spec.ts** - Tests sidebar behavior across devices
   - Fixed sidebar on desktop
   - Overlay sidebar on mobile
   - Tablet behavior
   - Accessibility and keyboard navigation

3. **chat-interface-responsive.spec.ts** - Tests chat interface responsiveness
   - Message display and scrolling
   - Input field behavior
   - Search panel behavior
   - Connection status display
   - Performance considerations

4. **modal-responsive.spec.ts** - Tests modal positioning and sizing
   - Modal centering on desktop
   - Full-width modals on mobile
   - Modal interactions and focus management
   - Accessibility compliance

5. **form-layouts.spec.ts** - Tests form layouts and input alignment
   - Login form responsiveness
   - Chat input forms
   - Modal form layouts
   - Form validation and states
   - Input accessibility

6. **navigation-components.spec.ts** - Tests navigation components
   - User menu and dropdowns
   - Theme toggle functionality
   - Mobile hamburger menu
   - Touch-friendly interactions
   - Keyboard navigation

### Test Configuration

The tests are configured to run across multiple browsers and viewports:

- **Desktop Chrome** (1280x720)
- **Desktop Firefox** (1280x720)
- **Desktop Safari** (1280x720)
- **Mobile Chrome** (393x851) - Pixel 5 viewport
- **Mobile Safari** (390x844) - iPhone 12 viewport
- **Tablet** (1024x1366) - iPad Pro viewport

## Key Testing Areas

### Responsive Design
- Layout adaptation across breakpoints
- Sidebar behavior (fixed vs overlay)
- Touch-friendly button sizes (44px minimum)
- Proper spacing and padding

### User Interface Components
- Modal positioning and sizing
- Form input alignment
- Navigation elements
- Search functionality
- Connection status indicators

### Accessibility
- ARIA labels and roles
- Keyboard navigation
- Focus management
- Screen reader compatibility
- Color contrast requirements

### Performance
- Smooth scrolling behavior
- Transition animations
- Virtual keyboard handling
- Backdrop blur effects

## Running Tests

### All Tests
```bash
npm run test:e2e
```

### Interactive Mode
```bash
npm run test:e2e:ui
```

### Headed Mode (Visible Browser)
```bash
npm run test:e2e:headed
```

### Debug Mode
```bash
npm run test:e2e:debug
```

### View Test Report
```bash
npm run test:e2e:report
```

### Run Specific Test File
```bash
npx playwright test responsive-layout.spec.ts
```

### Run Tests for Specific Browser
```bash
npx playwright test --project="Mobile Chrome"
```

## Test Data Setup

The tests use mock authentication with the following test user:
```typescript
{
  email: 'test@example.com',
  password: 'password123',
  username: 'testuser',
  displayName: 'Test User'
}
```

**Note**: These credentials are for testing purposes only. In a real environment, you'll need to set up proper test data or use your application's existing test users.

## Responsive Breakpoints Tested

- **Mobile**: 393px width (Pixel 5)
- **Mobile Large**: 390px width (iPhone 12)  
- **Tablet**: 1024px width (iPad Pro)
- **Desktop**: 1280px width
- **Desktop Large**: 1920px width

## Key Features Tested

### Mobile-Specific
- Hamburger menu functionality
- Sidebar overlay behavior
- Touch-friendly interactions
- Virtual keyboard handling
- Full-screen search overlays

### Desktop-Specific
- Fixed sidebar layout
- Side-by-side search panels
- Hover states and interactions
- Keyboard shortcuts
- Multi-column layouts

### Cross-Device
- Consistent branding
- Theme toggle persistence
- Connection status display
- User authentication flows
- Modal behavior

## Common Test Patterns

### Responsive Layout Checks
```typescript
// Check viewport-specific classes
await expect(element).toHaveClass(/lg:w-72/);
await expect(element).toHaveClass(/sm:px-6/);

// Verify touch-friendly sizing
const box = await element.boundingBox();
expect(box?.height).toBeGreaterThanOrEqual(44);
```

### Modal Testing
```typescript
// Check modal positioning
const modal = page.locator('[class*="fixed"][class*="inset-0"]');
await expect(modal).toBeVisible();

// Test overlay interactions  
await overlay.click();
await expect(modal).not.toBeVisible();
```

### Sidebar Behavior
```typescript
// Mobile sidebar overlay
const hamburger = page.locator('button[class*="lg:hidden"]');
await hamburger.click();

const overlay = page.locator('[class*="bg-opacity-50"]');
await expect(overlay).toBeVisible();
```

## Troubleshooting

### Test Failures
1. Ensure the development server is running on `http://localhost:3000`
2. Verify test user credentials exist in your system
3. Check for timing issues with `page.waitForTimeout()`
4. Ensure proper element selectors are used

### Browser Issues
1. Update Playwright browsers: `npx playwright install`
2. Check browser compatibility in `playwright.config.ts`
3. Verify viewport sizes match your application's breakpoints

### Authentication Issues
1. Update test credentials in `setup.ts`
2. Ensure login flow matches your application
3. Check for CSRF tokens or other auth requirements

## Best Practices

1. **Use stable selectors**: Prefer `data-testid` over class names
2. **Wait for elements**: Use `expect().toBeVisible()` instead of `isVisible()`
3. **Mock external services**: Avoid dependencies on real APIs
4. **Test user flows**: Focus on complete user interactions
5. **Keep tests independent**: Each test should be self-contained

## Future Enhancements

- Add visual regression testing with screenshot comparisons
- Implement accessibility testing with axe-core
- Add performance monitoring and metrics
- Test dark mode/theme switching
- Add internationalization testing