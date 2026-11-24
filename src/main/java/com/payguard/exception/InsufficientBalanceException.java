/**
 * Module: com.payguard.exception.InsufficientBalanceException
 * Purpose: Reports that an account cannot cover a requested payment.
 * Module lead (reviews and explains this module): Taani
 * Viva focus: Checked exceptions and insufficient funds
 * Concepts used: OOP, exception handling
 */
package com.payguard.exception;

/** Signals that a payment would make the sender's balance negative. */
public class InsufficientBalanceException extends Exception {
    /** Creates a checked exception with a message the caller can show or log. */
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
