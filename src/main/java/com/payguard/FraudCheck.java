/**
 * Module: com.payguard.FraudCheck
 * Purpose: Demonstrates the three fraud rules with a small hand-made dataset.
 * Module lead (reviews and explains this module): Shreya
 * Viva focus: Fraud rules with example transactions
 * Concepts used: Streams, lambdas, java.time
 */
package com.payguard;

import com.payguard.fraud.FraudDetector;
import com.payguard.model.Transaction;
import com.payguard.model.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/** Runs the fraud examples without a database or test framework. */
public class FraudCheck {

    /** Creates one example for each rule and prints the IDs each rule matches. */
    public static void main(String[] args) {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 12, 0);
        List<Transaction> transactions = List.of(
                transaction(1, 1, "15000.00", start, TransactionStatus.PENDING),
                transaction(2, 2, "100.00", start, TransactionStatus.PENDING),
                transaction(3, 2, "100.00", start.plusSeconds(10), TransactionStatus.PENDING),
                transaction(4, 2, "100.00", start.plusSeconds(20), TransactionStatus.PENDING),
                transaction(5, 2, "100.00", start.plusSeconds(30), TransactionStatus.PENDING),
                transaction(6, 3, "50.00", start, TransactionStatus.FAILED),
                transaction(7, 3, "50.00", start.plusSeconds(1), TransactionStatus.FAILED),
                transaction(8, 3, "50.00", start.plusSeconds(2), TransactionStatus.FAILED),
                transaction(9, 4, "75.00", start, TransactionStatus.PENDING));

        FraudDetector detector = new FraudDetector();
        List<Transaction> highAmount = detector.findHighAmountTransactions(transactions);
        List<Transaction> rapid = detector.findRapidTransactions(transactions);
        List<Transaction> repeatedFailures = detector.findRepeatedFailureTransactions(transactions);
        printIds("High amount", highAmount);
        printIds("Rapid transactions", rapid);
        printIds("Repeated failures", repeatedFailures);

        Set<Long> flaggedIds = Stream.of(highAmount, rapid, repeatedFailures)
                .flatMap(List::stream)
                .map(Transaction::getId)
                .collect(Collectors.toSet());
        List<Long> unflaggedIds = transactions.stream()
                .filter(transaction -> !flaggedIds.contains(transaction.getId()))
                .map(Transaction::getId)
                .collect(Collectors.toList());
        System.out.println("Not flagged by any rule: " + unflaggedIds);
    }

    /** Creates a transaction with explicit time and status for predictable examples. */
    private static Transaction transaction(long id, long senderId, String amount,
                                          LocalDateTime timestamp, TransactionStatus status) {
        Transaction transaction = new Transaction(id, senderId, senderId + 100, new BigDecimal(amount));
        transaction.setTimestamp(timestamp);
        transaction.setStatus(status);
        return transaction;
    }

    /** Prints transaction IDs so the rule results are short and easy to compare. */
    private static void printIds(String ruleName, List<Transaction> transactions) {
        List<Long> ids = transactions.stream()
                .map(Transaction::getId)
                .collect(Collectors.toList());
        System.out.println(ruleName + " flagged transaction IDs: " + ids);
    }
}
