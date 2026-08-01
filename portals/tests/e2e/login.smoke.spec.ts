import { expect, test } from '@playwright/test';

test.describe('Recruiter portal smoke', () => {
  test('renders the sign-in screen with branding', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByText('Integrity Pro', { exact: false }).first()).toBeVisible();
    await expect(page.getByRole('heading', { name: /Welcome back/i })).toBeVisible();
    await expect(page.getByLabel(/Work email/i)).toBeVisible();
    await expect(page.getByLabel(/Password/i)).toBeVisible();
  });

  test('rejects an empty form with validation errors', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: /Sign in/i }).click();
    await expect(page.getByText('Email is required')).toBeVisible();
    await expect(page.getByText('Password is required')).toBeVisible();
  });

  test('rejects an invalid email', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/Work email/i).fill('not-an-email');
    await page.getByLabel(/Password/i).fill('password123');
    await page.getByRole('button', { name: /Sign in/i }).click();
    await expect(page.getByText('Enter a valid email address')).toBeVisible();
  });

  test('links to registration and password recovery', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('link', { name: /Create an organization/i }).click();
    await expect(page).toHaveURL(/\/register/);
    await page.goto('/login');
    await page.getByRole('link', { name: /Forgot password/i }).click();
    await expect(page).toHaveURL(/\/forgot-password/);
  });
});
