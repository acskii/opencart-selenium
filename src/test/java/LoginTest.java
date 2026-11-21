import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.LoginPage;
import pages.RegisterPage;
import utils.ExcelReader;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Login Test
 *
 * @BeforeClass ensures a registered user exists (idempotent) so tests have
 *               known-good credentials.
 * @BeforeMethod logs out so each test starts from an anonymous session.
 */
public class LoginTest extends BaseTest {

    private LoginPage    loginPage;
    private RegisterPage registerPage;
    private ExcelReader  excelReader;

    private String primaryEmail;
    private String primaryPassword;
    private String secondaryEmail;
    private String secondaryPassword;

    @BeforeClass
    public void setupTest() {
        loginPage    = new LoginPage(driver);
        registerPage = new RegisterPage(driver);
        excelReader  = new ExcelReader("src/main/resources/testData.xlsx");

        List<Map<String, String>> registerUsers = excelReader.readRegisterData();
        if (registerUsers.isEmpty()) {
            throw new IllegalStateException("No register users in Excel — cannot run login tests");
        }

        Map<String, String> primary = registerUsers.getFirst();
        primaryEmail    = primary.get("E-Mail");
        primaryPassword = primary.get("Password");

        if (registerUsers.size() > 1) {
            Map<String, String> secondary = registerUsers.get(1);
            secondaryEmail    = secondary.get("E-Mail");
            secondaryPassword = secondary.get("Password");
        } else {
            secondaryEmail    = primaryEmail;
            secondaryPassword = primaryPassword;
        }

        ensureRegistered(primary);
        if (!primaryEmail.equals(secondaryEmail)) {
            ensureRegistered(registerUsers.get(1));
        }

        logWriter.logSection("LOGIN TEST");
    }

    private void ensureRegistered(Map<String, String> user) {
        try {
            registerPage.navigateToRegisterPage();
            registerPage.registerUser(
                    user.get("First Name"), user.get("Last Name"),
                    user.get("E-Mail"), user.get("Password"));
            logWriter.logMessage("✓ Pre-registration ensured: " + user.get("E-Mail"));
        } catch (Exception e) {
            logWriter.logMessage("⚠ Could not ensure user " + user.get("E-Mail") + ": " + e.getMessage());
        }
    }

    @BeforeMethod
    public void ensureCleanState() {
        try { loginPage.logout(); } catch (Exception ignored) {}
    }

    // ==================================================================
    // Basic form coverage (adds value, not in test list)
    // ==================================================================
    @Test(priority = 1, description = "Login form renders with masked password field")
    public void testLoginFormElementsVisible() {
        logWriter.logMessage("\n=== Login form rendering ===");
        loginPage.navigateToLoginPage();
        Assert.assertTrue(loginPage.isLoginFormVisible(),
                "Email, password and Login button must all be visible");
        Assert.assertEquals(loginPage.getPasswordFieldType(), "password",
                "Password field must be masked (type=password)");
        logWriter.logMessage("✓ Form visible, password masked");
    }

    @Test(priority = 2, description = "Empty credentials are rejected")
    public void testLoginEmptyCredentials() {
        logWriter.logMessage("\n=== Empty credentials ===");
        boolean ok = loginPage.loginWithoutCredentials();
        Assert.assertFalse(ok, "Empty credentials must not authenticate");
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error must appear for empty credentials");
        Assert.assertFalse(loginPage.isUserLoggedIn(),
                "No session must exist after a rejected empty-credential login");
        logWriter.logMessage("✓ Empty credentials rejected: " + loginPage.getErrorMessage());
    }

    @Test(priority = 3, description = "Password is case-sensitive")
    public void testLoginPasswordCaseSensitive() {
        logWriter.logMessage("\n=== Password case-sensitivity ===");
        String wrongCase = primaryPassword.equals(primaryPassword.toUpperCase())
                ? primaryPassword.toLowerCase()
                : primaryPassword.toUpperCase();
        if (wrongCase.equals(primaryPassword)) wrongCase = primaryPassword + "_x";

        boolean ok = loginPage.login(primaryEmail, wrongCase);
        Assert.assertFalse(ok, "Wrong-case password must NOT authenticate");
        Assert.assertFalse(loginPage.isUserLoggedIn());
        Assert.assertTrue(loginPage.isErrorMessageDisplayed());
        logWriter.logMessage("✓ Wrong-case password rejected");
    }

    // ==================================================================
    // TC_096 – Successful login
    // ==================================================================
    @Test(priority = 4, description = "TC_096/TC_133 – Login with valid registered credentials")
    public void testLoginValidCredentials() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_096/TC_133: Valid login");
        logWriter.logMessage("Email: " + primaryEmail);
        logWriter.logMessage("========================================");

        boolean ok = loginPage.login(primaryEmail, primaryPassword);
        String url = loginPage.getCurrentUrl();
        logWriter.logMessage("   Login successful: " + ok);
        logWriter.logMessage("   Landing URL:      " + url);

        Assert.assertTrue(ok, "Valid credentials must authenticate");
        Assert.assertFalse(loginPage.isErrorMessageDisplayed(),
                "No error should appear on a valid login");
        Assert.assertTrue(loginPage.isUserLoggedIn(),
                "Logout link must be present after login");
        Assert.assertTrue(url.contains("account/account"),
                "Must land on account/account, was: " + url);

        logWriter.logRegistration(primaryEmail, true,
                "✓ Valid login succeeded; landed on account/account");
    }

    // ==================================================================
    // TC_097 – Email case-insensitivity
    // ==================================================================
    @Test(priority = 5, description = "TC_097 – Email login is case-insensitive")
    public void testLoginEmailCaseInsensitivity() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_097: Email case-insensitivity");
        logWriter.logMessage("Email (uppercase): " + primaryEmail.toUpperCase());
        logWriter.logMessage("========================================");

        String upper = primaryEmail.toUpperCase();
        boolean ok = loginPage.login(upper, primaryPassword);

        Assert.assertFalse(loginPage.isErrorMessageDisplayed(),
                "No error should appear with uppercase email");
        Assert.assertTrue(ok, "Login should succeed with uppercase email");
        Assert.assertTrue(loginPage.isUserLoggedIn());
        Assert.assertTrue(loginPage.getCurrentUrl().contains("account/account"),
                "Must land on account/account, was: " + loginPage.getCurrentUrl());

        logWriter.logRegistration(upper, true,
                "✓ Uppercase email authenticated successfully");
    }

    // ==================================================================
    // TC_098 – Wrong password → generic warning
    // ==================================================================
    @Test(priority = 6, description = "TC_098 – Wrong password shows generic warning")
    public void testLoginInvalidPasswordGenericError() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_098: Wrong password");
        logWriter.logMessage("Email: " + primaryEmail + " | Password: WrongPassword999");
        logWriter.logMessage("========================================");

        boolean ok = loginPage.login(primaryEmail, "WrongPassword999");
        String err = loginPage.getErrorMessage();
        logWriter.logMessage("   Error captured: '" + err + "'");

        Assert.assertFalse(ok, "Wrong password must not authenticate");
        Assert.assertFalse(loginPage.isUserLoggedIn());
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error message must appear");
        Assert.assertTrue(err.contains("No match for E-Mail Address and/or Password"),
                "Generic warning must be shown, was: '" + err + "'");
        Assert.assertTrue(loginPage.getCurrentUrl().contains("account/login"),
                "Must remain on login page");

        logWriter.logRegistration(primaryEmail, true,
                "✓ Wrong password rejected with generic warning: " + err);
    }

    // ==================================================================
    // TC_099 – Non-existent email → identical warning
    // ==================================================================
    @Test(priority = 7, description = "TC_099 – Non-existent email shows identical generic warning")
    public void testLoginNonExistentEmailIdenticalError() {
        String fakeEmail = "nonexistent_" + System.currentTimeMillis() + "@domain.com";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_099: Non-existent email");
        logWriter.logMessage("Email: " + fakeEmail);
        logWriter.logMessage("========================================");

        boolean ok = loginPage.login(fakeEmail, "SomePassword!");
        String err = loginPage.getErrorMessage();
        logWriter.logMessage("   Error captured: '" + err + "'");

        Assert.assertFalse(ok, "Non-existent email must not authenticate");
        Assert.assertFalse(loginPage.isUserLoggedIn(), "No session must be created");
        Assert.assertTrue(err.contains("No match for E-Mail Address and/or Password"),
                "Identical generic warning must appear, was: '" + err + "'");

        logWriter.logRegistration(fakeEmail, true,
                "✓ Non-existent email rejected with same generic warning: " + err);
    }

    // ==================================================================
    // TC_100 – SQL injection
    // ==================================================================
    @Test(priority = 8, description = "TC_100 – SQL-injection payload is safely rejected")
    public void testLoginSqlInjectionRejected() {
        String emailPayload = "admin' OR '1'='1'--";
        String passPayload  = "' OR '1'='1";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_100: SQL injection");
        logWriter.logMessage("Email payload:    " + emailPayload);
        logWriter.logMessage("Password payload: " + passPayload);
        logWriter.logMessage("========================================");

        boolean ok = loginPage.login(emailPayload, passPayload);
        String err = loginPage.getErrorMessage();
        logWriter.logMessage("   Error captured: '" + err + "'");

        Assert.assertFalse(ok, "SQL-injection payload must NOT authenticate");
        Assert.assertFalse(loginPage.isUserLoggedIn());
        Assert.assertTrue(loginPage.getCurrentUrl().contains("account/login"),
                "Must stay on login page, was: " + loginPage.getCurrentUrl());
        Assert.assertTrue(err.contains("No match"),
                "Generic warning must appear, was: '" + err + "'");

        String src = driver.getPageSource();
        Assert.assertNotNull(src);
        Assert.assertFalse(src.contains("Fatal error"),   "No PHP fatal error may leak");
        Assert.assertFalse(src.contains("SQLSTATE"),      "No SQL details may leak");
        Assert.assertFalse(src.contains("Warning: mysqli"), "No mysqli warning may leak");

        Assert.assertTrue(driver.findElement(By.id("input-email")).isDisplayed(),
                "Login form must still render (server did not crash)");

        logWriter.logRegistration(emailPayload, true,
                "✓ SQL payload rejected safely — no auth bypass, no error leak");
    }

    // ==================================================================
    // TC_151 – Errors don't expose secrets
    // ==================================================================
    @Test(priority = 9, description = "TC_151 – Login error does not expose secrets")
    public void testLoginErrorsDoNotExposeSecrets() {
        String secretPwd = "S3cr3t_" + System.currentTimeMillis();

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_151: Error does not expose secrets");
        logWriter.logMessage("========================================");

        boolean ok = loginPage.login(primaryEmail, secretPwd);
        String err = loginPage.getErrorMessage();
        String src = driver.getPageSource();

        Assert.assertFalse(ok);
        Assert.assertTrue(loginPage.isErrorMessageDisplayed());
        Assert.assertTrue(err.contains("No match"),
                "Error must be the generic one, was: '" + err + "'");

        // No password leak in error text
        Assert.assertFalse(err.contains(secretPwd),
                "Error must not echo the password");
        // No technical diagnostics
        Assert.assertFalse(err.toLowerCase().contains("exception"),
                "Error must not include exception text");
        Assert.assertFalse(err.toLowerCase().contains("stack trace"),
                "Error must not include a stack trace");
        Assert.assertFalse(err.toLowerCase().contains("sql"),
                "Error must not mention SQL");

        // No technical leak in page source either
        Assert.assertFalse(src.contains("SQLSTATE"),       "No SQL leak in page source");
        Assert.assertFalse(src.contains("Fatal error"),    "No PHP fatal error in page source");
        Assert.assertFalse(src.contains("Warning: mysqli"), "No mysqli warning in page source");
        Assert.assertFalse(src.contains(secretPwd),
                "Password must not appear anywhere in page source");

        logWriter.logRegistration(primaryEmail, true,
                "✓ Error is generic; no password/SQL/stack trace leak");
    }

    // ==================================================================
    // TC_095 – 'login page' link on Register
    // ==================================================================
    @Test(priority = 10, description = "TC_095 – 'login page' link on Register redirects to Login")
    public void testLoginPageLinkOnRegister() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_095: 'login page' link on Register");
        logWriter.logMessage("========================================");

        loginPage.clickLoginPageLinkOnRegisterPage();
        String url = loginPage.getCurrentUrl();
        logWriter.logMessage("   Landing URL: " + url);

        Assert.assertTrue(url.contains("account/login"),
                "Clicking 'login page' must redirect to account/login, was: " + url);
        Assert.assertTrue(loginPage.isLoginFormVisible(),
                "Login form must render after redirect");

        logWriter.logRegistration("login page link", true,
                "✓ 'login page' link redirected to Login form");
    }

    // ==================================================================
    // TC_101 – New Customer Continue → Register
    // ==================================================================
    @Test(priority = 11, description = "TC_101 – New Customer Continue navigates to Register")
    public void testNewCustomerContinueRedirectsToRegister() {
        logWriter.logMessage("\n=== New Customer Continue ===");
        loginPage.navigateToLoginPage();
        loginPage.clickNewCustomerContinue();

        Assert.assertTrue(loginPage.getCurrentUrl().contains("account/register"),
                "Should redirect to account/register, was: " + loginPage.getCurrentUrl());
        Assert.assertTrue(driver.findElement(By.id("input-firstname")).isDisplayed(),
                "Register form must render First Name field");
        logWriter.logMessage("✓ Redirected to Register page");
    }

    // ==================================================================
    // TC_108 – Logout from dropdown
    // ==================================================================
    @Test(priority = 12, description = "TC_108 – Logout via My Account dropdown")
    public void testLogoutFromHeaderDropdown() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_108: Logout from dropdown");
        logWriter.logMessage("========================================");

        Assert.assertTrue(loginPage.login(primaryEmail, primaryPassword),
                "Pre-condition: must be able to log in");
        Assert.assertTrue(loginPage.isUserLoggedIn(), "Pre-condition: session active");

        loginPage.clickLogoutFromDropdown();

        Assert.assertTrue(driver.findElements(By.linkText("Logout")).isEmpty(),
                "Logout link must be gone after logout");

        String url = loginPage.getCurrentUrl();
        logWriter.logMessage("   URL after logout: " + url);
        Assert.assertTrue(
                url.contains("account/logout") || url.endsWith("/") || url.contains("common/home"),
                "Should land on logout confirmation or home, was: " + url);
        Assert.assertFalse(driver.findElements(By.linkText("Login")).isEmpty(),
                "'Login' link must be visible again in the top nav after logout");

        logWriter.logRegistration(primaryEmail, true,
                "✓ Logout via dropdown succeeded, session terminated");
    }

    // ==================================================================
    // TC_109 – Browser Back after logout
    // ==================================================================
    @Test(priority = 13, description = "TC_109 – Browser Back after logout does not restore session")
    public void testBrowserBackAfterLogout() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_109: Browser Back after logout");
        logWriter.logMessage("========================================");

        Assert.assertTrue(loginPage.login(primaryEmail, primaryPassword),
                "Pre-condition: must be able to log in");

        loginPage.navigateTo(
                "http://localhost/opencart-4.1.0.4/upload/index.php?route=account/account");

        loginPage.logout();
        Assert.assertFalse(loginPage.isUserLoggedIn(),
                "Must be logged out before testing browser Back");

        driver.navigate().back();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> !d.getCurrentUrl().contains("account/account"));
        } catch (Exception ignored) {}

        Assert.assertFalse(loginPage.isUserLoggedIn(),
                "Browser Back must NOT restore an authenticated session");
        String url = loginPage.getCurrentUrl();
        Assert.assertTrue(
                url.contains("account/login") || url.contains("common/home") || url.endsWith("/"),
                "Back navigation must be redirected away from protected page, was: " + url);

        logWriter.logRegistration(primaryEmail, true,
                "✓ Browser Back did not restore session, was: " + url);
    }

    // ==================================================================
    // TC_106 – Anonymous sidebar redirects
    // ==================================================================
    @Test(priority = 14, description = "TC_106 – Protected sidebar links redirect to Login when anonymous")
    public void testUnauthorizedSidebarRedirectsToLogin() {
        String[] protectedLinks = {
                "My Account", "Address Book", "Wish List", "Order History",
                "Downloads", "Subscriptions", "Reward Points", "Returns",
                "Transactions", "Newsletter"
        };
        SoftAssert softAssert = new SoftAssert();

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_106: Anonymous sidebar redirects");
        logWriter.logMessage("========================================");

        for (String link : protectedLinks) {
            try {
                loginPage.navigateToLoginPage();
                loginPage.clickSidebarLink(link);
                String url = loginPage.getCurrentUrl();
                logWriter.logMessage("   '" + link + "' → " + url);
                softAssert.assertTrue(url.contains("account/login"),
                        "'" + link + "' should redirect to login, was: " + url);
                softAssert.assertFalse(loginPage.isUserLoggedIn(),
                        "'" + link + "' must not grant access to a guest");
            } catch (Exception e) {
                softAssert.fail("Error testing '" + link + "': " + e.getMessage());
            }
        }
        softAssert.assertAll();
        logWriter.logRegistration("sidebar links (anonymous)", true,
                "✓ All protected links redirected to Login when anonymous");
    }

    // ==================================================================
    // TC_107 – Authenticated sidebar navigation
    // ==================================================================
    @Test(priority = 15, description = "TC_107 – Protected sidebar links load when authenticated")
    public void testAuthenticatedSidebarNavigation() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_107: Authenticated sidebar navigation");
        logWriter.logMessage("========================================");

        Assert.assertTrue(loginPage.login(primaryEmail, primaryPassword),
                "Pre-condition: must be able to log in");

        String[][] expectations = {
                {"Order History", "account/order"},
                {"Address Book",  "account/address"},
                {"Newsletter",    "account/newsletter"}
        };
        SoftAssert softAssert = new SoftAssert();
        for (String[] pair : expectations) {
            try {
                loginPage.clickSidebarLink(pair[0]);
                String url = loginPage.getCurrentUrl();
                logWriter.logMessage("   '" + pair[0] + "' → " + url);
                softAssert.assertTrue(url.contains(pair[1]),
                        "'" + pair[0] + "' should load " + pair[1] + ", was: " + url);
                softAssert.assertFalse(url.contains("account/login"),
                        "'" + pair[0] + "' must NOT bounce to login when authenticated");
            } catch (Exception e) {
                softAssert.fail("Error navigating to '" + pair[0] + "': " + e.getMessage());
            }
        }
        softAssert.assertAll();
        logWriter.logRegistration(primaryEmail, true,
                "✓ All sidebar sections rendered when authenticated");
    }

    // ==================================================================
    // TC_149 – Login redirect without loop
    // ==================================================================
    @Test(priority = 16, description = "TC_149 – Login from protected URL has no redirect loop")
    public void testLoginRedirectNoLoop() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_149: Login redirect, no loop");
        logWriter.logMessage("========================================");

        // Anonymous visit to protected URL must redirect to login
        loginPage.navigateTo(
                "http://localhost/opencart-4.1.0.4/upload/index.php?route=account/order");
        Assert.assertTrue(loginPage.getCurrentUrl().contains("account/login"),
                "Anonymous access to account/order must redirect to login, was: "
                        + loginPage.getCurrentUrl());
        logWriter.logMessage("   ✓ Protected URL redirected to login");

        // Login → lands on a documented page (account/account), no loop
        boolean ok = loginPage.login(primaryEmail, primaryPassword);
        String url = loginPage.getCurrentUrl();
        logWriter.logMessage("   After login URL: " + url);

        Assert.assertTrue(ok, "Login must succeed");
        Assert.assertFalse(url.contains("account/login"),
                "After login must NOT still be on login page");
        Assert.assertTrue(url.contains("account/"),
                "After login must land on a documented account page, was: " + url);

        logWriter.logRegistration(primaryEmail, true,
                "✓ Logged in from protected URL; landed on " + url + " (no loop)");
    }
}