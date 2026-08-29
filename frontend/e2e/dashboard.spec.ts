import { test, expect } from '@playwright/test';

const fakeUser = {
  token: 'fake-token',
  username: 'tester',
  email: 'tester@example.com',
  role: 'STUDENT',
};

test.beforeEach(async ({ context, page }) => {
  await context.addInitScript((user) => {
    window.localStorage.setItem('token', user.token);
    window.localStorage.setItem('user', JSON.stringify(user));
  }, fakeUser);

  await page.route('**/api/students**', (route) =>
    route.fulfill({ json: [] }),
  );
  await page.route('**/api/courses**', (route) =>
    route.fulfill({ json: [] }),
  );
  await page.route('**/api/enrollments**', (route) =>
    route.fulfill({ json: [] }),
  );
});

test('dashboard has no horizontal overflow', async ({ page }) => {
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

test('dashboard title and stat cards are not clipped', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Dashboard' })).toHaveText('Dashboard');
  await expect(page.getByText('Total Students')).toBeVisible();
  await expect(page.getByText('Active Courses')).toBeVisible();
  await expect(page.getByText('Total Enrollments')).toBeVisible();
  await expect(page.getByText('Pending Reviews')).toBeVisible();
});
