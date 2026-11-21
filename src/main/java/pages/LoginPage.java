package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenCart Account Login page.
 */
public class LoginPage extends BasePage {

    private static final String BASE_URL      = "http://localhost/opencart-4.1.0.4/upload/";
    private static final String LOGIN_URL     = BASE_URL + "index.php?route=account/login";
    private static final String REGISTER_URL  = BASE_URL + "index.php?route=account/register";
    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(10);

    private static final By LOGOUT_LINK   = By.linkText("Logout");
    private static final By SIDEBAR_LINKS = By.cssSelector("#column-right a.list-group-item");
    private static final By ERROR_ALERT   = By.cssSelector("#alert .alert-danger");

    @FindBy(id = "input-email")
    private WebElement emailInput;

    @FindBy(id = "input-password")
    private WebElement passwordInput;

    @FindBy(css = "#form-login button[type='submit']")
    private WebElement loginButton;

    @FindBy(css = "#content a.btn-primary[href*='account/register']")
    private WebElement newCustomerContinueButton;

    @FindBy(xpath = "//nav[@id='top']//span[@role='button' and contains(normalize-space(.), 'My Account')]")
    private WebElement myAccountDropdownToggle;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------
    public void navigateToLoginPage() {
        navigateTo(LOGIN_URL);
    }

    public boolean isOnLoginPage() {
        return driver.getCurrentUrl().contains("account/login");
    }

    public boolean isOnAccountPage() {
        return driver.getCurrentUrl().contains("account/account");
    }

    /**
     * TC_095 – Navigate to Register page, then click the 'login page' inline
     * link inside the intro paragraph, and wait for the Login URL.
     */
    public void clickLoginPageLinkOnRegisterPage() {
        navigateTo(REGISTER_URL);
        try {
            waitHelper.waitForElementToBeVisible(
                    driver.findElement(By.name("firstname")));
        } catch (Exception ignored) {}

        WebElement link = driver.findElement(
                By.xpath("//div[@id='content']//p[contains(.,'already have an account')]//a"));
        clickElement(link);

        try {
            new WebDriverWait(driver, DEFAULT_WAIT)
                    .until(d -> d.getCurrentUrl().contains("account/login"));
        } catch (TimeoutException ignored) {}
    }

    // ------------------------------------------------------------------
    // Form
    // ------------------------------------------------------------------
    public void enterEmail(String email) {
        if (email != null && !email.isEmpty()) writeText(emailInput, email);
    }

    public void enterPassword(String password) {
        if (password != null && !password.isEmpty()) writeText(passwordInput, password);
    }

    public void clearLoginForm() {
        try { emailInput.clear(); }   catch (Exception ignored) {}
        try { passwordInput.clear(); } catch (Exception ignored) {}
    }

    public boolean isLoginFormVisible() {
        try {
            return emailInput.isDisplayed()
                    && passwordInput.isDisplayed()
                    && loginButton.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getPasswordFieldType() {
        try { return passwordInput.getAttribute("type"); }
        catch (Exception e) { return ""; }
    }

    /** Bypass HTML5 validation so invalid input reaches the server. */
    public void disableHtml5Validation() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var f=document.getElementById('form-login');" +
                            "if(f)f.setAttribute('novalidate','novalidate');"
            );
        } catch (Exception ignored) {}
    }

    public void clickLogin() {
        clickElement(loginButton);
        waitForLoginOutcome();
    }

    private void waitForLoginOutcome() {
        try {
            new WebDriverWait(driver, DEFAULT_WAIT).until(d -> {
                if (!d.getCurrentUrl().contains("account/login")) return true;
                return !d.findElements(ERROR_ALERT).isEmpty();
            });
        } catch (TimeoutException ignored) {}
    }

    public boolean login(String email, String password) {
        navigateToLoginPage();
        if (!isOnLoginPage()) { logout(); navigateToLoginPage(); }
        disableHtml5Validation();
        enterEmail(email);
        enterPassword(password);
        clickLogin();
        return isLoginSuccessful();
    }

    public boolean loginWithoutCredentials() {
        navigateToLoginPage();
        if (!isOnLoginPage()) { logout(); navigateToLoginPage(); }
        clearLoginForm();
        disableHtml5Validation();
        clickLogin();
        return isLoginSuccessful();
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------
    public boolean isLoginSuccessful() {
        try {
            if (isOnLoginPage())           return false;
            if (isErrorMessageDisplayed()) return false;
            return isUserLoggedIn() && isOnAccountPage();
        } catch (Exception e) { return false; }
    }

    public boolean isUserLoggedIn() {
        try { return !driver.findElements(LOGOUT_LINK).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public boolean isErrorMessageDisplayed() {
        try { return !driver.findElements(ERROR_ALERT).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public String getErrorMessage() {
        try {
            List<WebElement> errs = driver.findElements(ERROR_ALERT);
            if (errs.isEmpty()) return "";
            return errs.get(0).getText().trim();
        } catch (Exception e) { return ""; }
    }

    // ------------------------------------------------------------------
    // Logout
    // ------------------------------------------------------------------
    public void openMyAccountDropdown() {
        clickElement(myAccountDropdownToggle);
    }

    public void clickLogoutFromDropdown() {
        openMyAccountDropdown();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(LOGOUT_LINK));
        } catch (TimeoutException ignored) {}

        clickElement(driver.findElement(LOGOUT_LINK));

        try {
            new WebDriverWait(driver, DEFAULT_WAIT).until(d ->
                    d.getCurrentUrl().contains("account/logout")
                            || d.findElements(LOGOUT_LINK).isEmpty());
        } catch (TimeoutException ignored) {}
    }

    public void logout() {
        try {
            if (!isUserLoggedIn()) return;
            try { clickLogoutFromDropdown(); }
            catch (Exception e) { navigateTo(BASE_URL + "index.php?route=account/logout"); }
            try { driver.manage().deleteAllCookies(); } catch (Exception ignored) {}
            navigateTo(BASE_URL);
        } catch (Exception e) {
            try { driver.manage().deleteAllCookies(); } catch (Exception ignored) {}
        }
    }

    // ------------------------------------------------------------------
    // New customer / forgotten password
    // ------------------------------------------------------------------
    public void clickNewCustomerContinue() {
        clickElement(newCustomerContinueButton);
        try {
            new WebDriverWait(driver, DEFAULT_WAIT)
                    .until(d -> d.getCurrentUrl().contains("account/register"));
        } catch (TimeoutException ignored) {}
    }

    // ------------------------------------------------------------------
    // Sidebar
    // ------------------------------------------------------------------
    public List<WebElement> getSidebarLinks() {
        return driver.findElements(SIDEBAR_LINKS);
    }

    public void clickSidebarLink(String linkText) {
        By locator = By.xpath(
                "//aside[@id='column-right']//a[normalize-space()='" + linkText + "']");
        clickElement(driver.findElement(locator));
        try {
            new WebDriverWait(driver, DEFAULT_WAIT)
                    .until(d -> d.getCurrentUrl().contains("account/"));
        } catch (TimeoutException ignored) {}
    }
}