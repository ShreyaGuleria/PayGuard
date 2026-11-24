/**
 * Module: com.payguard.io.CsvExporter
 * Purpose: Writes transaction reports to a CSV file.
 * Module lead (reviews and explains this module): Shreya
 * Viva focus: CSV escaping and resource handling
 * Concepts used: OOP, CSV, try-with-resources, java.nio
 */
package com.payguard.io;

import com.payguard.model.Transaction;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Exports transaction rows in a format that spreadsheet programs can read. */
public class CsvExporter {

    /** Writes a header and each transaction, closing the file even if writing fails. */
    public void exportTransactions(List<Transaction> transactions, Path outputFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write("id,sender_account_id,receiver_account_id,amount,timestamp,status");
            writer.newLine();

            for (Transaction transaction : transactions) {
                writer.write(transaction.getId() + ","
                        + transaction.getSenderAccountId() + ","
                        + transaction.getReceiverAccountId() + ","
                        + csvField(transaction.getAmount().toPlainString()) + ","
                        + csvField(transaction.getTimestamp().toString()) + ","
                        + csvField(transaction.getStatus().name()));
                writer.newLine();
            }
        }
    }

    /** Quotes only fields that need it and doubles inner quotes for valid CSV output. */
    private String csvField(String value) {
        if (!value.contains(",") && !value.contains("\"")) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
