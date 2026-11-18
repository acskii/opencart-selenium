package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for reading data from Excel files using Apache POI
 */
public class ExcelReader {

    private final String filePath;

    public ExcelReader(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Reads data from a specific sheet and returns it as a list of maps
     * @param sheetName name of the sheet to read
     * @return List of maps where each map represents a row (key = column name, value = data)
     */
    public List<Map<String, String>> readSheet(String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();

        // Read attempts to handle file locking by another process (e.g., Excel open)
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try (FileInputStream fis = new FileInputStream(filePath);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheet(sheetName);

                if (sheet == null) {
                    System.err.println("Sheet '" + sheetName + "' does not exist in the file.");
                    return data;
                }

                // Get column names from the first row
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    System.err.println("The sheet is empty.");
                    return data;
                }

                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(getCellValueAsString(cell));
                }

                // Read data from subsequent rows
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Map<String, String> rowData = new HashMap<>();
                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = row.getCell(j);
                        String value = (cell != null) ? getCellValueAsString(cell) : "";
                        rowData.put(headers.get(j), value);
                    }

                    // Only add rows that are not completely empty
                    if (!isRowEmpty(rowData)) {
                        data.add(rowData);
                    }
                }

                // Successful read, exit the loop
                return data;

            } catch (IOException e) {
                boolean locked = (e instanceof FileNotFoundException) ||
                        (e.getMessage() != null && e.getMessage().toLowerCase().contains("being used"));
                System.err.println("Error reading the Excel file (attempt " + attempt + "/" + maxAttempts + "): " + e.getMessage());
                if (locked && attempt < maxAttempts) {
                    System.err.println("It seems the file is in use. Close Excel if you have it open and we will try again...");
                    try { Thread.sleep(600); } catch (InterruptedException ie) { /* ignore */ }
                    continue;
                } else {
                    e.printStackTrace();
                    break;
                }
            }
        }

        return data;
    }

    /**
     * Converts a cell value to String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Convert number to string without scientific notation
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * Checks if a row is completely empty
     */
    private boolean isRowEmpty(Map<String, String> rowData) {
        for (String value : rowData.values()) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads user data for registration
     */
    public List<Map<String, String>> readRegisterData() {
        return readSheet("RegisterData");
    }

    /**
     * Reads login data
     */
    public List<Map<String, String>> readLoginData() {
        return readSheet("LoginData");
    }

    /**
     * Reads product data for search
     */
    public List<Map<String, String>> readProductSearchData() {
        return readSheet("ProductSearch");
    }
}