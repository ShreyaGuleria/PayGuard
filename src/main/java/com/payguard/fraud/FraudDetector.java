/**
 * Module: com.payguard.fraud.FraudDetector
 * Purpose: Flags transactions that match configured suspicious-activity rules.
 * Module lead (reviews and explains this module): Shreya
 * Viva focus: Stream pipelines and named thresholds
 * Concepts used: OOP, Streams, lambdas, java.time
 */
package com.payguard.fraud;

import com.payguard.model.Transaction;
import com.payguard.model.TransactionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Checks transaction amounts, sender frequency, and repeated failed payments. */
public class FraudDetector {
    public static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
    public static final int MAX_TXNS_IN_WINDOW = 3;
    public static final int WINDOW_SECONDS = 60;
    public static final int MAX_FAILED_ATTEMPTS = 3;

    /**
     * Returns transactions that exceed the amount limit.
     * This filter keeps the high-amount rule separate so it is easy to explain and display.
     */
    public List<Transaction> findHighAmountTransactions(List<Transaction> transactions) {
        // Keep only transactions whose amount is greater than the named threshold.
        return transactions.stream()
                .filter(transaction -> transaction.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0)
                .collect(Collectors.toList());
    }

    /**
     * Returns transactions that exceed the sender's count limit in a rolling time window.
     * The trailing window ends at each candidate, so old transactions outside it do not count.
     */
    public List<Transaction> findRapidTransactions(List<Transaction> transactions) {
        // Group first so each candidate is compared only with transactions from its sender.
        Map<Long, List<Transaction>> transactionsBySender = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getSenderAccountId));

        // Keep candidates with more than MAX_TXNS_IN_WINDOW from that sender in their trailing window.
        return transactionsBySender.entrySet().stream()
                .flatMap(senderGroup -> senderGroup.getValue().stream()
                        .filter(candidate -> countSenderTransactionsInWindow(
                                senderGroup.getKey(), senderGroup.getValue(), candidate)
                                > MAX_TXNS_IN_WINDOW))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns failed transactions from senders who reached the failure limit.
     * Counting only FAILED rows avoids treating successful payments as failed attempts.
     */
    public List<Transaction> findRepeatedFailureTransactions(List<Transaction> transactions) {
        // Group failed rows by sender and count them before choosing senders over the limit.
        Set<Long> sendersWithRepeatedFailures = transactions.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.FAILED)
                .collect(Collectors.groupingBy(Transaction::getSenderAccountId, Collectors.counting()))
                .entrySet().stream()
                .filter(senderCount -> senderCount.getValue() >= MAX_FAILED_ATTEMPTS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // Return only failed rows for those senders so the flagged rows show the repeated attempts.
        return transactions.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.FAILED)
                .filter(transaction -> sendersWithRepeatedFailures.contains(
                        transaction.getSenderAccountId()))
                .collect(Collectors.toList());
    }

    /** Marks and returns each transaction that matches one or more of the three rules. */
    public List<Transaction> flagSuspiciousTransactions(List<Transaction> transactions) {
        // Combine the three rule results and remove duplicates before changing each status.
        List<Transaction> suspicious = Stream.of(
                        findHighAmountTransactions(transactions),
                        findRapidTransactions(transactions),
                        findRepeatedFailureTransactions(transactions))
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
        suspicious.forEach(transaction -> transaction.setStatus(TransactionStatus.FLAGGED));
        return suspicious;
    }

    /** Counts only the candidate sender's transactions in the 60-second trailing window. */
    private long countSenderTransactionsInWindow(long senderId, List<Transaction> senderTransactions,
                                                  Transaction candidate) {
        // Filter by time first, then group and count the remaining rows by sender.
        Map<Long, Long> countsBySender = senderTransactions.stream()
                .filter(transaction -> !transaction.getTimestamp().isBefore(
                        candidate.getTimestamp().minusSeconds(WINDOW_SECONDS)))
                .filter(transaction -> !transaction.getTimestamp().isAfter(candidate.getTimestamp()))
                .collect(Collectors.groupingBy(
                        Transaction::getSenderAccountId, Collectors.counting()));
        return countsBySender.getOrDefault(senderId, 0L);
    }
}
