/**
 * Module: com.payguard.model.TransactionStatus
 * Purpose: Lists the possible processing states of a payment transaction.
 * Module lead (reviews and explains this module): Taani
 * Viva focus: Enums and fixed transaction states
 * Concepts used: OOP, enum
 */
package com.payguard.model;

/** Gives transactions a fixed set of states instead of free-form text. */
public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    FLAGGED
}
