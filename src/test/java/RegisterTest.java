import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.LoginPage;
import pages.RegisterPage;
import utils.ExcelReader;
import utils.WaitHelper;

import java.util.List;
import java.util.Map;

/**
 * User Registration Test
 * Reads test data from Excel (positive + negative), plus dedicated tests for
 * scenarios requiring custom UI interaction (privacy toggle, newsletter,
 * duplicate email, injection safety, password leakage, field association).
 */
public class RegisterTest extends BaseTest {

    private RegisterPage registerPage;
    private LoginPage    loginPage;
    private ExcelReader  excelReader;
    private WaitHelper   waitHelper;

    @BeforeClass
    public void setupTest() {
        registerPage = new RegisterPage(driver);
        loginPage    = new LoginPage(driver);
        waitHelper   = new WaitHelper(driver);
        excelReader  = new ExcelReader("src/main/resources/testData.xlsx");
        logWriter.logSection("USER REGISTRATION TEST");
    }

    @BeforeMethod
    public void ensureCleanState() {
        try { loginPage.logout(); } catch (Exception ignored) {}
    }

    // ==================================================================
    // Data-driven: TC_088, TC_089, TC_090, TC_122–126, TC_128
    // ==================================================================
    @Test(priority = 1, description = "Data-driven registration (positive & negative)")
    public void testRegister() {
        List<Map<String, String>> users = excelReader.readRegisterData();

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("DATA-DRIVEN REGISTRATION TEST");
        logWriter.logMessage("Test rows to run: " + users.size());
        logWriter.logMessage("========================================");

        SoftAssert softAssert = new SoftAssert();
        int passed = 0, failed = 0;

        for (int i = 0; i < users.size(); i++) {
            Map<String, String> user = users.get(i);
            String fn = val(user.get("First Name"));
            String ln = val(user.get("Last Name"));
            String em = val(user.get("E-Mail"));
            String pw = val(user.get("Password"));
            String exp = val(user.get("Expected Result")).toLowerCase();
            String expErr = val(user.get("Expected Error Contains"));
            boolean expectSuccess = exp.isEmpty() || exp.startsWith("succ");

            logWriter.logMessage("\n--- Case " + (i + 1) + "/" + users.size()
                    + " | expected=" + (expectSuccess ? "SUCCESS" : "FAILURE")
                    + " | FirstName='" + fn + "' | LastName='" + ln
                    + "' | Email='" + em + "' | Password='" + pw + "' ---");
            System.out.println("\n=== Case " + (i + 1) + "/" + users.size()
                    + " | " + (expectSuccess ? "EXPECTED SUCCESS" : "EXPECTED FAILURE")
                    + " | " + (em.isEmpty() ? "<empty email>" : em) + " ===");

            try {
                registerPage.navigateToRegisterPage();
                boolean registered = registerPage.registerUser(fn, ln, em, pw);
                String err = registerPage.getLastRegistrationError();

                if (expectSuccess) {
                    if (registered) {
                        String okMsg = registerPage.getSuccessMessage();
                        logWriter.logRegistration(em, true, "✓ Registered successfully: " + okMsg);
                        System.out.println("✓ Registered: " + em);
                        passed++;
                    } else if (err.toLowerCase().contains("already registered")) {
                        logWriter.logRegistration(em, false,
                                "⚠ Already registered (soft-pass on re-run): " + err);
                        System.out.println("⚠ Already registered (soft-pass): " + em);
                        passed++;
                    } else {
                        String msg = "Expected SUCCESS but got error: " + err;
                        logWriter.logRegistration(em, false, "✗ " + msg);
                        System.out.println("✗ " + msg);
                        softAssert.fail("Case " + (i + 1) + ": " + msg);
                        failed++;
                    }
                } else {
                    if (registered) {
                        String msg = "Expected FAILURE but user was registered";
                        logWriter.logRegistration(em, false, "✗ " + msg);
                        System.out.println("✗ " + msg);
                        softAssert.fail("Case " + (i + 1) + ": " + msg);
                        failed++;
                    } else if (err.isEmpty()) {
                        String msg = "Expected FAILURE with error, no error shown";
                        logWriter.logRegistration(em, false, "✗ " + msg);
                        System.out.println("✗ " + msg);
                        softAssert.fail("Case " + (i + 1) + ": " + msg);
                        failed++;
                    } else if (!expErr.isEmpty()
                            && !err.toLowerCase().contains(expErr.toLowerCase())) {
                        String msg = "Expected error containing '" + expErr + "', got: " + err;
                        logWriter.logRegistration(em, false, "⚠ " + msg);
                        System.out.println("⚠ " + msg);
                        softAssert.fail("Case " + (i + 1) + ": " + msg);
                        failed++;
                    } else {
                        logWriter.logRegistration(em, true, "✓ Expected failure reproduced: " + err);
                        System.out.println("✓ Expected failure reproduced for: "
                                + (em.isEmpty() ? "<empty email>" : em));
                        passed++;
                    }
                }
            } catch (Exception e) {
                String msg = "Exception in case " + (i + 1) + ": " + e.getMessage();
                logWriter.logRegistration(em, false, "✗ " + msg);
                System.out.println("✗ " + msg);
                softAssert.fail(msg);
                failed++;
            }
        }

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("DATA-DRIVEN REGISTRATION SUMMARY");
        logWriter.logMessage("Total cases:  " + users.size());
        logWriter.logMessage("Passed:       " + passed);
        logWriter.logMessage("Failed:       " + failed);
        logWriter.logMessage("========================================");

        System.out.println("\n========================================");
        System.out.println("SUMMARY: " + passed + " passed, " + failed + " failed of "
                + users.size() + " total");
        System.out.println("========================================");

        softAssert.assertAll();
    }

    // ==================================================================
    // TC_087 / TC_122 – All required empty → four field-level errors
    // ==================================================================
    @Test(priority = 2, description = "TC_087/TC_122 – All required fields empty triggers per-field errors")
    public void testAllRequiredFieldsEmpty() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_087/TC_122: All required fields empty");
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_087/TC_122: All fields empty ===");

        registerPage.navigateToRegisterPage();
        registerPage.fillRegistrationForm("", "", "", "");
        registerPage.acceptPrivacyPolicy();
        registerPage.clickContinue();

        String fnErr = registerPage.getFieldErrorMessage("firstname");
        String lnErr = registerPage.getFieldErrorMessage("lastname");
        String emErr = registerPage.getFieldErrorMessage("email");
        String pwErr = registerPage.getFieldErrorMessage("password");

        logWriter.logMessage("First Name error: '" + fnErr + "'");
        logWriter.logMessage("Last Name error:  '" + lnErr + "'");
        logWriter.logMessage("E-Mail error:     '" + emErr + "'");
        logWriter.logMessage("Password error:   '" + pwErr + "'");

        Assert.assertTrue(fnErr.contains("First Name must be between"),
                "First Name error must appear, got: " + fnErr);
        Assert.assertTrue(lnErr.contains("Last Name must be between"),
                "Last Name error must appear, got: " + lnErr);
        Assert.assertTrue(emErr.contains("E-Mail"),
                "Email error must appear, got: " + emErr);
        Assert.assertTrue(pwErr.contains("Password must be between"),
                "Password error must appear, got: " + pwErr);
        Assert.assertFalse(registerPage.isSuccessMessageDisplayed(),
                "Registration must not succeed");
        Assert.assertTrue(registerPage.isStillOnRegisterForm(),
                "Must remain on register page");
        Assert.assertTrue(registerPage.isFormFieldsVisible(),
                "Form fields must remain visible");

        logWriter.logMessage("✓ All four field-level errors shown as expected");
        logWriter.logRegistration("<empty>", true, "✓ All-required-empty case reproduced correctly");
    }

    // ==================================================================
    // TC_091 – Privacy OFF → warning + no account
    // ==================================================================
    @Test(priority = 3, description = "TC_091 – Registration blocked without Privacy Policy")
    public void testPrivacyPolicyNotAccepted() {
        String email = "noprivacy_" + System.currentTimeMillis() + "@example.com";
        String pwd   = "ValidPass123";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_091: Privacy Policy NOT accepted");
        logWriter.logMessage("Email: " + email);
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_091: Privacy not accepted ===");

        registerPage.navigateToRegisterPage();
        registerPage.fillRegistrationForm("John", "Doe", email, pwd);
        // Intentionally NOT calling acceptPrivacyPolicy()
        registerPage.clickContinue();

        String warning = registerPage.getLastRegistrationError();
        logWriter.logMessage("Warning captured: '" + warning + "'");

        Assert.assertTrue(warning.toLowerCase().contains("privacy policy"),
                "Warning must mention Privacy Policy, got: '" + warning + "'");
        Assert.assertFalse(registerPage.isSuccessMessageDisplayed(),
                "Registration must not succeed without privacy acceptance");
        Assert.assertTrue(registerPage.isStillOnRegisterForm(),
                "Must remain on register page");

        logWriter.logRegistration(email, true,
                "✓ Registration blocked with warning: " + warning);

        // Verify no account was actually created
        logWriter.logMessage("Verifying account was NOT created via login attempt...");
        boolean loginOk = loginPage.login(email, pwd);
        Assert.assertFalse(loginOk, "Account must NOT have been created");
        logWriter.logMessage("✓ Login refused — account was not created as expected");
        logWriter.logRegistration(email, true, "✓ No account persisted (verified via login)");
    }

    // ==================================================================
    // TC_092 – Privacy OFF → ON → success
    // ==================================================================
    @Test(priority = 4, description = "TC_092 – Registration succeeds after enabling Privacy")
    public void testPrivacyPolicyToggleRecovery() {
        String email = "recovery_" + System.currentTimeMillis() + "@example.com";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_092: Privacy toggle recovery");
        logWriter.logMessage("Email: " + email);
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_092: Privacy toggle recovery ===");

        registerPage.navigateToRegisterPage();
        registerPage.fillRegistrationForm("John", "Doe", email, "ValidPass123");

        logWriter.logMessage("Step 1: Submit WITHOUT privacy (expect warning)");
        registerPage.clickContinue();
        String firstWarning = registerPage.getLastRegistrationError();
        logWriter.logMessage("   Warning captured: '" + firstWarning + "'");
        Assert.assertTrue(firstWarning.toLowerCase().contains("privacy policy"),
                "First submit must show privacy warning");
        Assert.assertTrue(registerPage.isStillOnRegisterForm(),
                "Must remain on register page after first rejection");

        logWriter.logMessage("Step 2: Accept privacy and resubmit");
        registerPage.acceptPrivacyPolicy();
        registerPage.clickContinue();

        boolean ok = registerPage.isSuccessMessageDisplayed();
        logWriter.logMessage("   Success visible: " + ok);
        Assert.assertTrue(ok, "Registration should succeed after privacy acceptance");

        logWriter.logRegistration(email, true,
                "✓ Registration succeeded after enabling Privacy Policy");
    }

//    // ==================================================================
//    // TC_093 – Newsletter ON
//    // ==================================================================
//    @Test(priority = 5, description = "TC_093 – Newsletter ON records subscription")
//    public void testNewsletterOptIn() {
//        String email = "nl_on_" + System.currentTimeMillis() + "@example.com";
//
//        logWriter.logMessage("\n========================================");
//        logWriter.logMessage("TC_093: Newsletter ON");
//        logWriter.logMessage("Email: " + email);
//        logWriter.logMessage("========================================");
//        System.out.println("\n=== TC_093: Newsletter ON ===");
//
//        registerPage.navigateToRegisterPage();
//        boolean registered = registerPage.registerUserWithOptions(
//                "John", "Doe", email, "ValidPass123",
//                true, true, true);
//        logWriter.logMessage("Registration result: " + (registered ? "SUCCESS" : "FAILED"));
//        Assert.assertTrue(registered, "Registration with newsletter ON must succeed");
//
//        logWriter.logMessage("Navigating to Newsletter page to check subscription status...");
//        registerPage.navigateToNewsletterPage();
//        String url = driver.getCurrentUrl();
//        logWriter.logMessage("   URL: " + url);
//
//        boolean subscribed = registerPage.isNewsletterSubscribed();
//        logWriter.logMessage("   Newsletter checkbox selected: " + subscribed);
//        Assert.assertTrue(subscribed,
//                "Newsletter must be 'Yes' on /account/newsletter after opt-in");
//        Assert.assertTrue(url.contains("account/newsletter"),
//                "Must be on newsletter page, was: " + url);
//
//        logWriter.logRegistration(email, true,
//                "✓ Newsletter subscription recorded (checkbox checked on newsletter page)");
//
//        registerPage.logoutAfterRegistration();
//        logWriter.logMessage("✓ Logged out after verification");
//    }
//
//    // ==================================================================
//    // TC_094 – Newsletter default OFF
//    // ==================================================================
//    @Test(priority = 6, description = "TC_094 – Newsletter default OFF remains unsubscribed")
//    public void testNewsletterDefaultOff() {
//        String email = "nl_off_" + System.currentTimeMillis() + "@example.com";
//
//        logWriter.logMessage("\n========================================");
//        logWriter.logMessage("TC_094: Newsletter default OFF");
//        logWriter.logMessage("Email: " + email);
//        logWriter.logMessage("========================================");
//        System.out.println("\n=== TC_094: Newsletter default OFF ===");
//
//        registerPage.navigateToRegisterPage();
//        boolean defaultOn = registerPage.isNewsletterSubscriptionOn();
//        logWriter.logMessage("Default newsletter toggle state: " + (defaultOn ? "ON" : "OFF"));
//        Assert.assertFalse(defaultOn, "Newsletter toggle must default to OFF");
//
//        boolean registered = registerPage.registerUserWithOptions(
//                "Jane", "Smith", email, "ValidPass123",
//                false, true, true);
//        logWriter.logMessage("Registration result: " + (registered ? "SUCCESS" : "FAILED"));
//        Assert.assertTrue(registered, "Registration without newsletter must succeed");
//
//        logWriter.logMessage("Navigating to Newsletter page to check status...");
//        registerPage.navigateToNewsletterPage();
//        String url = driver.getCurrentUrl();
//        logWriter.logMessage("   URL: " + url);
//
//        boolean subscribed = registerPage.isNewsletterSubscribed();
//        logWriter.logMessage("   Newsletter checkbox selected: " + subscribed);
//        Assert.assertFalse(subscribed,
//                "Newsletter must be 'No' on /account/newsletter when toggle left OFF");
//
//        logWriter.logRegistration(email, true,
//                "✓ Newsletter subscription correctly OFF (checkbox unchecked)");
//
//        registerPage.logoutAfterRegistration();
//        logWriter.logMessage("✓ Logged out after verification");
//    }

    // ==================================================================
    // TC_127 – Duplicate email
    // ==================================================================
    @Test(priority = 7, description = "TC_127 – Duplicate email is rejected")
    public void testDuplicateEmailPrevented() {
        String email = "dup_" + System.currentTimeMillis() + "@example.com";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_127: Duplicate email prevention");
        logWriter.logMessage("Email: " + email);
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_127: Duplicate email ===");

        logWriter.logMessage("Step 1: First registration (must succeed)");
        registerPage.navigateToRegisterPage();
        boolean first = registerPage.registerUser("John", "Doe", email, "ValidPass123");
        logWriter.logMessage("   First registration: " + (first ? "SUCCESS" : "FAILED"));
        Assert.assertTrue(first, "First registration must succeed");

        logWriter.logMessage("Step 2: Second registration with same email (must fail)");
        registerPage.navigateToRegisterPage();
        boolean second = registerPage.registerUser("Jane", "Smith", email, "OtherPwd456");
        logWriter.logMessage("   Second registration: " + (second ? "UNEXPECTED SUCCESS" : "correctly FAILED"));
        Assert.assertFalse(second, "Duplicate registration must fail");

        String err = registerPage.getLastRegistrationError();
        logWriter.logMessage("   Error captured: '" + err + "'");
        Assert.assertTrue(err.toLowerCase().contains("already registered"),
                "Error must mention 'already registered', got: '" + err + "'");

        logWriter.logRegistration(email, true,
                "✓ Duplicate registration prevented: " + err);
    }

    // ==================================================================
    // TC_130 – Leading/trailing spaces
    // ==================================================================
    @Test(priority = 8, description = "TC_130 – Leading/trailing spaces handled consistently")
    public void testLeadingTrailingSpaces() {
        String base    = "spaces_" + System.currentTimeMillis();
        String padded  = "  " + base + "@example.com  ";
        String trimmed = base + "@example.com";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_130: Leading/trailing spaces");
        logWriter.logMessage("Padded email:  '" + padded + "'");
        logWriter.logMessage("Trimmed email: '" + trimmed + "'");
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_130: Leading/trailing spaces ===");

        registerPage.navigateToRegisterPage();
        boolean registered = registerPage.registerUser("  John  ", "  Doe  ", padded, "ValidPass123");
        logWriter.logMessage("Registration with padded input: "
                + (registered ? "ACCEPTED (trimmed)" : "REJECTED"));

        if (registered) {
            logWriter.logMessage("Testing login with trimmed email...");
            loginPage.navigateToLoginPage();
            boolean loginOk = loginPage.login(trimmed, "ValidPass123");
            logWriter.logMessage("   Login with trimmed email: "
                    + (loginOk ? "SUCCESS" : "FAILED"));
            Assert.assertTrue(loginOk,
                    "If spaces were trimmed on registration, trimmed email must authenticate");
            logWriter.logRegistration(trimmed, true,
                    "✓ Spaces trimmed consistently — trimmed email authenticates");
        } else {
            String err = registerPage.getLastRegistrationError();
            logWriter.logMessage("   Rejection reason: '" + err + "'");
            Assert.assertFalse(err.isEmpty(),
                    "If registration failed, an error must be shown");
            logWriter.logRegistration(padded, true,
                    "✓ Spaces rejected with error: " + err);
        }
    }

    // ==================================================================
    // TC_131 – Email case handling
    // ==================================================================
    @Test(priority = 9, description = "TC_131 – Email case handling is consistent")
    public void testEmailCaseHandlingConsistency() {
        String lower = "case_" + System.currentTimeMillis() + "@example.com";
        String upper = lower.toUpperCase();

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_131: Email case consistency");
        logWriter.logMessage("Lowercase: " + lower);
        logWriter.logMessage("Uppercase: " + upper);
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_131: Email case consistency ===");

        logWriter.logMessage("Step 1: Register with lowercase email");
        registerPage.navigateToRegisterPage();
        boolean first = registerPage.registerUser("John", "Doe", lower, "ValidPass123");
        logWriter.logMessage("   Lowercase registration: " + (first ? "SUCCESS" : "FAILED"));
        Assert.assertTrue(first, "Lowercase registration must succeed");

        logWriter.logMessage("Step 2: Register with uppercase variant");
        registerPage.navigateToRegisterPage();
        boolean second = registerPage.registerUser("John", "Doe", upper, "ValidPass123");
        logWriter.logMessage("   Uppercase registration: "
                + (second ? "UNEXPECTED SUCCESS" : "correctly REJECTED"));
        Assert.assertFalse(second,
                "Uppercase version of existing email must fail");

        String err = registerPage.getLastRegistrationError();
        logWriter.logMessage("   Error captured: '" + err + "'");
        Assert.assertTrue(err.toLowerCase().contains("already registered"),
                "Error must indicate duplicate email, got: '" + err + "'");

        logWriter.logRegistration(upper, true,
                "✓ Case-insensitive duplicate detection working: " + err);
    }

    // ==================================================================
    // TC_132 – Markup safe rendering
    // ==================================================================
    @Test(priority = 10, description = "TC_132 – Markup input is safely rendered")
    public void testMarkupSafeRendering() {
        String marker = "<b>QA_CANARY</b>";
        String email  = "markup_" + System.currentTimeMillis() + "@example.com";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_132: Markup safe rendering");
        logWriter.logMessage("Marker: " + marker);
        logWriter.logMessage("Email:  " + email);
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_132: Markup safe rendering ===");

        registerPage.navigateToRegisterPage();
        boolean registered = registerPage.registerUserWithOptions(
                marker, "Doe", email, "ValidPass123", false, true, true);
        logWriter.logMessage("Registration with markup: "
                + (registered ? "ACCEPTED" : "REJECTED"));

        if (registered) {
            logWriter.logMessage("Navigating to account edit page to inspect rendering...");
            registerPage.navigateTo(
                    "http://localhost/opencart-4.1.0.4/upload/index.php?route=account/edit");
            waitHelper.customWait(800);

            String bodyText = driver.findElement(By.tagName("body")).getText();
            String src      = driver.getPageSource();

            boolean renderedAsText = bodyText.contains("<b>QA_CANARY</b>");
            boolean renderedAsHtml = bodyText.contains("QA_CANARY") && !renderedAsText;
            logWriter.logMessage("   Rendered as literal text: " + renderedAsText);
            logWriter.logMessage("   Rendered as HTML (unsafe): " + renderedAsHtml);

            Assert.assertFalse(renderedAsHtml,
                    "Markup must be encoded as text, not rendered as HTML");
            Assert.assertFalse(src.contains("<b>QA_CANARY</b>") && !src.contains("&lt;b&gt;"),
                    "Page source must escape the markup");

            logWriter.logRegistration(email, true,
                    "✓ Markup encoded as text — no HTML rendering");
            registerPage.logoutAfterRegistration();
        } else {
            String err = registerPage.getLastRegistrationError();
            logWriter.logMessage("   Rejection reason: '" + err + "'");
            Assert.assertFalse(err.isEmpty(),
                    "If markup was rejected, an error must be shown");
            logWriter.logRegistration(email, true,
                    "✓ Markup rejected with error: " + err);
        }
    }

    // ==================================================================
    // TC_133 – SQL metacharacters
    // ==================================================================
    @Test(priority = 11, description = "TC_133 – SQL metacharacters handled safely")
    public void testSqlMetacharactersSafe() {
        String email1 = "sql_punct_" + System.currentTimeMillis() + "@example.com";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_133: SQL metacharacters safety");
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_133: SQL metacharacters ===");

        logWriter.logMessage("Step 1: Register with legitimate apostrophe (O'Connor)");
        registerPage.navigateToRegisterPage();
        boolean punctOk = registerPage.registerUser("Patrick", "O'Connor", email1, "ValidPass123");
        logWriter.logMessage("   O'Connor registration: " + (punctOk ? "SUCCESS" : "FAILED"));
        Assert.assertTrue(punctOk, "Apostrophe must be accepted in names");

        String src1 = driver.getPageSource();
        Assert.assertFalse(src1.contains("SQLSTATE"),    "No SQL details may leak (punct case)");
        Assert.assertFalse(src1.contains("Fatal error"), "No PHP fatal error may leak (punct case)");
        logWriter.logMessage("   No SQL/PHP errors leaked for punctuation case");
        logWriter.logRegistration(email1, true,
                "✓ Apostrophe accepted, no SQL/PHP error leaked");

        logWriter.logMessage("Step 2: Attempt registration with SQL-looking payload");
        String payload = "a'; --";
        String email2  = "sql_payload_" + System.currentTimeMillis() + "@example.com";
        registerPage.navigateToRegisterPage();
        boolean payloadOk = registerPage.registerUser(payload, "Test", email2, "ValidPass123");
        logWriter.logMessage("   SQL-payload registration: "
                + (payloadOk ? "ACCEPTED (treated as plain text)" : "REJECTED"));

        String src2 = driver.getPageSource();
        Assert.assertFalse(src2.contains("SQLSTATE"),       "No SQL details may leak (payload case)");
        Assert.assertFalse(src2.contains("Fatal error"),    "No PHP fatal error may leak (payload case)");
        Assert.assertFalse(src2.contains("Warning: mysqli"), "No mysqli warning may leak (payload case)");
        logWriter.logMessage("   No SQL/mysqli errors leaked for payload case");

        registerPage.navigateToRegisterPage();
        Assert.assertTrue(driver.findElement(By.name("firstname")).isDisplayed(),
                "Register form must still render (server alive)");
        logWriter.logMessage("✓ Application remains responsive after payload attempt");
        logWriter.logRegistration(email2, true,
                "✓ SQL metacharacters handled without error/crash");
    }

    // ==================================================================
    // TC_134 – Password not leaked
    // ==================================================================
    @Test(priority = 12, description = "TC_134 – Password is not exposed in URL or visible feedback")
    public void testPasswordNotExposed() {
        String marker = "SecretMarkerXYZ_" + System.currentTimeMillis();
        String email  = "leak_" + System.currentTimeMillis() + "@example.com";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_134: Password not exposed in URL/errors");
        logWriter.logMessage("Email:  " + email);
        logWriter.logMessage("Marker: " + marker);
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_134: Password not exposed ===");

        registerPage.navigateToRegisterPage();
        registerPage.disableHtml5Validation();
        registerPage.fillRegistrationForm("John", "Doe", "not-an-email", marker);
        registerPage.acceptPrivacyPolicy();
        registerPage.clickContinue();

        String err = registerPage.getLastRegistrationError();
        logWriter.logMessage("   Error captured: '" + err + "'");
        Assert.assertTrue(!err.isEmpty(),
                "A validation error must appear for the invalid email");

        String url = driver.getCurrentUrl();
        String title = driver.getTitle();
        logWriter.logMessage("   URL:   " + url);
        logWriter.logMessage("   Title: " + title);

        Assert.assertFalse(url.contains(marker),   "URL must not contain password");
        Assert.assertFalse(err.contains(marker),   "Error must not echo password");
        Assert.assertFalse(title.contains(marker), "Title must not contain password");

        WebElement pwd = driver.findElement(By.name("password"));
        String type = pwd.getAttribute("type");
        logWriter.logMessage("   Password field type attribute: '" + type + "'");
        Assert.assertEquals(type, "password", "Password field must remain masked");

        logWriter.logRegistration(email, true,
                "✓ Password not exposed in URL, title, or error text");
    }

    // ==================================================================
    // TC_135 – Error / field association
    // ==================================================================
    @Test(priority = 13, description = "TC_135 – Error appears on the correct field only")
    public void testFieldErrorAssociation() {
        String ts = String.valueOf(System.currentTimeMillis());

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_135: Field-error association");
        logWriter.logMessage("========================================");
        System.out.println("\n=== TC_135: Field-error association ===");

        verifySingleFieldError("firstname", "", "John",
                "fe1_" + ts + "@example.com", "ValidPass123", "First Name must be between");
        verifySingleFieldError("lastname", "John", "",
                "fe2_" + ts + "@example.com", "ValidPass123", "Last Name must be between");
        verifySingleFieldError("email", "John", "Doe",
                "", "ValidPass123", "E-Mail");
        verifySingleFieldError("password", "John", "Doe",
                "fe3_" + ts + "@example.com", "", "Password must be between");

        logWriter.logRegistration("fe_*", true,
                "✓ Field-error association correct for all four fields");
    }

    private void verifySingleFieldError(String field, String fn, String ln,
                                        String em, String pw, String expected) {
        logWriter.logMessage("\n--- Verifying " + field + " error association ---");
        registerPage.navigateToRegisterPage();
        registerPage.fillRegistrationForm(fn, ln, em, pw);
        registerPage.acceptPrivacyPolicy();
        registerPage.clickContinue();

        String err = registerPage.getFieldErrorMessage(field);
        logWriter.logMessage("   Target field '" + field + "' error: '" + err + "'");
        Assert.assertTrue(err.toLowerCase().contains(expected.toLowerCase()),
                "Field '" + field + "' should show error containing '" + expected + "', got: '" + err + "'");

        for (String other : new String[]{"firstname", "lastname", "email", "password"}) {
            if (!other.equals(field)) {
                String otherErr = registerPage.getFieldErrorMessage(other);
                logWriter.logMessage("   Other field '" + other + "' error: '" + otherErr + "'");
                Assert.assertTrue(otherErr.isEmpty(),
                        "Field '" + other + "' must NOT show an error when only '" + field + "' is invalid");
            }
        }
        Assert.assertTrue(registerPage.isFormFieldsVisible(),
                "Form fields must remain visible when error shows");
        logWriter.logMessage("✓ Correct association: only '" + field + "' has an error");
    }

    private static String val(String v) { return v == null ? "" : v.trim(); }
}