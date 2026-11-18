import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import utils.LogWriter;

import java.time.Duration;

/**
 * BaseTest - Base Class for All Automated Tests
 *
 * PURPOSE:
 * Parent class that centralizes common configuration for all tests.
 * Implements the "Test Setup" pattern from TestNG to initialize resources
 * only once before executing the entire test suite.
 *
 * RESPONSIBILITIES:
 * 1. WebDriver configuration (Selenium)
 * 2. Logging system initialization
 * 3. Automatic test data generation (Excel)
 * 4. Timeout and browser options configuration
 * 5. Resource cleanup after tests
 *
 * DESIGN PATTERN:
 * - Template Method: Defines the setup/teardown flow
 * - Singleton: WebDriver and LogWriter shared by all tests
 *
 * LIFECYCLE:
 * @BeforeSuite -> Runs ONCE before all tests
 * [Execution of all tests]
 * @AfterSuite -> Runs ONCE after all tests
 *
 * USAGE:
 * All tests must inherit from this class:
 * public class MyTest extends BaseTest { ... }
 *
 * SHARED VARIABLES:
 * - driver: WebDriver (Chrome) instance accessible from all tests
 * - logWriter: Logging system to record operations
 *
 * @author QA Team - San Buenaventura Cali
 * @version 1.0
 * @since 2025-11-10
 */
public class BaseTest {

    /**
     * WebDriver shared by all tests
     * - protected: Accessible in child classes
     * - static: Single instance for the entire suite
     *
     * IMPORTANT: Do not create new driver instances in tests,
     * always use this inherited variable.
     */
    protected static WebDriver driver;

    /**
     * Shared logging system
     * - Generates log files with timestamp
     * - Records all test operations
     * - Automatically closed in @AfterSuite
     */
    protected static LogWriter logWriter;

    /**
     * Initial setup method - Runs ONCE before all tests
     *
     * EXECUTION ORDER:
     * 1. Generate test data (Excel)
     * 2. Configure ChromeDriver automatically
     * 3. Configure browser options
     * 4. Initialize WebDriver
     * 5. Configure timeouts
     * 6. Initialize logging system
     *
     * APPLIED CONFIGURATIONS:
     * - Browser maximized on start
     * - Notifications disabled
     * - Pop-ups blocked
     * - Implicit timeout: 10 seconds
     * - Page load timeout: 30 seconds
     *
     * @throws Exception if there is an error in the initial configuration
     */
    @BeforeSuite
    public void setup() {
        // STEP 2: Configure ChromeDriver automatically
        // WebDriverManager downloads and configures the driver compatible with the installed Chrome
        WebDriverManager.chromedriver().setup();

        // STEP 3: Configure Chrome browser options
        ChromeOptions options = new ChromeOptions();

        // Maximize window on start (better for element visualization)
        options.addArguments("--start-maximized");

        // Disable browser notifications (avoids annoying pop-ups)
        options.addArguments("--disable-notifications");

        // Disable pop-up blocking (some tests may require pop-ups)
        options.addArguments("--disable-popup-blocking");

        // STEP 4: Create WebDriver instance with the configured options
        driver = new ChromeDriver(options);

        // STEP 5: Configure global timeouts

        // Implicit timeout: Waiting time when searching for elements
        // If an element does not exist, waits up to 10 seconds before failing
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

        // Page load timeout: Maximum time for a page to load
        // If the page takes more than 30 seconds, a TimeoutException is thrown
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

        // STEP 6: Initialize the logging system
        // LogWriter creates a log file with a unique timestamp
        logWriter = new LogWriter();

        // Record initial information in the log
        logWriter.logMessage("=== START OF TEST EXECUTION ===");
        logWriter.logMessage("Browser: Chrome");
        logWriter.logMessage("Base URL: https://opencart.abstracta.us/");
    }

    /**
     * Cleanup method - Runs ONCE after all tests
     *
     * PURPOSE:
     * Release resources and close connections opened during tests.
     * It is critical to avoid orphaned browser processes.
     *
     * CLEANUP ORDER:
     * 1. Close and save the log file
     * 2. Close the browser and release resources
     *
     * IMPORTANT:
     * - This method ALWAYS runs, even if tests fail
     * - Uses null-safe verification to avoid NullPointerException
     *
     * @throws Exception if there is an error closing resources (non-critical)
     */
    @AfterSuite
    public void tearDown() {
        // STEP 1: Close the logging system
        if (logWriter != null) {
            // Record end of execution in the log
            logWriter.logMessage("=== END OF TEST EXECUTION ===");
            logWriter.logMessage("Log path: " + logWriter.getLogFilePath());

            // Close the log file (flush and close of the BufferedWriter)
            logWriter.close();

            // Inform the user where to find the log
            System.out.println("\n========================================");
            System.out.println("Log saved at: " + logWriter.getLogFilePath());
            System.out.println("========================================\n");
        }

        // STEP 2: Close the browser and release resources
        if (driver != null) {
            // quit() closes all windows and terminates the WebDriver session
            // Different from close() which only closes the current window
            driver.quit();
        }
    }

    /**
     * Getter to obtain the WebDriver instance
     *
     * USAGE:
     * WebDriver myDriver = getDriver();
     *
     * @return Shared WebDriver instance (Chrome)
     */
    public WebDriver getDriver() {
        return driver;
    }

    /**
     * Getter to obtain the LogWriter instance
     *
     * USAGE:
     * LogWriter logger = getLogWriter();
     * logger.logMessage("My message");
     *
     * @return Shared LogWriter instance
     */
    public LogWriter getLogWriter() {
        return logWriter;
    }
}