import { test, expect } from "@playwright/test";

function uniqueEmail() {
  return `e2e-${Date.now()}-${Math.floor(Math.random() * 10000)}@example.com`;
}

const PASSWORD = "SuperSecret123!";

test.describe("TracePilot Core Workflows", () => {
  const email = uniqueEmail();

  test("critical path: register -> submit -> audit -> share -> revoke -> logout", async ({ page, context }) => {

    await page.goto("/register");

    await page.getByPlaceholder("Your name").fill("E2E Test User");
    await page.getByPlaceholder("you@example.com").fill(email);
    await page.getByPlaceholder("Min. 8 characters").fill(PASSWORD);
    await page.getByPlaceholder("Repeat password").fill(PASSWORD);
    
    await page.getByRole("button", { name: /create account/i }).click();

    await expect(page).toHaveURL(/\/app\/submit/, { timeout: 15_000 });

    
    await page.goto("/app/submit");
    
    await page.waitForURL(/\/app\/submit|\/login/, { timeout: 15_000 });

    if (page.url().includes("/login")) {
      await page.getByPlaceholder("you@example.com").fill(email);
      await page.getByPlaceholder("••••••••").fill(PASSWORD);
      await page.getByRole("button", { name: /^login$/i }).click();
      await expect(page).toHaveURL(/\/app\/submit/, { timeout: 15_000 });
    }

    await expect(page.getByRole("heading", { name: /submit/i })).toBeVisible({ timeout: 15_000 });

    await page.getByRole("button", { name: /log ?out/i }).click();
    await expect(page).toHaveURL(/\/login$/, { timeout: 10_000 });

    // Log back in for the remaining actions
    await page.goto("/login");
    await page.getByPlaceholder("you@example.com").fill(email);
    await page.getByPlaceholder("••••••••").fill(PASSWORD);
    await page.getByRole("button", { name: /^login$/i }).click();
    await expect(page).toHaveURL(/\/app\/submit/, { timeout: 15_000 });

    await expect(page.getByRole("heading", { name: /submit/i })).toBeVisible();

    await page.getByRole("button", { name: /load sample/i }).click();
    await page.getByPlaceholder(/billing discount fix/i).fill("E2E sample audit");

    await page.getByRole("button", { name: /start audit/i }).click();

    await expect(page).toHaveURL(/\/app\/audits\/[^/]+\/processing/, { timeout: 15_000 });
    await expect(page).toHaveURL(/\/app\/audits\/[^/]+$/, { timeout: 120_000 });

    await expect(page.getByText(/Outcome:/i).first()).toBeVisible({ timeout: 10_000 });


    await page.goto("/app/history");
    
    const auditButton = page.getByRole('button', { name: 'E2E sample audit — Generic' }).first();
    await expect(auditButton).toBeVisible({ timeout: 10_000 });
    await auditButton.click();
    
    await expect(page).toHaveURL(/\/app\/audits\/[^/]+$/);

   
    const auditUrl = page.url();
    const auditId = auditUrl.match(/\/app\/audits\/([^/]+)$/)?.[1];
    expect(auditId).toBeTruthy();

    await context.grantPermissions(["clipboard-read", "clipboard-write"]);
    await page.getByRole("button", { name: /share report/i }).click();
    await expect(page.getByRole("button", { name: /revoke share link/i })).toBeVisible({
      timeout: 10_000,
    });

    const shareUrl = await page.evaluate(() => navigator.clipboard.readText());
    expect(shareUrl).toContain("/shared/");

    const loggedOutPage = await context.newPage();
    await loggedOutPage.goto(shareUrl);
    await expect(loggedOutPage.getByText(/Outcome:/i).first()).toBeVisible({ timeout: 10_000 });
    await loggedOutPage.close();

    await page.getByRole("button", { name: /revoke share link/i }).click();
    await expect(page.getByRole("button", { name: /share report/i })).toBeVisible({
      timeout: 10_000,
    });

    const revokedPage = await context.newPage();
    await revokedPage.goto(shareUrl);
    const stillShowsFindings = await revokedPage
      .getByText(/Outcome:/i)
      .first()
      .isVisible()
      .catch(() => false);
    expect(stillShowsFindings).toBe(false);
    await revokedPage.close();
  });


  test("an unauthenticated user is redirected away from protected app routes", async ({ page }) => {
    await page.goto("/app/history");
    await expect(page).toHaveURL(/\/login$/, { timeout: 10_000 });
  });

  test("a non-admin user is redirected away from the admin page", async ({ page }) => {
    const email = uniqueEmail();
    await page.goto("/register");
    await page.getByPlaceholder("Your name").fill("Non Admin");
    await page.getByPlaceholder("you@example.com").fill(email);
    await page.getByPlaceholder("Min. 8 characters").fill(PASSWORD);
    await page.getByPlaceholder("Repeat password").fill(PASSWORD);
    await page.getByRole("button", { name: /create account/i }).click();
    await expect(page).toHaveURL(/\/app\/submit/, { timeout: 15_000 });

    await page.goto("/admin", { waitUntil: 'commit' }).catch(() => { });;
    await page.waitForURL(url => !url.pathname.endsWith('/admin'), { timeout: 10_000 });
    await expect(page).not.toHaveURL(/\/admin$/, { timeout: 10_000 });
  });
});