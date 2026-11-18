package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class for writing test execution logs
 */
public class LogWriter {

    private String logFilePath;
    private BufferedWriter writer;
    private SimpleDateFormat dateFormat;

    /**
     * Constructor that creates the log file with timestamp
     */
    public LogWriter() {
        dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        String logFileName = "TestLog_" + timestamp + ".txt";

        // Create logs directory if it does not exist
        File logDir = new File("src/main/resources/logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        this.logFilePath = logDir.getPath() + File.separator + logFileName;

        try {
            writer = new BufferedWriter(new FileWriter(logFilePath, true));
            writeHeader();
        } catch (IOException e) {
            System.err.println("Error creating log file: " + e.getMessage());
        }
    }

    /**
     * Constructor with custom path
     */
    public LogWriter(String customPath) {
        dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        this.logFilePath = customPath;

        try {
            File file = new File(logFilePath);
            file.getParentFile().mkdirs();
            writer = new BufferedWriter(new FileWriter(logFilePath, true));
            writeHeader();
        } catch (IOException e) {
            System.err.println("Error creating log file: " + e.getMessage());
        }
    }

    /**
     * Writes the log header
     */
    private void writeHeader() throws IOException {
        String separator = "========================================";
        writer.write(separator);
        writer.newLine();
        writer.write("TEST EXECUTION REPORT - OpenCart");
        writer.newLine();
        writer.write("Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
        writer.newLine();
        writer.write(separator);
        writer.newLine();
        writer.newLine();
        writer.flush();
    }

    /**
     * Writes user registration information
     */
    public void logRegistration(String email, boolean successful, String message) {
        try {
            writer.write("[REGISTRATION] ");
            writer.write(getCurrentTimestamp());
            writer.newLine();
            writer.write("  Email: " + email);
            writer.newLine();
            writer.write("  Status: " + (successful ? "SUCCESS" : "FAILED"));
            writer.newLine();
            if (message != null && !message.isEmpty()) {
                writer.write("  Message: " + message);
                writer.newLine();
            }
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        }
    }

    /**
     * Writes login information
     */
    public void logLogin(String email, boolean successful, String message) {
        try {
            writer.write("[LOGIN] ");
            writer.write(getCurrentTimestamp());
            writer.newLine();
            writer.write("  Email: " + email);
            writer.newLine();
            writer.write("  Status: " + (successful ? "SUCCESS" : "FAILED"));
            writer.newLine();
            if (message != null && !message.isEmpty()) {
                writer.write("  Message: " + message);
                writer.newLine();
            }
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        }
    }

    /**
     * Writes information for product added to cart
     */
    public void logProductAdded(String category, String subCategory, String product,
                                String quantity, boolean successful) {
        try {
            writer.write("[PRODUCT ADDED] ");
            writer.write(getCurrentTimestamp());
            writer.newLine();
            writer.write("  Category: " + category);
            writer.newLine();
            if (subCategory != null && !subCategory.isEmpty()) {
                writer.write("  Subcategory: " + subCategory);
                writer.newLine();
            }
            writer.write("  Product: " + product);
            writer.newLine();
            writer.write("  Quantity: " + quantity);
            writer.newLine();
            writer.write("  Status: " + (successful ? "SUCCESS" : "FAILED"));
            writer.newLine();
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        }
    }

    /**
     * Writes cart verification information
     */
    public void logCartVerification(int expectedProducts, int foundProducts,
                                    boolean successful) {
        try {
            writer.write("[CART VERIFICATION] ");
            writer.write(getCurrentTimestamp());
            writer.newLine();
            writer.write("  Expected Products: " + expectedProducts);
            writer.newLine();
            writer.write("  Found Products: " + foundProducts);
            writer.newLine();
            writer.write("  Status: " + (successful ? "SUCCESS" : "FAILED"));
            writer.newLine();
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        }
    }

    /**
     * Writes a generic message to the log
     */
    public void logMessage(String message) {
        try {
            writer.write(getCurrentTimestamp() + " - " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        }
    }

    /**
     * Writes a section separator
     */
    public void logSection(String sectionName) {
        try {
            writer.newLine();
            writer.write("========== " + sectionName + " ==========");
            writer.newLine();
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        }
    }

    /**
     * Gets the current timestamp formatted
     */
    private String getCurrentTimestamp() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
    }

    /**
     * Closes the writer
     */
    public void close() {
        try {
            if (writer != null) {
                writer.write("\n========== END OF REPORT ==========\n");
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing log: " + e.getMessage());
        }
    }

    /**
     * Gets the log file path
     */
    public String getLogFilePath() {
        return logFilePath;
    }
}