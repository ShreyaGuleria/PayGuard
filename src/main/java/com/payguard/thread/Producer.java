/**
 * Module: com.payguard.thread.Producer
 * Purpose: Places a prepared set of transactions into the shared queue.
 * Module lead (reviews and explains this module): Savleen
 * Viva focus: Runnable and transaction production
 * Concepts used: OOP, Runnable, Thread, wait/notify
 */
package com.payguard.thread;

import com.payguard.model.Transaction;
import com.payguard.queue.TransactionQueue;
import java.util.List;

/** Runs on a producer thread and hands transactions to consumers. */
public class Producer implements Runnable {
    private final TransactionQueue queue;
    private final List<Transaction> transactions;

    /** Receives the shared queue and transactions this producer should submit. */
    public Producer(TransactionQueue queue, List<Transaction> transactions) {
        this.queue = queue;
        this.transactions = transactions;
    }

    /** Adds each prepared transaction; interruption stops production cleanly. */
    @Override
    public void run() {
        try {
            for (Transaction transaction : transactions) {
                queue.put(transaction);
            }
        } catch (InterruptedException exception) {
            // Restore the interrupt flag so the thread that started us can detect the stop.
            Thread.currentThread().interrupt();
        }
    }
}
