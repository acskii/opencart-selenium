import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.SearchPage;

import java.util.List;

/**
 * Search Test — covers TC_073 to TC_085 plus additional edge-case tests
 * TC_141 to TC_147.
 *
 * AUTOMATION NOTES
 *  - TC_077 / TC_078 / TC_079 / TC_082 / TC_083 depend on catalog data:
 *      * 'revolutionary' must exist ONLY in a product description (iPhone
 *        in stock OpenCart demo data).
 *      * 'Apple Cinema 30' must live under Components (directly or via the
 *        Monitors child category).
 *    If a test fails, it documents the actual catalog state at test time.
 *  - TC_080 checks DOM indentation via &nbsp; in the option innerHTML, not
 *    visual rendering. Visual review of the dropdown is still recommended.
 *  - TC_075 verifies the XSS payload did not execute (no JS alert) and the
 *    SQL payload did not leak SQLSTATE / Fatal error in the source.
 */
public class SearchTest extends BaseTest {

    private SearchPage searchPage;

    @BeforeClass
    public void setupTest() {
        searchPage = new SearchPage(driver);
        logWriter.logSection("SEARCH TEST");
    }

    @BeforeMethod
    public void resetState() {
        // Every test navigates fresh; ensure we're at a known page
        try { searchPage.navigateToSearchPage(); } catch (Exception ignored) {}
    }

    // ==================================================================
    // TC_073 – Valid keyword returns products
    // ==================================================================
    @Test(priority = 1, description = "TC_073 – Search with valid keyword 'Mac' returns products")
    public void testValidKeywordSearch() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_073: Valid keyword search");
        logWriter.logMessage("Keyword: 'Mac'");
        logWriter.logMessage("========================================");

        searchPage.performSearch("Mac");
        String url = driver.getCurrentUrl();
        String h1  = searchPage.getH1Text();
        int    cnt = searchPage.getResultCount();

        logWriter.logMessage("   URL:   " + url);
        logWriter.logMessage("   H1:    '" + h1 + "'");
        logWriter.logMessage("   Count: " + cnt);

        Assert.assertEquals(searchPage.getSearchParamFromUrl(), "Mac", "URL must carry search=Mac, was: " + url);
        Assert.assertTrue(h1.startsWith("Search - "),
                "H1 must start with 'Search - ', was: " + h1);
        Assert.assertTrue(h1.contains("Mac"), "H1 must contain keyword, was: " + h1);
        Assert.assertTrue(cnt > 0,
                "At least one product must match 'Mac' — catalog may need review");
        Assert.assertFalse(searchPage.isEmptyStateDisplayed(),
                "Empty-state message must NOT appear when results exist");

        logWriter.logMessage("✓ Returned " + cnt + " product(s) for 'Mac'");
        logWriter.logRegistration("Mac", true, "✓ Valid keyword search returned " + cnt + " products");
    }

    // ==================================================================
    // TC_074 – Empty / whitespace keyword
    // ==================================================================
    @Test(priority = 2, description = "TC_074 – Empty and whitespace-only keyword show empty state")
    public void testEmptyAndWhitespaceKeyword() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_074: Empty / whitespace keyword");
        logWriter.logMessage("========================================");

        // Case A: truly empty
        logWriter.logMessage("Case A: empty keyword");
        searchPage.performSearch("");
        Assert.assertTrue(searchPage.isEmptyStateDisplayed(),
                "Empty keyword must show empty state");
        Assert.assertTrue(searchPage.getEmptyMessageText()
                        .contains("no product that matches"),
                "Empty message text must match expected wording");
        logWriter.logMessage("   ✓ Empty case: empty state shown");

        // Case B: three spaces
        logWriter.logMessage("Case B: three spaces");
        searchPage.performSearch("   ");
        Assert.assertTrue(searchPage.isEmptyStateDisplayed(),
                "Whitespace-only keyword must be trimmed → empty state");
        Assert.assertTrue(searchPage.getEmptyMessageText()
                        .contains("no product that matches"),
                "Whitespace case must show same empty message");
        Assert.assertFalse(driver.getPageSource().contains("Fatal error"),
                "No PHP fatal error may appear on whitespace search");
        logWriter.logMessage("   ✓ Whitespace case: empty state shown, no error");

        logWriter.logRegistration("", true, "✓ Empty & whitespace searches handled gracefully");
    }

    // ==================================================================
    // TC_075 – XSS payload is safe
    // ==================================================================
    @Test(priority = 3, description = "TC_075 – XSS payload does not execute")
    public void testXssPayloadSafe() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_075 (XSS): Script injection is safe");
        logWriter.logMessage("========================================");

        String payload = "alert(\"XSS\")";
        searchPage.performSearch(payload);
        logWriter.logMessage("   Payload sent: " + payload);
        logWriter.logMessage("   Current URL:  " + driver.getCurrentUrl());

        // No JS alert must have been triggered
        try {
            driver.switchTo().alert();
            Assert.fail("XSS vulnerability: alert() was executed by the payload");
        } catch (NoAlertPresentException expected) {
            logWriter.logMessage("   ✓ No JS alert triggered");
        }

        String src = driver.getPageSource();
        Assert.assertFalse(src.contains("<script>alert"),
                "Payload must be HTML-encoded, not injected as raw <script>");
        Assert.assertFalse(src.contains("Fatal error"),
                "No PHP fatal error may leak");

        // H1 must be present and contain the payload as plain text
        String h1 = searchPage.getH1Text();
        logWriter.logMessage("   H1: '" + h1 + "'");
        Assert.assertTrue(h1.startsWith("Search - "),
                "Page must still render Search - ... header, was: " + h1);

        logWriter.logRegistration(payload, true,
                "✓ XSS payload safely encoded — no script execution");
    }

    // ==================================================================
    // TC_075 – SQL payload is safe
    // ==================================================================
    @Test(priority = 4, description = "TC_075 (SQLi) – SQL injection payload is safe")
    public void testSqlPayloadSafe() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_075 (SQLi): SQL injection is safe");
        logWriter.logMessage("========================================");

        String payload = "' OR '1'='1";
        searchPage.performSearch(payload);
        logWriter.logMessage("   Payload sent: " + payload);
        logWriter.logMessage("   Current URL:  " + driver.getCurrentUrl());

        String src = driver.getPageSource();
        Assert.assertFalse(src.contains("SQLSTATE"),
                "No SQL error must appear in page source");
        Assert.assertFalse(src.contains("Fatal error"),
                "No PHP fatal error must appear in page source");
        Assert.assertFalse(src.contains("Warning: mysqli"),
                "No mysqli warning must appear in page source");

        String h1 = searchPage.getH1Text();
        logWriter.logMessage("   H1: '" + h1 + "'");
        Assert.assertTrue(h1.startsWith("Search - "),
                "Page must still render Search - ... header");

        // The search page must still be usable
        searchPage.performSearch("Mac");
        Assert.assertTrue(searchPage.getResultCount() > 0,
                "App must remain functional after SQL payload attempt");

        logWriter.logRegistration(payload, true,
                "✓ SQL payload handled safely — no error leakage, app responsive");
    }

    // ==================================================================
    // TC_076 – Case insensitivity
    // ==================================================================
    @Test(priority = 5, description = "TC_076 – Case-insensitive keyword matching")
    public void testCaseInsensitiveSearch() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_076: Case insensitivity");
        logWriter.logMessage("========================================");

        searchPage.performSearch("apple");
        int lower = searchPage.getResultCount();
        List<String> lowerNames = searchPage.getResultProductNames();
        logWriter.logMessage("   'apple': " + lower + " result(s) " + lowerNames);

        searchPage.performSearch("APPLE");
        int upper = searchPage.getResultCount();
        List<String> upperNames = searchPage.getResultProductNames();
        logWriter.logMessage("   'APPLE': " + upper + " result(s) " + upperNames);

        searchPage.performSearch("ApPlE");
        int mixed = searchPage.getResultCount();
        List<String> mixedNames = searchPage.getResultProductNames();
        logWriter.logMessage("   'ApPlE': " + mixed + " result(s) " + mixedNames);

        Assert.assertEquals(upper, lower,
                "Uppercase must return same count as lowercase");
        Assert.assertEquals(mixed, lower,
                "Mixed case must return same count as lowercase");
        Assert.assertEquals(upperNames, lowerNames,
                "Result product names must be identical across casings");

        logWriter.logRegistration("apple/APPLE/ApPlE", true,
                "✓ All casings returned identical results (" + lower + " products)");
    }

    // ==================================================================
    // TC_077 – Description keyword excluded when checkbox unchecked
    // ==================================================================
    @Test(priority = 6, description = "TC_077 – Keyword in description only is excluded when unchecked")
    public void testDescriptionKeywordExcluded() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_077: Description keyword excluded");
        logWriter.logMessage("Keyword: 'revolutionary' | Checkbox: UNCHECKED");
        logWriter.logMessage("========================================");

        searchPage.navigateToSearchPage();
        searchPage.setSearchInDescription(false);
        searchPage.enterKeyword("revolutionary");
        searchPage.clickSearch();

        int cnt = searchPage.getResultCount();
        logWriter.logMessage("   Result count: " + cnt);

        Assert.assertTrue(searchPage.isEmptyStateDisplayed(),
                "Without description search, 'revolutionary' should return empty (it is "
                        + "only in the iPhone description). If this fails, verify the catalog: "
                        + "the keyword may exist in a product NAME.");
        logWriter.logRegistration("revolutionary (unchecked)", true,
                "✓ Keyword-in-description only was excluded (empty state shown)");
    }

    // ==================================================================
    // TC_078 – Description keyword included when checkbox checked
    // ==================================================================
    @Test(priority = 7, description = "TC_078 – Keyword in description is found when checked")
    public void testDescriptionKeywordIncluded() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_078: Description keyword included");
        logWriter.logMessage("Keyword: 'revolutionary' | Checkbox: CHECKED");
        logWriter.logMessage("========================================");

        searchPage.navigateToSearchPage();
        searchPage.setSearchInDescription(true);
        searchPage.enterKeyword("revolutionary");
        searchPage.clickSearch();

        int cnt = searchPage.getResultCount();
        List<String> names = searchPage.getResultProductNames();
        logWriter.logMessage("   Result count: " + cnt);
        logWriter.logMessage("   Products:     " + names);

        Assert.assertTrue(cnt > 0,
                "With description search enabled, 'revolutionary' must return at least "
                        + "one product (the iPhone in stock OpenCart demo). If this fails, verify "
                        + "the catalog has a product whose description contains the keyword.");

        logWriter.logRegistration("revolutionary (checked)", true,
                "✓ Description search returned " + cnt + " product(s): " + names);
    }

    // ==================================================================
    // TC_079 – Category filter
    // ==================================================================
    @Test(priority = 8, description = "TC_079 – Category filter narrows results")
    public void testCategoryFilter() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_079: Category filter");
        logWriter.logMessage("========================================");

        // Cinema + Components
        logWriter.logMessage("Step 1: Cinema + Desktops");
        searchPage.performAdvancedSearch("Cinema", "Desktops", false, null);
        int compCnt = searchPage.getResultCount();
        List<String> compNames = searchPage.getResultProductNames();
        logWriter.logMessage("   Desktops results: " + compCnt + " " + compNames);
        Assert.assertTrue(compCnt > 0,
                "Desktops category must return products matching 'Cinema' "
                        + "(e.g., 'Apple Cinema 30'). If 0, the catalog has the product only in "
                        + "a child category — see TC_082.");

        // Cinema + Phones & PDAs
        logWriter.logMessage("Step 2: Cinema + Phones & PDAs");
        searchPage.performAdvancedSearch("Cinema", "Phones & PDAs", false, null);
        int phonesCnt = searchPage.getResultCount();
        logWriter.logMessage("   Phones results: " + phonesCnt);
        Assert.assertTrue(searchPage.isEmptyStateDisplayed(),
                "Phones & PDAs should not contain any 'Cinema' product");

        logWriter.logRegistration("Cinema/Components|Phones", true,
                "✓ Category filter narrowed results correctly "
                        + "(Components=" + compCnt + ", Phones=" + phonesCnt + ")");
    }

    // ==================================================================
    // TC_080 – Hierarchical indentation
    // ==================================================================
    @Test(priority = 9, description = "TC_080 – Category dropdown shows indented subcategories")
    public void testCategoryHierarchyIndentation() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_080: Category dropdown indentation");
        logWriter.logMessage("========================================");

        searchPage.navigateToSearchPage();
        List<String> raws = searchPage.getCategoryOptionsRawHtml();

        String compRaw = null, monitorsRaw = null, miceRaw = null;
        for (String raw : raws) {
            if (raw == null) continue;
            if (raw.trim().equals("Components")) compRaw = raw;
            if (raw.contains("Monitors")) monitorsRaw = raw;
            if (raw.contains("Mice and Trackballs")) miceRaw = raw;
        }

        logWriter.logMessage("   Components raw:         '" + compRaw + "'");
        logWriter.logMessage("   Monitors raw:           '" + monitorsRaw + "'");
        logWriter.logMessage("   Mice and Trackballs raw:'" + miceRaw + "'");

        Assert.assertNotNull(compRaw,     "Components option must exist");
        Assert.assertNotNull(monitorsRaw, "Monitors option must exist");
        Assert.assertNotNull(miceRaw,     "Mice and Trackballs option must exist");

        Assert.assertEquals(compRaw.trim(), "Components",
                "Parent 'Components' must have no leading whitespace");

        Assert.assertTrue(monitorsRaw.startsWith("&nbsp;")
                        || monitorsRaw.startsWith(" "),
                "Child 'Monitors' must be indented with &nbsp; or whitespace, was: "
                        + "'" + monitorsRaw + "'");

        Assert.assertTrue(miceRaw.startsWith("&nbsp;")
                        || miceRaw.startsWith(" "),
                "Child 'Mice and Trackballs' must be indented, was: '" + miceRaw + "'");

        logWriter.logRegistration("category dropdown", true,
                "✓ Hierarchy indentation verified (children prefixed with &nbsp;)");
    }

    // ==================================================================
    // TC_081 – Subcategory checkbox enabled/disabled state
    // ==================================================================
    @Test(priority = 10, description = "TC_081 – Subcategory checkbox disabled for All Categories")
    public void testSubcategoryCheckboxToggle() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_081: Subcategory checkbox enable/disable");
        logWriter.logMessage("========================================");

        searchPage.navigateToSearchPage();
        searchPage.selectCategory("All Categories");
        boolean disabledOnAll = !searchPage.isSubcategoryCheckboxEnabled();
        logWriter.logMessage("   With 'All Categories' — checkbox enabled? "
                + searchPage.isSubcategoryCheckboxEnabled());
        Assert.assertTrue(disabledOnAll,
                "Subcategory checkbox must be DISABLED when 'All Categories' selected");

        searchPage.selectCategory("Components");
        boolean enabledOnComponents = searchPage.isSubcategoryCheckboxEnabled();
        logWriter.logMessage("   With 'Components' — checkbox enabled? "
                + enabledOnComponents);
        Assert.assertTrue(enabledOnComponents,
                "Subcategory checkbox must be ENABLED when a specific category is selected");

        logWriter.logRegistration("subcategory checkbox", true,
                "✓ Checkbox toggles correctly (disabled → enabled)");
    }

    // ==================================================================
    // TC_082 – Subcategories checked includes child items
    // ==================================================================
    @Test(priority = 11, description = "TC_082 – Subcategories checked includes child-category items")
    public void testSubcategoriesCheckedIncludesChildItems() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_082: Subcategories CHECKED includes child items");
        logWriter.logMessage("========================================");

        searchPage.performAdvancedSearch("Cinema", "Components", false, true);
        int cnt = searchPage.getResultCount();
        List<String> names = searchPage.getResultProductNames();
        logWriter.logMessage("   Result count: " + cnt);
        logWriter.logMessage("   Products:     " + names);

        Assert.assertTrue(cnt > 0,
                "With subcategories CHECKED, 'Apple Cinema 30' (in Components > Monitors) "
                        + "must be returned under the parent 'Components' category");

        logWriter.logRegistration("Cinema/Components/checked", true,
                "✓ Subcategories-checked returned " + cnt + " product(s): " + names);
    }

    // ==================================================================
    // TC_083 – Subcategories unchecked excludes child items
    // ==================================================================
    @Test(priority = 12, description = "TC_083 – Subcategories unchecked excludes child-category items")
    public void testSubcategoriesUncheckedExcludesChildItems() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_083: Subcategories UNCHECKED excludes child items");
        logWriter.logMessage("========================================");

        searchPage.performAdvancedSearch("Cinema", "Components", false, false);
        int cnt = searchPage.getResultCount();
        logWriter.logMessage("   Result count: " + cnt);

        Assert.assertTrue(searchPage.isEmptyStateDisplayed(),
                "With subcategories UNCHECKED, 'Apple Cinema 30' (only in Monitors child "
                        + "of Components) must NOT appear under 'Components'");

        logWriter.logRegistration("Cinema/Components/unchecked", true,
                "✓ Subcategories-unchecked correctly excluded child items");
    }

    // ==================================================================
    // TC_084 – Empty state message
    // ==================================================================
    @Test(priority = 13, description = "TC_084 – Empty state message shown for non-matching keyword")
    public void testEmptyStateMessageDisplayed() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_084: Empty state message");
        logWriter.logMessage("Keyword: 'test'");
        logWriter.logMessage("========================================");

        searchPage.navigateToSearchPage();
        searchPage.setSearchInDescription(false);
        searchPage.enterKeyword("test");
        searchPage.clickSearch();

        Assert.assertTrue(searchPage.isResultsHeadingDisplayed(),
                "Results heading 'Products meeting the search criteria' must appear");
        Assert.assertTrue(searchPage.isEmptyStateDisplayed(),
                "Empty state must be shown for non-matching keyword");

        String msg = searchPage.getEmptyMessageText();
        logWriter.logMessage("   Empty message: '" + msg + "'");
        Assert.assertEquals(msg, "There is no product that matches the search criteria.",
                "Empty message text must match exactly");

        logWriter.logRegistration("test", true,
                "✓ Empty state message rendered exactly as expected");
    }

    // ==================================================================
    // TC_085 – Dynamic title + H1 + breadcrumb
    // ==================================================================
    @Test(priority = 14, description = "TC_085 – Page title, H1 and breadcrumb update with keyword")
    public void testDynamicTitleH1AndBreadcrumb() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_085: Dynamic title/H1/breadcrumb");
        logWriter.logMessage("Keyword: 'Monitors'");
        logWriter.logMessage("========================================");

        searchPage.performSearch("Monitors");
        String title = searchPage.getPageTitle();
        String h1    = searchPage.getH1Text();
        List<String> crumbs = searchPage.getBreadcrumbTexts();

        logWriter.logMessage("   Title:      '" + title + "'");
        logWriter.logMessage("   H1:         '" + h1 + "'");
        logWriter.logMessage("   Breadcrumbs: " + crumbs);

        Assert.assertTrue(title.contains("Search - Monitors"),
                "Page title must contain 'Search - Monitors', was: " + title);
        Assert.assertTrue(h1.contains("Search - Monitors"),
                "H1 must contain 'Search - Monitors', was: " + h1);
        Assert.assertTrue(crumbs.contains("Search"),
                "Breadcrumb must include 'Search', was: " + crumbs);

        logWriter.logRegistration("Monitors", true,
                "✓ Dynamic title/H1/breadcrumb confirmed");
    }

    // ==================================================================
    // TC_141 – Keyword preserved in the search input
    // ==================================================================
    @Test(priority = 15, description = "TC_141 – Search input retains submitted keyword")
    public void testKeywordPreservedInInput() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_141: Keyword preserved after search");
        logWriter.logMessage("========================================");

        searchPage.performSearch("Mac");
        String inputValue = searchPage.getSearchInputValue();
        logWriter.logMessage("   Input value after search: '" + inputValue + "'");

        Assert.assertEquals(inputValue, "Mac",
                "Search input must retain 'Mac' after submission");
        logWriter.logRegistration("Mac", true, "✓ Keyword preserved in input");
    }

    // ==================================================================
    // TC_142 – Very long keyword
    // ==================================================================
    @Test(priority = 16, description = "TC_142 – 500-char keyword does not crash the page")
    public void testLongKeyword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) sb.append('A');
        String longKw = sb.toString();

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_142: Very long keyword (" + longKw.length() + " chars)");
        logWriter.logMessage("========================================");

        searchPage.performSearch(longKw);
        String src = driver.getPageSource();
        Assert.assertFalse(src.contains("Fatal error"),
                "No PHP fatal error for long keyword");
        Assert.assertFalse(src.contains("SQLSTATE"),
                "No SQL error for long keyword");
        Assert.assertTrue(searchPage.getH1Text().startsWith("Search - "),
                "Page must still render normally");

        logWriter.logRegistration("[" + longKw.length() + " chars]", true,
                "✓ Long keyword handled without error");
    }

    // ==================================================================
    // TC_143 – Result items are product links
    // ==================================================================
    @Test(priority = 17, description = "TC_143 – Every result is a clickable product link")
    public void testResultLinksPresent() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_143: Result items contain product links");
        logWriter.logMessage("========================================");

        searchPage.performSearch("Mac");
        int cnt = searchPage.getResultCount();
        List<WebElement> links = searchPage.getProductLinks();
        logWriter.logMessage("   Products: " + cnt + " | Links found: " + links.size());

        Assert.assertTrue(cnt > 0, "Precondition: at least one product must exist");
        Assert.assertFalse(links.isEmpty(),
                "Result items must expose at least one product link");

        for (WebElement link : links) {
            String href = link.getAttribute("href");
            Assert.assertTrue(href != null && href.contains("product_id="),
                    "Every product link must contain product_id, was: " + href);
        }

        logWriter.logRegistration("Mac", true,
                "✓ " + links.size() + " product link(s) verified");
    }

    // ==================================================================
    // TC_144 – Enter key triggers search
    // ==================================================================
    @Test(priority = 18, description = "TC_144 – Enter key submits the search form")
    public void testEnterKeyTriggerSearch() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_144: Enter key triggers search");
        logWriter.logMessage("========================================");

        searchPage.navigateToSearchPage();
        searchPage.enterKeyword("Mac");
        searchPage.submitWithEnterKey();

        Assert.assertTrue(driver.getCurrentUrl().contains("search=Mac"),
                "URL must contain search=Mac after Enter, was: " + driver.getCurrentUrl());
        Assert.assertTrue(searchPage.getResultCount() > 0,
                "Enter key must produce results just like the Search button");

        logWriter.logRegistration("Mac (Enter key)", true,
                "✓ Enter key triggered search successfully");
    }

    // ==================================================================
    // TC_145 – Header search box redirects to search results
    // ==================================================================
    @Test(priority = 19, description = "TC_145 – Global header search form redirects to results")
    public void testHeaderSearchForm() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_145: Header search form");
        logWriter.logMessage("========================================");

        searchPage.searchFromHeader("Mac");
        String url = driver.getCurrentUrl();
        logWriter.logMessage("   URL after header search: " + url);

        Assert.assertTrue(url.contains("route=product/search"),
                "Header search must land on the product search page, was: " + url);
        Assert.assertTrue(searchPage.getResultCount() > 0,
                "Header search must return results for 'Mac'");

        logWriter.logRegistration("Mac (header form)", true,
                "✓ Header form redirected to search results successfully");
    }

    // ==================================================================
    // TC_146 – Special characters
    // ==================================================================
    @Test(priority = 20, description = "TC_146 – Special characters are handled safely")
    public void testSpecialCharactersInKeyword() {
        String payload = "c++ #tag & name=test";

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_146: Special characters");
        logWriter.logMessage("Payload: '" + payload + "'");
        logWriter.logMessage("========================================");

        searchPage.performSearch(payload);
        String src = driver.getPageSource();

        Assert.assertFalse(src.contains("Fatal error"),
                "No PHP fatal error for special chars");
        Assert.assertFalse(src.contains("SQLSTATE"),
                "No SQL error for special chars");
        Assert.assertTrue(searchPage.getH1Text().startsWith("Search - "),
                "Page must still render normally");

        logWriter.logRegistration(payload, true,
                "✓ Special characters handled — no error, page renders");
    }

    // ==================================================================
    // TC_147 – Sequential searches
    // ==================================================================
    @Test(priority = 21, description = "TC_147 – Sequential searches replace previous results")
    public void testSequentialSearches() {
        logWriter.logMessage("\n========================================");
        logWriter.logMessage("TC_147: Sequential searches");
        logWriter.logMessage("========================================");

        searchPage.performSearch("Mac");
        int macCount = searchPage.getResultCount();
        String macH1 = searchPage.getH1Text();
        logWriter.logMessage("   Search 'Mac': " + macCount + " results | H1: " + macH1);
        Assert.assertTrue(macH1.contains("Mac"),
                "H1 must reflect 'Mac' after first search");

        searchPage.performSearch("Cinema");
        int cinemaCount = searchPage.getResultCount();
        String cinemaH1 = searchPage.getH1Text();
        logWriter.logMessage("   Search 'Cinema': " + cinemaCount + " results | H1: " + cinemaH1);
        Assert.assertTrue(cinemaH1.contains("Cinema"),
                "H1 must reflect 'Cinema' after second search");
        Assert.assertFalse(cinemaH1.contains("Mac"),
                "H1 must NOT retain previous keyword 'Mac'");

        searchPage.performSearch("Apple Cinema 30");
        String finalH1 = searchPage.getH1Text();
        int finalCnt = searchPage.getResultCount();
        logWriter.logMessage("   Search 'Apple Cinema 30': " + finalCnt
                + " results | H1: " + finalH1);
        Assert.assertTrue(finalH1.contains("Apple Cinema 30"),
                "H1 must reflect the exact final keyword");

        logWriter.logRegistration("Mac→Cinema→Apple Cinema 30", true,
                "✓ Each search replaced prior results correctly");
    }
}