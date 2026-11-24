/**
 * Module: com.payguard.exception.AccountNotFoundException
 * Purpose: Reports that a requested account ID does not exist.
 * Module lead (reviews and explains this module): Taani
 * Viva focus: Checked exceptions and account lookup errors
 * Concepts used: OOP, exception handling
 */
package com.payguard.exception;

/** Signals that account lookup could not find the requested account. */
public class AccountNotFoundException extends Exception {
    /** Creates a checked exception with the missing account details. */
    public AccountNotFoundException(String message) {
        super(message);
    }
}
