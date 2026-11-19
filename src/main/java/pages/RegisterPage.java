package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

public class RegisterPage extends BasePage {

    private static final String BASE_URL       = "http://localhost/opencart-4.1.0.4/upload/";
    private static final String REGISTER_URL   = BASE_URL + "index.php?route=account/register";
    private static final String NEWSLETTER_URL = BASE_URL + "index.php?route=account/newsletter";

    @FindBy(name = "firstname")        private WebElement firstNameInput;
    @FindBy(name = "lastname")         private WebElement lastNameInput;
    @FindBy(name = "email")            private WebElement emailInput;
    @FindBy(name = "password")         private WebElement passwordInput;
    @FindBy(name = "agree")            private WebElement privacyPolicyCheckbox;
    @FindBy(id   = "input-newsletter") private WebElement newsletterCheckbox;

    @FindBy(xpath = "//button[@type='submit' and normalize-space(.)='Continue']")
    private WebElement continueButton;

    @FindBy(xpath = "//a[contains(normalize-space(text()), 'Continue')]")
    private WebElement continueButtonAfterSuccess;

    @FindBy(css = "a.btn.btn-primary") private WebElement continuePrimaryButton;

    @FindBy(xpath = "//div[contains(@class, 'alert-success')]")                private WebElement successMessage;
    @FindBy(xpath = "//h1[contains(., 'Your Account Has Been Created')]")      private WebElement accountCreatedHeader;
    @FindBy(xpath = "//div[@id='content']//p[contains(., 'successfully')]")    private WebElement accountCreatedText;
    @FindBy(xpath = "//h1[contains(., 'Account Logout')]")                     private WebElement accountLogoutHeader;

    @FindBy(linkText = "My Account") private WebElement myAccountLink;
    @FindBy(linkText = "Logout")     private WebElement logoutLink;

    /**
     * Captures the last error/warning text observed right after a submit.
     * Needed because OpenCart's #alert .alert-danger auto-dismisses after ~4 s;
     * reading it again later (via getAnyErrorMessage) returns empty.
     */
    private String lastRegistrationError = "";

    public RegisterPage(WebDriver driver) { super(driver); }

    public String getLastRegistrationError() { return lastRegistrationError; }
    public void resetLastRegistrationError() { lastRegistrationError = ""; }

    // ==================================================================
    // Navigation
    // ==================================================================
    public void navigateToRegisterPage() {
        navigateTo(REGISTER_URL);
        waitHelper.customWait(300);
        try {
            waitHelper.waitForElementToBeVisible(firstNameInput);
            System.out.println("✓ Registration form loaded");
        } catch (Exception e) {
            if (isElementDisplayed(logoutLink)) {
                logoutAfterRegistration();
                navigateTo(REGISTER_URL);
                waitHelper.waitForElementToBeVisible(firstNameInput);
            } else throw e;
        }
        waitHelper.customWait(500);
    }

    public void navigateToNewsletterPage() {
        navigateTo(NEWSLETTER_URL);
        waitHelper.customWait(400);
    }

    // ==================================================================
    // Form
    // ==================================================================
    public void fillRegistrationForm(String fn, String ln, String em, String pw) {
        writeTextIfNotEmpty(firstNameInput, fn);
        writeTextIfNotEmpty(lastNameInput,  ln);
        writeTextIfNotEmpty(emailInput,     em);
        writeTextIfNotEmpty(passwordInput,  pw);
    }

    private void writeTextIfNotEmpty(WebElement el, String value) {
        if (value != null && !value.isEmpty()) writeText(el, value);
    }

    public void disableHtml5Validation() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var f=document.getElementById('form-register');if(f)f.setAttribute('novalidate','novalidate');");
        } catch (Exception ignored) {}
    }

    public void acceptPrivacyPolicy() {
        try {
            scrollToElement(privacyPolicyCheckbox);
            if (!privacyPolicyCheckbox.isSelected()) clickElement(privacyPolicyCheckbox);
            if (!privacyPolicyCheckbox.isSelected()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", privacyPolicyCheckbox);
            }
            System.out.println(privacyPolicyCheckbox.isSelected()
                    ? "✓ Privacy policy accepted" : "⚠ Privacy policy NOT accepted");
        } catch (Exception e) {
            System.out.println("⚠ Error accepting privacy policy: " + e.getMessage());
        }
    }

    public void setNewsletterSubscription(boolean subscribe) {
        try {
            scrollToElement(newsletterCheckbox);
            if (newsletterCheckbox.isSelected() != subscribe) clickElement(newsletterCheckbox);
            if (newsletterCheckbox.isSelected() != subscribe) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", newsletterCheckbox);
            }
            System.out.println("✓ Newsletter set to " + (subscribe ? "ON" : "OFF"));
        } catch (Exception e) {
            System.out.println("⚠ Could not set newsletter: " + e.getMessage());
        }
    }

    public boolean isNewsletterSubscriptionOn() {
        try { return newsletterCheckbox.isSelected(); }
        catch (Exception e) { return false; }
    }

    // ==================================================================
    // Continue – captures alert text before it fades
    // ==================================================================
    public void clickContinue() {
        lastRegistrationError = "";
        try { scrollToElement(continueButton); } catch (Exception ignored) {}

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                waitHelper.waitForElementToBeClickable(continueButton);
                clickElement(continueButton);

                // Poll aggressively (100 ms × 25 = 2.5 s) — bootstrap alert fades in ~3-4 s
                for (int i = 0; i < 25; i++) {
                    waitHelper.customWait(100);

                    String err = getAnyErrorMessage();
                    if (!err.isEmpty()) {
                        lastRegistrationError = err;
                        System.out.println("   [captured error] " + err);
                        return;
                    }
                    if (isOnSuccessPage() || isElementDisplayed(continueButtonAfterSuccess)) {
                        System.out.println("   [captured success]");
                        return;
                    }
                }
            } catch (Exception e) {
                try { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", continueButton); }
                catch (Exception ignored) {}
            }
            waitHelper.customWait(300);
        }
    }

    // ==================================================================
    // Error inspection
    // ==================================================================
    public String getFieldErrorMessage(String fieldName) {
        try {
            WebElement err = driver.findElement(By.id("error-" + fieldName));
            if (err.isDisplayed()) {
                String t = err.getText();
                return t != null ? t.trim() : "";
            }
        } catch (Exception ignored) {}
        return "";
    }

    public String getAnyErrorMessage() {
        StringBuilder sb = new StringBuilder();
        for (String f : new String[]{"firstname", "lastname", "email", "password"}) {
            String err = getFieldErrorMessage(f);
            if (!err.isEmpty()) { if (sb.length() > 0) sb.append(" | "); sb.append(err); }
        }
        try {
            List<WebElement> alerts = driver.findElements(
                    By.cssSelector("#alert .alert-danger, #alert .alert-warning"));
            for (WebElement a : alerts) {
                if (a.isDisplayed()) {
                    String t = a.getText();
                    if (t != null && !t.trim().isEmpty()) {
                        if (sb.length() > 0) sb.append(" | ");
                        sb.append(t.trim());
                    }
                }
            }
        } catch (Exception ignored) {}
        return sb.toString().trim();
    }

    public boolean isAnyErrorDisplayed() { return !getAnyErrorMessage().isEmpty(); }

    public boolean isWarningDisplayed() {
        try {
            List<WebElement> ws = driver.findElements(
                    By.cssSelector("#alert .alert-warning, #alert .alert-danger"));
            for (WebElement w : ws) if (w.isDisplayed()) return true;
        } catch (Exception ignored) {}
        return false;
    }

    public boolean isErrorMessageDisplayed() { return isAnyErrorDisplayed(); }
    public String  getErrorMessage()         { return getAnyErrorMessage(); }

    // ==================================================================
    // Success / page state
    // ==================================================================
    public boolean isSuccessMessageDisplayed() {
        try {
            waitHelper.customWait(800);
            return isElementDisplayed(successMessage) || isOnSuccessPage();
        } catch (Exception e) { return false; }
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

    public boolean isStillOnRegisterForm() {
        try { return getCurrentUrl().contains("route=account/register") && firstNameInput.isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public boolean isFormFieldsVisible() {
        try {
            return firstNameInput.isDisplayed() && lastNameInput.isDisplayed()
                    && emailInput.isDisplayed() && passwordInput.isDisplayed()
                    && continueButton.isDisplayed();
        } catch (Exception e) { return false; }
    }

    // ==================================================================
    // Newsletter subscription status (on /account/newsletter)
    // ==================================================================
    public boolean isNewsletterSubscribed() {
        try {
            WebElement cb = driver.findElement(By.id("input-newsletter"));
            return cb.isSelected();
        } catch (Exception e) {
            System.out.println("⚠ Could not read newsletter status: " + e.getMessage());
            return false;
        }
    }

    // ==================================================================
    // Orchestration
    // ==================================================================
    public boolean registerUser(String fn, String ln, String em, String pw) {
        return registerUserWithOptions(fn, ln, em, pw, false, true, false);
    }

    public boolean registerUserWithOptions(String firstName, String lastName,
                                           String email, String password,
                                           boolean subscribeNewsletter,
                                           boolean acceptPrivacy,
                                           boolean stayLoggedIn) {
        lastRegistrationError = "";
        boolean successful = false;
        try {
            System.out.println("   → Filling registration form...");
            disableHtml5Validation();
            fillRegistrationForm(firstName, lastName, email, password);

            if (subscribeNewsletter) {
                System.out.println("   → Enabling newsletter...");
                setNewsletterSubscription(true);
            }
            if (acceptPrivacy) {
                System.out.println("   → Accepting privacy policy...");
                acceptPrivacyPolicy();
            }

            System.out.println("   → Submitting form...");
            clickContinue();

            String outcome = waitForRegistrationOutcome(8000);
            if ("success".equals(outcome)) {
                System.out.println("✓ Registration successful");
                successful = true;
                clickContinueAfterSuccess();
            } else if ("error".equals(outcome)) {
                System.out.println("⚠ Registration failed: " + lastRegistrationError);
            } else {
                if (!lastRegistrationError.isEmpty())             successful = false;
                else if (isAnyErrorDisplayed())                    successful = false;
                else if (isStillOnRegisterForm())                  successful = false;
                else { successful = true; clickContinueAfterSuccess(); }
            }
        } catch (Exception e) {
            System.out.println("✗ Exception during registration: " + e.getMessage());
            successful = false;
        } finally {
            if (!stayLoggedIn) {
                try { if (isElementDisplayed(logoutLink)) logoutAfterRegistration(); }
                catch (Exception ex) { try { driver.manage().deleteAllCookies(); } catch (Exception ignored) {} }
            }
        }
        return successful;
    }

    private String waitForRegistrationOutcome(int timeoutMs) {
        if (!lastRegistrationError.isEmpty()) return "error";

        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                if (isOnSuccessPage() || isElementDisplayed(continuePrimaryButton)
                        || isElementDisplayed(continueButtonAfterSuccess)) return "success";
            } catch (Exception ignored) {}

            try {
                String err = getAnyErrorMessage();
                if (!err.isEmpty()) {
                    lastRegistrationError = err;
                    return "error";
                }
            } catch (Exception ignored) {}
            waitHelper.customWait(200);
        }
        return "unknown";
    }

    // ==================================================================
    // Post-success / logout
    // ==================================================================
    public void clickContinueAfterSuccess() {
        try {
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
                        WebElement c = driver.findElement(By.xpath("//a[normalize-space(text())='Continue']"));
                        scrollToElement(c); c.click();
                    } catch (Exception e2) {
                        try {
                            WebElement c = (continuePrimaryButton != null) ? continuePrimaryButton
                                    : driver.findElement(By.cssSelector("a.btn.btn-primary"));
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", c);
                        } catch (Exception ignored) {}
                    }
                }
                boolean leftSuccess = false;
                for (int w = 0; w < 10; w++) {
                    waitHelper.customWait(200);
                    if (!getCurrentUrl().contains("route=account/success")) { leftSuccess = true; break; }
                }
                if (leftSuccess) { System.out.println("✓ Left success page"); return; }
            }
            navigateTo(BASE_URL);
        } catch (Exception e) {
            System.out.println("⚠ Error leaving success page: " + e.getMessage());
        }
    }

    public void logoutAfterRegistration() {
        try {
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
                System.out.println("⚠ Direct logout failed: " + e.getMessage());
            }
            boolean onLogout = waitForLogoutPage(5000);
            if (onLogout) {
                try {
                    if (isElementDisplayed(continuePrimaryButton)) {
                        scrollToElement(continuePrimaryButton);
                        clickElement(continuePrimaryButton);
                    } else {
                        WebElement c = driver.findElement(By.cssSelector("a.btn.btn-primary"));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", c);
                    }
                } catch (Exception ignored) {}
            }
            try { driver.manage().deleteAllCookies(); } catch (Exception ignored) {}
            ensureAtHome();
        } catch (Exception e) {
            try { driver.manage().deleteAllCookies(); ensureAtHome(); } catch (Exception ignored) {}
        }
    }

    private boolean waitForLogoutPage(int timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                if (isElementDisplayed(accountLogoutHeader)
                        || getCurrentUrl().contains("route=account/logout")) return true;
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