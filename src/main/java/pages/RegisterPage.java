package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

public class RegisterPage extends BasePage {

    private static final String BASE_URL     = "http://localhost/opencart-4.1.0.4/upload/";
    private static final String REGISTER_URL = BASE_URL + "index.php?route=account/register";

    // ---- Form fields ----
    @FindBy(name = "firstname")     private WebElement firstNameInput;
    @FindBy(name = "lastname")      private WebElement lastNameInput;
    @FindBy(name = "email")         private WebElement emailInput;
    @FindBy(name = "password")      private WebElement passwordInput;
    @FindBy(name = "agree")         private WebElement privacyPolicyCheckbox;

    // The submit is a <button type="submit">Continue</button>, not <input value="Continue">
    @FindBy(xpath = "//button[@type='submit' and normalize-space(.)='Continue']")
    private WebElement continueButton;

    // Links shown on the success page / logout page (never on the register form)
    @FindBy(xpath = "//a[contains(normalize-space(text()), 'Continue')]")
    private WebElement continueButtonAfterSuccess;

    @FindBy(css = "a.btn.btn-primary")
    private WebElement continuePrimaryButton;

    // ---- Success markers ----
    @FindBy(xpath = "//div[contains(@class, 'alert-success')]")
    private WebElement successMessage;

    @FindBy(xpath = "//h1[contains(., 'Your Account Has Been Created')]")
    private WebElement accountCreatedHeader;

    @FindBy(xpath = "//div[@id='content']//p[contains(., 'successfully created') or contains(., 'successfully')]")
    private WebElement accountCreatedText;

    @FindBy(xpath = "//h1[contains(., 'Account Logout')]")
    private WebElement accountLogoutHeader;

    // ---- Session helpers ----
    @FindBy(linkText = "My Account") private WebElement myAccountLink;
    @FindBy(linkText = "Logout")     private WebElement logoutLink;

    public RegisterPage(WebDriver driver) {
        super(driver);
    }

    // ============================================================
    // Navigation
    // ============================================================
    public void navigateToRegisterPage() {
        navigateTo(REGISTER_URL);
        waitHelper.customWait(300);

        try {
            waitHelper.waitForElementToBeVisible(firstNameInput);
            System.out.println("✓ Registration form loaded successfully");
        } catch (Exception e) {
            System.out.println("⚠ Registration form not found, checking for an active session...");
            try {
                if (isElementDisplayed(logoutLink)) {
                    System.out.println("⚠ Active session detected, logging out...");
                    logoutAfterRegistration();
                    navigateTo(REGISTER_URL);
                    waitHelper.customWait(300);
                    waitHelper.waitForElementToBeVisible(firstNameInput);
                    System.out.println("✓ Registration form loaded after logout");
                } else {
                    throw e;
                }
            } catch (Exception ex) {
                System.out.println("✗ Critical error reaching registration form");
                throw ex;
            }
        }
        waitHelper.customWait(500);
    }

    // ============================================================
    // Form interaction
    // ============================================================
    public void fillRegistrationForm(String firstName, String lastName, String email, String password) {
        writeTextIfNotEmpty(firstNameInput, firstName);
        writeTextIfNotEmpty(lastNameInput,  lastName);
        writeTextIfNotEmpty(emailInput,     email);
        writeTextIfNotEmpty(passwordInput,  password);
    }

    private void writeTextIfNotEmpty(WebElement el, String value) {
        if (value != null && !value.isEmpty()) {
            writeText(el, value);
        }
    }

    /**
     * Bypass HTML5 validation so malformed input reaches the server and
     * produces a server-side field error (required for negative cases).
     */
    public void disableHtml5Validation() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var f = document.getElementById('form-register');" +
                            "if (f) f.setAttribute('novalidate','novalidate');"
            );
        } catch (Exception ignored) {}
    }

    public void acceptPrivacyPolicy() {
        try {
            scrollToElement(privacyPolicyCheckbox);
            if (!privacyPolicyCheckbox.isSelected()) {
                clickElement(privacyPolicyCheckbox);
            }
            if (!privacyPolicyCheckbox.isSelected()) {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();", privacyPolicyCheckbox);
            }
            if (privacyPolicyCheckbox.isSelected()) {
                System.out.println("✓ Privacy policy accepted");
            } else {
                System.out.println("⚠ Could not accept privacy policy checkbox");
            }
        } catch (Exception e) {
            System.out.println("⚠ Error accepting privacy policy: " + e.getMessage());
        }
    }

    public void clickContinue() {
        try { scrollToElement(continueButton); } catch (Exception ignored) {}

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                waitHelper.waitForElementToBeClickable(continueButton);
                clickElement(continueButton);
                waitHelper.customWait(500);

                boolean movedToSuccess = isOnSuccessPage() || isElementDisplayed(continueButtonAfterSuccess);
                boolean hasErrorNow    = isAnyErrorDisplayed();
                if (movedToSuccess || hasErrorNow) {
                    return;
                }
            } catch (Exception e) {
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", continueButton);
                } catch (Exception ignored) {}
            }
            waitHelper.customWait(800);
        }
    }

    // ============================================================
    // Error inspection — field-level + alert-level
    // ============================================================

    /**
     * Returns the text of an OpenCart field error div (#error-{field}), or "" if
     * none. The div always exists in the DOM; it becomes visible (display:block)
     * only when a validation error is set.
     */
    public String getFieldErrorMessage(String fieldName) {
        try {
            WebElement err = driver.findElement(By.id("error-" + fieldName));
            if (err.isDisplayed()) {
                String txt = err.getText();
                return txt != null ? txt.trim() : "";
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * Concatenates every visible validation message on the page.
     * Sources:
     *  - #error-firstname / -lastname / -email / -password / -agree  (field-level)
     *  - #alert .alert-danger / .alert-warning                        (global)
     */
    public String getAnyErrorMessage() {
        StringBuilder sb = new StringBuilder();

        for (String field : new String[]{"firstname", "lastname", "email", "password", "agree"}) {
            String err = getFieldErrorMessage(field);
            if (!err.isEmpty()) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(err);
            }
        }

        try {
            List<WebElement> alerts = driver.findElements(
                    By.cssSelector("#alert .alert-danger, #alert .alert-warning"));
            for (WebElement alert : alerts) {
                if (alert.isDisplayed()) {
                    String txt = alert.getText();
                    if (txt != null && !txt.trim().isEmpty()) {
                        if (sb.length() > 0) sb.append(" | ");
                        sb.append(txt.trim());
                    }
                }
            }
        } catch (Exception ignored) {}

        return sb.toString().trim();
    }

    public boolean isAnyErrorDisplayed() {
        return !getAnyErrorMessage().isEmpty();
    }

    /** Backwards-compatible alias, now field-error aware. */
    public boolean isErrorMessageDisplayed() {
        try {
            waitHelper.customWait(400);
            boolean displayed = isAnyErrorDisplayed();
            if (displayed) {
                System.out.println("⚠ Validation error detected: " + getAnyErrorMessage());
            }
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /** Backwards-compatible getter, now returns everything. */
    public String getErrorMessage() {
        return getAnyErrorMessage();
    }

    // ============================================================
    // Success detection
    // ============================================================
    public boolean isSuccessMessageDisplayed() {
        try {
            waitHelper.customWait(800);
            boolean displayed = isElementDisplayed(successMessage) || isOnSuccessPage();
            if (displayed) System.out.println("✓ Registration success detected");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isOnSuccessPage() {
        try { if (isElementDisplayed(accountCreatedHeader)) return true; } catch (Exception ignored) {}
        try { if (isElementDisplayed(accountCreatedText))   return true; } catch (Exception ignored) {}
        try { return waitHelper.waitForUrlContains("route=account/success"); } catch (Exception e) { return false; }
    }

    public String getSuccessMessage() {
        if (isSuccessMessageDisplayed()) {
            String msg = getText(successMessage);
            System.out.println("   Message: " + msg);
            return msg;
        }
        return "";
    }

    /** True if the user is still looking at the register form (no redirect to success). */
    public boolean isStillOnRegisterForm() {
        try {
            return getCurrentUrl().contains("route=account/register")
                    && firstNameInput.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // Post-success / post-logout helpers
    // ============================================================
    public void clickContinueAfterSuccess() {
        try {
            System.out.println("   → Looking for the Continue button on the confirmation page...");
            waitHelper.customWait(300);

            for (int i = 0; i < 3; i++) {
                if (!isOnSuccessPage()) return;

                try {
                    try {
                        waitHelper.waitForElementToBeClickable(continuePrimaryButton);
                        scrollToElement(continuePrimaryButton);
                        clickElement(continuePrimaryButton);
                    } catch (Exception ignore) {}

                    waitHelper.waitForElementToBeClickable(continueButtonAfterSuccess);
                    scrollToElement(continueButtonAfterSuccess);
                    clickElement(continueButtonAfterSuccess);
                } catch (Exception e1) {
                    try {
                        WebElement continueLink = driver.findElement(
                                By.xpath("//a[normalize-space(text())='Continue']"));
                        scrollToElement(continueLink);
                        continueLink.click();
                    } catch (Exception e2) {
                        try {
                            WebElement c = (continuePrimaryButton != null) ? continuePrimaryButton :
                                    driver.findElement(By.cssSelector("a.btn.btn-primary"));
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", c);
                        } catch (Exception ignored) {}
                    }
                }

                boolean leftSuccess = false;
                for (int w = 0; w < 10; w++) {
                    waitHelper.customWait(200);
                    if (!getCurrentUrl().contains("route=account/success")) {
                        leftSuccess = true;
                        break;
                    }
                }
                if (leftSuccess) {
                    System.out.println("✓ Left the success page");
                    return;
                }
            }

            System.out.println("   → Navigating directly to home as fallback...");
            navigateTo(BASE_URL);
            waitHelper.customWait(500);
        } catch (Exception e) {
            System.out.println("⚠ Error while leaving success page: " + e.getMessage());
        }
    }

    public void logoutAfterRegistration() {
        try {
            System.out.println("   → Logging out after registration...");

            try {
                waitHelper.waitForElementToBeClickable(myAccountLink);
                scrollToElement(myAccountLink);
                clickElement(myAccountLink);
                waitHelper.customWait(300);
            } catch (Exception ignored) {}

            try {
                waitHelper.waitForElementToBeClickable(logoutLink);
                scrollToElement(logoutLink);
                clickElement(logoutLink);
            } catch (Exception e) {
                System.out.println("⚠ Could not click Logout directly: " + e.getMessage());
            }

            boolean onLogout = waitForLogoutPage(5000);
            if (onLogout) {
                System.out.println("✓ Logout successful");
                try {
                    if (isElementDisplayed(continuePrimaryButton)) {
                        scrollToElement(continuePrimaryButton);
                        clickElement(continuePrimaryButton);
                    } else {
                        WebElement cont = driver.findElement(By.cssSelector("a.btn.btn-primary"));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cont);
                    }
                } catch (Exception e) {
                    System.out.println("⚠ Could not click Continue on Logout: " + e.getMessage());
                }
            } else {
                System.out.println("⚠ Logout page not confirmed, clearing session");
            }

            try { driver.manage().deleteAllCookies(); } catch (Exception ignored) {}
            ensureAtHome();

        } catch (Exception e) {
            System.out.println("⚠ Logout error: " + e.getMessage());
            try {
                driver.manage().deleteAllCookies();
                ensureAtHome();
            } catch (Exception ignored) {}
        }
    }

    // ============================================================
    // Orchestration
    // ============================================================
    public boolean registerUser(String firstName, String lastName, String email, String password) {
        boolean registrationSuccessful = false;
        try {
            System.out.println("   → Filling registration form...");
            fillRegistrationForm(firstName, lastName, email, password);

            System.out.println("   → Accepting privacy policy...");
            acceptPrivacyPolicy();

            System.out.println("   → Submitting form...");
            clickContinue();

            String outcome = waitForRegistrationOutcome(8000);

            if ("success".equals(outcome)) {
                System.out.println("✓ Registration completed successfully");
                registrationSuccessful = true;
                clickContinueAfterSuccess();
            } else if ("error".equals(outcome)) {
                System.out.println("⚠ Registration failed - Error: " + getAnyErrorMessage());
                registrationSuccessful = false;
            } else {
                // Ambiguous — decide conservatively
                boolean stillOnRegister = isStillOnRegisterForm();
                boolean anyError        = isAnyErrorDisplayed();

                if (anyError) {
                    System.out.println("⚠ Ambiguous, but error text is present: " + getAnyErrorMessage());
                    registrationSuccessful = false;
                } else if (stillOnRegister) {
                    System.out.println("⚠ Ambiguous and still on register page — treating as FAILURE (defensive)");
                    System.out.println("   URL: " + getCurrentUrl());
                    registrationSuccessful = false;
                } else {
                    System.out.println("⚠ Ambiguous but URL left register page — assuming success");
                    registrationSuccessful = true;
                    clickContinueAfterSuccess();
                }
            }
        } catch (Exception e) {
            System.out.println("✗ Exception during registration: " + e.getMessage());
            e.printStackTrace();
            registrationSuccessful = false;
        } finally {
            try {
                // Only log out when a real session exists (Logout link visible).
                // The "My Account" aside link is ALWAYS visible, so it must not be used.
                if (isElementDisplayed(logoutLink)) {
                    logoutAfterRegistration();
                } else {
                    System.out.println("   → No active session to close");
                }
            } catch (Exception ex) {
                System.out.println("⚠ Error closing session: " + ex.getMessage());
                try { driver.manage().deleteAllCookies(); } catch (Exception ignored) {}
            }
        }
        return registrationSuccessful;
    }

    /**
     * Poll for a clear success or failure signal.
     * Failure is detected from BOTH field-level (#error-*) and alert-level errors.
     */
    private String waitForRegistrationOutcome(int timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                if (isOnSuccessPage()) return "success";
                if (isElementDisplayed(continuePrimaryButton)) return "success";
                if (isElementDisplayed(continueButtonAfterSuccess)) return "success";
            } catch (Exception ignored) {}

            try {
                if (isAnyErrorDisplayed()) return "error";
            } catch (Exception ignored) {}

            waitHelper.customWait(200);
        }
        return "unknown";
    }

    private boolean waitForLogoutPage(int timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                if (isElementDisplayed(accountLogoutHeader)
                        || getCurrentUrl().contains("route=account/logout")) {
                    return true;
                }
            } catch (Exception ignored) {}
            waitHelper.customWait(200);
        }
        return false;
    }

    private void ensureAtHome() {
        if (!getCurrentUrl().contains("opencart-4.1.0.4/upload/")) {
            navigateTo(BASE_URL);
            waitHelper.customWait(300);
        }
    }
}