/**
 * Module: com.payguard.model.Transaction
 * Purpose: Represents a payment moving between two accounts.
 * Module lead (reviews and explains this module): Taani
 * Viva focus: Transaction data, timestamps, and status
 * Concepts used: OOP, encapsulation, java.time
 */
package com.payguard.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Holds transaction data shared by the queue, processing, and database code. */
public class Transaction {
    private long id;
    private long senderAccountId;
    private long receiverAccountId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private TransactionStatus status;

    /** Creates a pending transaction and records when it was created. */
    public Transaction(long id, long senderAccountId, long receiverAccountId, BigDecimal amount) {
        this.id = id;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
    }

    /** Returns the transaction ID used by storage and reports. */
    public long getId() {
        return id;
    }

    /** Sets the ID when the database assigns it during insertion. */
    public void setId(long id) {
        this.id = id;
    }

    /** Returns the account that sends the payment. */
    public long getSenderAccountId() {
        return senderAccountId;
    }

    /** Changes the sender account ID. */
    public void setSenderAccountId(long senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    /** Returns the account that receives the payment. */
    public long getReceiverAccountId() {
        return receiverAccountId;
    }

    /** Changes the receiver account ID. */
    public void setReceiverAccountId(long receiverAccountId) {
        this.receiverAccountId = receiverAccountId;
    }

    /** Returns the payment amount using decimal arithmetic for currency. */
    public BigDecimal getAmount() {
        return amount;
    }

    /** Changes the payment amount. */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /** Returns the creation time used by reports and fraud rules. */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /** Sets the time, for example when reconstructing a row from the database. */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /** Returns the current processing state. */
    public TransactionStatus getStatus() {
        return status;
    }

    /** Updates the state after processing or fraud checks. */
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}
