/**
 * Module: com.payguard.io.CsvImporter
 * Purpose: Reads and validates account rows from a CSV file.
 * Module lead (reviews and explains this module): Shreya
 * Viva focus: CSV parsing, validation, and resources
 * Concepts used: OOP, CSV, JDBC, try-with-resources
 */
package com.payguard.io;

import com.payguard.dao.AccountDAO;
import com.payguard.model.Account;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Imports accounts with two CSV columns: holder_name and balance. */
public class CsvImporter {

    /** Reads the file, skips invalid rows, saves valid accounts, and prints an import summary. */
    public void importAccounts(Path inputFile, AccountDAO accountDAO) throws IOException, SQLException {
        int importedCount = 0;
        int skippedCount = 0;
        int rowsRead = 0;
        int lineNumber = 0;
        boolean firstNonEmptyRow = true;

        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                boolean countedAsDataRow = false;
                try {
                    List<String> fields = parseCsvLine(line, lineNumber);
                    if (firstNonEmptyRow && isHeader(fields)) {
                        firstNonEmptyRow = false;
                        continue;
                    }
                    firstNonEmptyRow = false;
                    rowsRead++;
                    countedAsDataRow = true;
                    validateAndImport(fields, accountDAO, lineNumber);
                    importedCount++;
                } catch (IllegalArgumentException exception) {
                    firstNonEmptyRow = false;
                    if (!countedAsDataRow) {
                        rowsRead++;
                    }
                    skippedCount++;
                    System.out.println("Skipping line " + lineNumber + ": " + exception.getMessage());
                }
            }
        }
        System.out.println("Import summary: rows read=" + rowsRead
                + ", imported=" + importedCount + ", skipped=" + skippedCount);
    }

    /** Checks the two required fields and saves the row only after all values are valid. */
    private void validateAndImport(List<String> fields, AccountDAO accountDAO, int lineNumber)
            throws SQLException {
        if (fields.size() != 2) {
            throw invalidRow(lineNumber, "expected holder_name and balance columns");
        }

        String holderName = fields.get(0).trim();
        if (holderName.isEmpty()) {
            throw invalidRow(lineNumber, "holder name cannot be empty");
        }

        BigDecimal balance;
        try {
            balance = new BigDecimal(fields.get(1).trim());
        } catch (NumberFormatException exception) {
            throw invalidRow(lineNumber, "balance must be a valid decimal number");
        }
        if (balance.signum() < 0) {
            throw invalidRow(lineNumber, "balance cannot be negative");
        }

        accountDAO.create(new Account(0, holderName, balance));
    }

    /** Checks whether the first non-empty row uses the documented column names. */
    private boolean isHeader(List<String> fields) {
        return fields.size() == 2
                && fields.get(0).trim().equalsIgnoreCase("holder_name")
                && fields.get(1).trim().equalsIgnoreCase("balance");
    }

    /**
     * Splits one CSV row while respecting quoted commas and doubled quote characters.
     * This keeps the importer small while supporting names that contain commas.
     */
    private List<String> parseCsvLine(String line, int lineNumber) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (insideQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (character == ',' && !insideQuotes) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }

        if (insideQuotes) {
            throw invalidRow(lineNumber, "quoted field is not closed");
        }
        fields.add(field.toString());
        return fields;
    }

    /** Builds a clear validation error that points to the source row. */
    private IllegalArgumentException invalidRow(int lineNumber, String reason) {
        return new IllegalArgumentException("Invalid CSV row " + lineNumber + ": " + reason + ".");
    }
}
