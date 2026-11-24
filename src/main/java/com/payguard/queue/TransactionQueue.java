/**
 * Module: com.payguard.queue.TransactionQueue
 * Purpose: Shares transactions safely between producer and consumer threads.
 * Module lead (reviews and explains this module): Savleen
 * Viva focus: wait/notify and bounded buffer
 * Concepts used: OOP, synchronized, wait/notify, bounded queue
 */
package com.payguard.queue;

import com.payguard.model.Transaction;
import java.util.LinkedList;
import java.util.Queue;

/** Keeps a fixed number of transactions and coordinates waiting threads. */
public class TransactionQueue {
    private final Queue<Transaction> transactions = new LinkedList<>();
    private final int capacity;

    /** Sets the maximum queue size so producers cannot use unlimited memory. */
    public TransactionQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be greater than zero.");
        }
        this.capacity = capacity;
    }

    /**
     * Adds a transaction, waiting while the queue is full.
     * The method lock prevents two producers from checking the same free slot
     * and both adding, which would let the queue exceed its capacity.
     */
    public synchronized void put(Transaction transaction) throws InterruptedException {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null.");
        }

        while (transactions.size() == capacity) {
            wait();
        }

        transactions.add(transaction);
        notifyAll();
    }

    /**
     * Removes and returns the next transaction, waiting while the queue is empty.
     * The method lock prevents two consumers from removing the same item or
     * changing the linked list at the same time.
     */
    public synchronized Transaction take() throws InterruptedException {
        while (transactions.isEmpty()) {
            wait();
        }

        Transaction transaction = transactions.remove();
        notifyAll();
        return transaction;
    }

    /** Returns the current item count for display and simulation status. */
    public synchronized int size() {
        return transactions.size();
    }

    /** Returns the fixed limit so the caller can describe queue capacity. */
    public int getCapacity() {
        return capacity;
    }
}
