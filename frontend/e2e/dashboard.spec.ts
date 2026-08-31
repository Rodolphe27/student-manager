import { test, expect } from '@playwright/test';

const user = { token: 'fake-token', username: 'tester', email: 'tester@example.com' };

test.describe('Dashboard — staff', () => {
  test.beforeEach(async ({ context, page }) => {
    await context.addInitScript((u) => {
      window.localStorage.setItem('token', u.token);
      window.localStorage.setItem('user', JSON.stringify(u));
    }, { ...user, role: 'ADMIN' });

    await page.route('**/api/students**', (route) => route.fulfill({ json: [] }));
    await page.route('**/api/courses**', (route) => route.fulfill({ json: [] }));
    await page.route('**/api/enrollments**', (route) => route.fulfill({ json: [] }));
  });

  test('has no horizontal overflow', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

    const overflow = await page.evaluate(() => {
      const main = document.querySelector('main');
      return {
        documentOverflows: document.documentElement.scrollWidth > document.documentElement.clientWidth,
        mainOverflows: main ? main.scrollWidth > main.clientWidth : null,
      };
    });

    expect(overflow.documentOverflows).toBe(false);
    expect(overflow.mainOverflows).toBe(false);
  });

  test('title and stat cards are not clipped', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Dashboard' })).toHaveText('Dashboard');
    await expect(page.getByText('Total Students')).toBeVisible();
    await expect(page.getByText('Active Courses')).toBeVisible();
    await expect(page.getByText('Total Enrollments')).toBeVisible();
    await expect(page.getByText('Pending Reviews')).toBeVisible();
  });
});

test.describe('Dashboard — student', () => {
  test.beforeEach(async ({ context, page }) => {
    await context.addInitScript((u) => {
      window.localStorage.setItem('token', u.token);
      window.localStorage.setItem('user', JSON.stringify(u));
    }, { ...user, role: 'STUDENT' });

    // A STUDENT only reaches these endpoints; the list endpoints 403.
    await page.route('**/api/students', (route) => route.fulfill({ status: 403, json: {} }));
    await page.route('**/api/enrollments', (route) => route.fulfill({ status: 403, json: {} }));
    await page.route('**/api/students/me', (route) =>
      route.fulfill({ json: { id: 1, firstName: 'T', lastName: 'Ester', email: user.email } }),
    );
    await page.route('**/api/courses**', (route) => route.fulfill({ json: [] }));
    await page.route('**/api/enrollments/student/**', (route) => route.fulfill({ json: [] }));
  });

  test('loads a student-scoped dashboard without the error state', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByText('My Enrollments')).toBeVisible();
    await expect(page.getByText('Available Courses')).toBeVisible();
    await expect(page.getByText('Could not load dashboard data.')).toHaveCount(0);
    await expect(page.getByText('Total Students')).toHaveCount(0);
  });
});
