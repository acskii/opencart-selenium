# Test Execution Report — OpenCart Product Search

- **Date:** 19/11/2026 19:59:26 – 20:00:19
- **Browser:** Chrome
- **Base URL:** `http://localhost/opencart-4.1.0.4/upload/`
- **Source Log:** `src\main\resources\logs\TestLog_20261119_195926.txt`

## Automation Test Results

| Test Case                                                            | Test Input / Target | Expected Behavior | Automation Result | Details / Notes |
|:---------------------------------------------------------------------| :--- | :--- | :---: | :--- |
| **TC_073**                                                           | `Keyword='Mac'` | Search returns products matching 'Mac'; heading updates | **PASS** | Returned **4** product(s). URL carried `search=Mac`; H1 = `'Search - Mac'`. |
| **TC_074**                                                           | `Keyword=''` (empty) and `'   '` (3 spaces) | Empty state shown, no error | **PASS** | Both cases showed *"There is no product that matches the search criteria."*; no PHP fatal error leaked. |
| **TC_075 (XSS)**                                                     | `Payload='alert("XSS")'` | Script does not execute; payload HTML-encoded | **PASS** | No JS alert triggered. H1 rendered literal `'Search - alert("XSS")'` — payload treated as plain text. |
| **TC_075 (SQLi)**                                                    | `Payload="' OR '1'='1"` | No SQL error; page remains usable | **PASS** | No `SQLSTATE` / `Fatal error` / `Warning: mysqli` in source. Follow-up search `'Mac'` still returned results. |
| **TC_076**                                                           | `Keywords='apple'` / `'APPLE'` / `'ApPlE'` | All casings return identical result count | **PASS** | All three returned **1** result. *Note: product-name extraction returned empty list for each casing (theme layout). Count-based assertion passed and casings matched.* |
| **TC_077**                                                           | `Keyword='revolutionary'`, description checkbox **UNCHECKED** | Empty state (keyword lives only in descriptions) | **PASS** | Result count = **0**; empty state shown — description-only keyword correctly excluded. |
| **TC_078**                                                           | `Keyword='revolutionary'`, description checkbox **CHECKED** | At least one product returned | **PASS** | Returned **2** products. *Note: product-name extraction returned empty list — CSS selector `h4 a` doesn't match this theme's card layout; count assertion validated the behavior.* |
| **TC_079**                                                           | `Keyword='Cinema'`, `Category='Components'` | At least 1 product (expected `Apple Cinema 30`) | **FAIL** | Returned **0** products. Step 2 (Phones & PDAs) not reached. **Root cause:** stock demo catalog places `Apple Cinema 30` in the **Monitors** child of Components, not directly in Components. See TC_082 for the correct passing counterpart (subcategories enabled). |
| **TC_080**                                                           | Inspect category dropdown option innerHTML | Subcategories indented with non-breaking spaces | **PASS** | `Components` = `'Components'` (no indent); `Monitors` = `'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Monitors'`; `Mice and Trackballs` = `'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Mice and Trackballs'`. |
| **TC_081**                                                           | Category dropdown toggle: `All Categories` → `Components` | Subcategory checkbox disabled → enabled | **PASS** | With `All Categories`: `enabled=false`; with `Components`: `enabled=true`. |
| **TC_082**                                                           | `Keyword='Cinema'`, `Category='Components'`, **Subcategories CHECKED** | `Apple Cinema 30` returned via child category | **PASS** | Returned **1** product. *Note: name extraction returned empty list — count assertion validated behavior.* |
| **TC_083**                                                           | `Keyword='Cinema'`, `Category='Components'`, **Subcategories UNCHECKED** | No results (product only in child category) | **PASS** | Result count = **0**; empty state shown — child items correctly excluded. |
| **TC_084**                                                           | `Keyword='test'`, description checkbox unchecked | Exact empty state message | **PASS** | Message matched exactly: *"There is no product that matches the search criteria."* |
| **TC_085**                                                           | `Keyword='Monitors'` | Page title, H1 and breadcrumb update dynamically | **PASS** | Title = `'Search - Monitors'`; H1 = `'Search - Monitors'`; breadcrumbs = `[Search]`. |
| **TC_141**                                                           | `Keyword='Mac'` | Search input retains submitted keyword | **PASS** | Input value after results load = `'Mac'`. |
| **TC_142**                                                           | `Keyword='A' × 500` (500 chars) | No crash; no error leakage | **PASS** | Page rendered normally; H1 still begins with `'Search - '`; no `Fatal error` / `SQLSTATE` in source. |
| **TC_143**                                                           | `Keyword='Mac'` | Every result item exposes a product link | **PASS** | 4 products → 8 anchor links found (thumbnail image + title per card). Every `href` contains `product_id=`. |
| **TC_144**                                                           | `Keyword='Mac'` submitted via **Enter key** | Same result as clicking Search button | **PASS** | URL contained `search=Mac`; 4 results shown. |
| **TC_145**                                                           | `Keyword='Mac'` submitted via **header (global) form** | Redirect to `route=product/search` results page | **PASS** | Landed on `...route=product/search&search=Mac` with 4 results. |
| **TC_146**                                                           | `Keyword='c++ #tag & name=test'` | Special characters encoded safely | **PASS** | Page rendered normally; no `Fatal error` / `SQLSTATE`; H1 begins with `'Search - '`. |
| **TC_147**                                                           | Sequential: `Mac` → `Cinema` → `Apple Cinema 30` | Each search replaces prior results | **PASS** | `Mac` = 4 results; `Cinema` = 1 result; `Apple Cinema 30` = 1 result. H1 updated to the exact final keyword; no stale data from previous queries. |

## Summary

- **Total automated test cases:** 21
- **Passed:** 20
- **Failed:** 1 (TC_079)

## Notes

1. **TC_079 failure — catalog dependency, not a defect.** The stock OpenCart 4.x demo data places `Apple Cinema 30` under `Components > Monitors` (child), not directly inside `Components`. Because TC_079 does not enable *"Search in subcategories"*, the product is excluded — this is **expected site behavior**. The dedicated passing counterpart is **TC_082** (subcategories enabled), which correctly returns the product. Recommendation: either (a) accept TC_079 as a documented "catalog behavior" expectation rather than a failure, or (b) create a test-only product directly under `Components` in the demo data.

2. **Product-name extraction returned empty lists** in TC_076, TC_078, TC_082. The page theme renders product cards without an `h4 > a` element, so `SearchPage.getResultProductNames()` found no matching anchors. The **count-based assertions** still validated the intended behavior, so these tests legitimately passed. Future improvement: update the selector in `getResultProductNames()` to match this theme (e.g., `.product-thumb .description a` or `a[href*='product_id=']`).

3. **TC_143 link count (8 links / 4 products)** — each product card contains two anchors: one for the thumbnail image and one for the product-name text, both containing `product_id=`. All were verified; no defect.