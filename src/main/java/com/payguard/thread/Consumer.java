/**
 * Module: com.payguard.thread.Consumer
 * Purpose: Removes transactions from the shared queue and sends them for processing.
 * Module lead (reviews and explains this module): Savleen
 * Viva focus: Runnable and transaction consumption
 * Concepts used: OOP, Runnable, Thread, wait/notify
 */
package com.payguard.thread;

import com.payguard.model.Transaction;
import com.payguard.queue.TransactionQueue;

/** Runs on a consumer thread and delegates each removed transaction. */
public class Consumer implements Runnable {
    private final TransactionQueue queue;
    private final TransactionHandler handler;
    private final int transactionCount;

    /**
     * Sets how many items to consume and what operation processes each item.
     * A handler keeps queue coordination separate from payment and database logic.
     */
    public Consumer(TransactionQueue queue, TransactionHandler handler, int transactionCount) {
        if (transactionCount < 0) {
            throw new IllegalArgumentException("Transaction count cannot be negative.");
        }
        this.queue = queue;
        this.handler = handler;
        this.transactionCount = transactionCount;
    }

    /** Takes the requested number of items and passes them to the handler. */
    @Override
    public void run() {
        try {
            for (int i = 0; i < transactionCount; i++) {
                Transaction transaction = queue.take();
                handler.process(transaction);
            }
        } catch (InterruptedException exception) {
            // Restore the interrupt flag so the thread that started us can detect the stop.
            Thread.currentThread().interrupt();
        }
    }

    /** Describes the work a consumer performs after taking a transaction. */
    public interface TransactionHandler {
        /** Processes one transaction, such as checking a balance and saving its result. */
        void process(Transaction transaction);
    }
}
