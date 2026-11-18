package pages;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import utils.WaitHelper;

public class BasePage {
    protected WebDriver driver;
    protected WaitHelper waitHelper;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.waitHelper = new WaitHelper(driver);
        PageFactory.initElements(driver, this);
    }

    /**
     * Navigates to a specific URL
     */
    public void navigateTo(String url) {
        driver.get(url);
        waitHelper.waitForPageLoad();
    }

    /**
     * Clicks an element after waiting for it to be clickable
     */
    protected void clickElement(WebElement element) {
        waitHelper.waitForElementToBeClickable(element);
        element.click();
    }

    /**
     * Writes text into an element after waiting for it to be visible
     */
    protected void writeText(WebElement element, String text) {
        waitHelper.waitForElementToBeVisible(element);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Gets the text of an element after waiting for it to be visible
     */
    protected String getText(WebElement element) {
        waitHelper.waitForElementToBeVisible(element);
        return element.getText();
    }

    /**
     * Checks if an element is visible
     */
    protected boolean isElementDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Scrolls to an element using JavaScript
     */
    protected void scrollToElement(WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView(true);", element);
        waitHelper.customWait(500);
    }

    /**
     * Gets the title of the current page
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Gets the current URL
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}