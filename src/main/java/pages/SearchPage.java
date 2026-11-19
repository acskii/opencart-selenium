package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenCart Product Search page.
 *
 * Notes on the underlying page (see provided HTML):
 *  - The "Search" button is <button type="button" id="button-search"> (JS-driven).
 *  - The category dropdown is disabled until a specific category is chosen for the
 *    sub-category checkbox; selecting a category enables it.
 *  - Subcategory options are indented with &nbsp; entities in their innerHTML.
 *  - Empty results show: "There is no product that matches the search criteria."
 *  - H1 and <title> both render as "Search - <keyword>".
 */
public class SearchPage extends BasePage {

    private static final String BASE_URL    = "http://localhost/opencart-4.1.0.4/upload/";
    private static final String SEARCH_URL  = BASE_URL + "index.php?route=product/search";
    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(10);

    // Standard OpenCart 4 product listing container
    private static final By PRODUCT_THUMBS = By.cssSelector("#product-search .product-thumb");
    private static final By PRODUCT_LINKS  = By.cssSelector("#product-search a[href*='product_id=']");
    private static final By EMPTY_MESSAGE  = By.xpath("//div[@id='content']//p[contains(.,'no product that matches')]");

    @FindBy(id = "input-search")       private WebElement searchInput;
    @FindBy(id = "input-description")  private WebElement descriptionCheckbox;
    @FindBy(id = "input-category")     private WebElement categorySelect;
    @FindBy(id = "input-sub-category") private WebElement subCategoryCheckbox;
    @FindBy(id = "button-search")      private WebElement searchButton;

    @FindBy(xpath = "//div[@id='content']//h1[contains(.,'Search')]")             private WebElement h1Header;
    @FindBy(xpath = "//div[@id='content']//h2[contains(.,'Products meeting')]")   private WebElement resultsHeading;

    // Header (global) search form
    private static final By HEADER_SEARCH_INPUT  = By.xpath("//header//form//input[@name='search']");
    private static final By HEADER_SEARCH_BUTTON = By.xpath("//header//form//button[@type='submit']");

    public SearchPage(WebDriver driver) { super(driver); }

    // ==================================================================
    // Navigation
    // ==================================================================
    public void navigateToSearchPage() {
        navigateTo(SEARCH_URL);
        waitHelper.customWait(300);
        try { waitHelper.waitForElementToBeVisible(searchInput); }
        catch (Exception e) { System.out.println("⚠ Search form not visible after navigation"); }
    }

    public void navigateToHome() {
        navigateTo(BASE_URL);
        waitHelper.customWait(300);
    }

    // ==================================================================
    // Form interaction
    // ==================================================================
    public void enterKeyword(String keyword) {
        try {
            searchInput.clear();
            if (keyword != null && !keyword.isEmpty()) searchInput.sendKeys(keyword);
        } catch (Exception e) {
            System.out.println("⚠ Could not enter keyword: " + e.getMessage());
        }
    }

    public void setSearchInDescription(boolean enable) {
        try {
            if (descriptionCheckbox.isSelected() != enable) clickElement(descriptionCheckbox);
        } catch (Exception e) {
            System.out.println("⚠ Could not toggle description: " + e.getMessage());
        }
    }

    public void selectCategory(String categoryName) {
        try {
            new Select(categorySelect).selectByVisibleText(categoryName);
            // Fire change so the subcategory toggle updates
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                    categorySelect);
            waitHelper.customWait(200);
        } catch (Exception e) {
            System.out.println("⚠ Could not select category '" + categoryName + "': " + e.getMessage());
        }
    }

    public void selectCategoryById(String id) {
        try {
            new Select(categorySelect).selectByValue(id);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                    categorySelect);
            waitHelper.customWait(200);
        } catch (Exception e) {
            System.out.println("⚠ Could not select category id " + id + ": " + e.getMessage());
        }
    }

    public void setSearchInSubcategories(boolean enable) {
        try {
            if (!subCategoryCheckbox.isEnabled() && enable) {
                System.out.println("⚠ Cannot enable subcategory checkbox (currently disabled)");
                return;
            }
            if (subCategoryCheckbox.isSelected() != enable) clickElement(subCategoryCheckbox);
        } catch (Exception e) {
            System.out.println("⚠ Could not toggle subcategory: " + e.getMessage());
        }
    }

    public void clickSearch() {
        String beforeUrl = driver.getCurrentUrl();
        String beforeH1  = getH1Text();

        clickElement(searchButton);

        try {
            new WebDriverWait(driver, DEFAULT_WAIT).until(d -> {
                String url = d.getCurrentUrl();
                String h1  = "";
                try { h1 = h1Header.getText().trim(); } catch (Exception ignored) {}
                return !url.equals(beforeUrl) || !h1.equals(beforeH1);
            });
        } catch (Exception ignored) {}
        waitHelper.customWait(400);
    }

    /** Press Enter on the search field — same as clicking Search. */
    public void submitWithEnterKey() {
        String beforeUrl = driver.getCurrentUrl();
        searchInput.sendKeys(org.openqa.selenium.Keys.ENTER);
        try {
            new WebDriverWait(driver, DEFAULT_WAIT)
                    .until(ExpectedConditions.not(ExpectedConditions.urlToBe(beforeUrl)));
        } catch (Exception ignored) {}
        waitHelper.customWait(400);
    }

    /** Search via the header (global) form, then wait for the results page. */
    public void searchFromHeader(String keyword) {
        navigateToHome();
        WebElement headerInput = driver.findElement(HEADER_SEARCH_INPUT);
        headerInput.clear();
        headerInput.sendKeys(keyword);
        headerInput.sendKeys(org.openqa.selenium.Keys.ENTER);
        try {
            new WebDriverWait(driver, DEFAULT_WAIT)
                    .until(ExpectedConditions.urlContains("route=product/search"));
        } catch (Exception ignored) {}
        waitHelper.customWait(500);
    }

    // ==================================================================
    // Composite
    // ==================================================================
    public void performSearch(String keyword) {
        navigateToSearchPage();
        enterKeyword(keyword);
        clickSearch();
    }

    public void performAdvancedSearch(String keyword,
                                      String categoryName,
                                      boolean searchDescription,
                                      Boolean searchSubcategories) {
        navigateToSearchPage();
        enterKeyword(keyword);
        if (categoryName != null && !categoryName.isEmpty()) {
            selectCategory(categoryName);
            if (searchSubcategories != null) setSearchInSubcategories(searchSubcategories);
        }
        if (searchDescription) setSearchInDescription(true);
        clickSearch();
    }

    // ==================================================================
    // Querying
    // ==================================================================
    public String getSearchInputValue() {
        try { return searchInput.getAttribute("value"); }
        catch (Exception e) { return ""; }
    }

    public int getResultCount() {
        try { return driver.findElements(PRODUCT_THUMBS).size(); }
        catch (Exception e) { return 0; }
    }

    public List<String> getResultProductNames() {
        List<String> names = new ArrayList<>();
        try {
            for (WebElement item : driver.findElements(PRODUCT_THUMBS)) {
                try {
                    WebElement link = item.findElement(By.cssSelector("h4 a, .description h4 a, a"));
                    String text = link.getText().trim();
                    if (!text.isEmpty()) names.add(text);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return names;
    }

    public List<WebElement> getProductLinks() {
        return driver.findElements(PRODUCT_LINKS);
    }

    public boolean isEmptyStateDisplayed() {
        try {
            List<WebElement> msgs = driver.findElements(EMPTY_MESSAGE);
            boolean hasMsg = !msgs.isEmpty() && msgs.get(0).isDisplayed();
            boolean noProducts = driver.findElements(PRODUCT_THUMBS).isEmpty();
            return hasMsg && noProducts;
        } catch (Exception e) { return false; }
    }

    public String getEmptyMessageText() {
        try {
            List<WebElement> msgs = driver.findElements(EMPTY_MESSAGE);
            return msgs.isEmpty() ? "" : msgs.get(0).getText().trim();
        } catch (Exception e) { return ""; }
    }

    public boolean isResultsHeadingDisplayed() {
        try { return resultsHeading.isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public String getH1Text() {
        try { return h1Header.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getPageTitle() { return driver.getTitle(); }

    public List<String> getBreadcrumbTexts() {
        List<String> texts = new ArrayList<>();
        try {
            for (WebElement bc : driver.findElements(By.cssSelector("ul.breadcrumb li.breadcrumb-item"))) {
                String t = bc.getText().trim();
                if (!t.isEmpty()) texts.add(t);
            }
        } catch (Exception ignored) {}
        return texts;
    }

    public boolean isSubcategoryCheckboxEnabled() {
        try { return subCategoryCheckbox.isEnabled(); }
        catch (Exception e) { return false; }
    }

    public String getSelectedCategoryName() {
        try { return new Select(categorySelect).getFirstSelectedOption().getText().trim(); }
        catch (Exception e) { return ""; }
    }

    /** Returns the raw innerHTML of every <option> in the category dropdown. */
    public List<String> getCategoryOptionsRawHtml() {
        List<String> raws = new ArrayList<>();
        try {
            for (WebElement opt : new Select(categorySelect).getOptions()) {
                raws.add(opt.getAttribute("innerHTML"));
            }
        } catch (Exception ignored) {}
        return raws;
    }

    /** Decoded value of the &search= query parameter, "" if absent or empty. */
    public String getSearchParamFromUrl() {
        try {
            String url = driver.getCurrentUrl();
            int idx = url.indexOf("&search=");
            if (idx < 0) idx = url.indexOf("?search=");
            if (idx < 0) return "";
            int start = url.indexOf('=', idx) + 1;
            int end = url.indexOf('&', start);
            String raw = (end < 0) ? url.substring(start) : url.substring(start, end);
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }
}