/**
 * Module: com.payguard.exception.InvalidTransactionException
 * Purpose: Reports transaction data that cannot be processed safely.
 * Module lead (reviews and explains this module): Taani
 * Viva focus: Checked exceptions and invalid payment data
 * Concepts used: OOP, exception handling
 */
package com.payguard.exception;

/** Signals that a transaction has invalid values such as a non-positive amount. */
public class InvalidTransactionException extends Exception {
    /** Creates a checked exception with a message describing the invalid data. */
    public InvalidTransactionException(String message) {
        super(message);
    }
}
