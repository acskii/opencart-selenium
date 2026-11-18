import pages.RegisterPage;
import utils.ExcelReader;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.Map;

/**
 * User Registration Test
 * Reads test data from Excel (positive and negative cases) and validates
 * both successful and expected-failure registrations on OpenCart.
 */
public class RegisterTest extends BaseTest {

    private RegisterPage registerPage;
    private ExcelReader excelReader;

    @BeforeClass
    public void setupTest() {
        registerPage = new RegisterPage(driver);
        excelReader  = new ExcelReader("src/main/resources/testData.xlsx");
        logWriter.logSection("USER REGISTRATION TEST");
    }

    @Test(priority = 1, description = "Register using test data from testData Excel sheet")
    public void testRegister() {
        List<Map<String, String>> users = excelReader.readRegisterData();

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("Test rows to run: " + users.size());
        logWriter.logMessage("========================================");

        SoftAssert softAssert = new SoftAssert();
        int passed = 0;
        int failed = 0;

        for (int i = 0; i < users.size(); i++) {
            Map<String, String> user = users.get(i);
            String firstName      = valueOrEmpty(user.get("First Name"));
            String lastName       = valueOrEmpty(user.get("Last Name"));
            String email          = valueOrEmpty(user.get("E-Mail"));
            String password       = valueOrEmpty(user.get("Password"));
            String expectedResult = valueOrEmpty(user.get("Expected Result")).toLowerCase();
            String expectedError  = valueOrEmpty(user.get("Expected Error Contains"));
            boolean expectedSuccess = expectedResult.isEmpty() || expectedResult.startsWith("succ");

            logWriter.logMessage("\n--- Case " + (i + 1) + "/" + users.size()
                    + " | expected=" + (expectedSuccess ? "SUCCESS" : "FAILURE")
                    + " | email='" + email + "' ---");
            System.out.println("\n=== Case " + (i + 1) + "/" + users.size()
                    + " | " + (expectedSuccess ? "EXPECTED SUCCESS" : "EXPECTED FAILURE")
                    + " | " + (email.isEmpty() ? "<empty email>" : email) + " ===");

            try {
                registerPage.navigateToRegisterPage();
                registerPage.disableHtml5Validation();
                Thread.sleep(200);

                boolean registered = registerPage.registerUser(firstName, lastName, email, password);
                String  errorText  = registerPage.getAnyErrorMessage();

                if (expectedSuccess) {
                    if (registered) {
                        String successMsg = registerPage.getSuccessMessage();
                        logWriter.logRegistration(email, true, "✓ Registered: " + successMsg);
                        System.out.println("✓ Registered: " + email);
                        passed++;
                    } else if (errorText.toLowerCase().contains("already registered")) {
                        logWriter.logRegistration(email, false, "⚠ Already registered (re-run OK): " + errorText);
                        System.out.println("⚠ Already registered (soft-pass): " + email);
                        passed++;
                    } else {
                        String msg = "Expected SUCCESS but got error: " + errorText;
                        logWriter.logRegistration(email, false, "✗ " + msg);
                        System.out.println("✗ " + msg);
                        softAssert.fail(msg);
                        failed++;
                    }
                } else {
                    if (registered) {
                        String msg = "Expected FAILURE but the user was registered: " + email;
                        logWriter.logRegistration(email, false, "✗ " + msg);
                        System.out.println("✗ " + msg);
                        softAssert.fail(msg);
                        failed++;
                    } else if (errorText.isEmpty()) {
                        String msg = "Expected FAILURE with error, but no error message was found";
                        logWriter.logRegistration(email, false, "✗ " + msg);
                        System.out.println("✗ " + msg);
                        softAssert.fail(msg);
                        failed++;
                    } else if (!expectedError.isEmpty()
                            && !errorText.toLowerCase().contains(expectedError.toLowerCase())) {
                        String msg = "Expected error containing '" + expectedError
                                + "' but got: '" + errorText + "'";
                        logWriter.logRegistration(email, false, "⚠ " + msg);
                        System.out.println("⚠ " + msg);
                        softAssert.fail(msg);
                        failed++;
                    } else {
                        logWriter.logRegistration(email, true, "✓ Expected failure reproduced: " + errorText);
                        System.out.println("✓ Expected failure reproduced for: "
                                + (email.isEmpty() ? "<empty email>" : email));
                        passed++;
                    }
                }

                Thread.sleep(500);

            } catch (Exception e) {
                String msg = "Exception while running case " + (i + 1) + ": " + e.getMessage();
                logWriter.logRegistration(email, false, "✗ " + msg);
                System.out.println("✗ " + msg);
                e.printStackTrace();
                softAssert.fail(msg);
                failed++;
            }
        }

        logWriter.logMessage("\n========================================");
        logWriter.logMessage("USER REGISTRATION SUMMARY");
        logWriter.logMessage("Total cases:  " + users.size());
        logWriter.logMessage("Passed:       " + passed);
        logWriter.logMessage("Failed:       " + failed);
        logWriter.logMessage("========================================");

        System.out.println("\n========================================");
        System.out.println("SUMMARY: " + passed + " passed, " + failed + " failed out of "
                + users.size() + " total");
        System.out.println("========================================");

        softAssert.assertAll();
    }

    private static String valueOrEmpty(String v) {
        return v == null ? "" : v.trim();
    }
}