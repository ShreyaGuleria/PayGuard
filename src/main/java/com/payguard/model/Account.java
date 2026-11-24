/**
 * Module: com.payguard.model.Account
 * Purpose: Represents a user's payment account and its current balance.
 * Module lead (reviews and explains this module): Taani
 * Viva focus: OOP, encapsulation, and account balances
 * Concepts used: OOP, encapsulation
 */
package com.payguard.model;

import java.math.BigDecimal;

/** Stores the account details that other PayGuard packages use. */
public class Account {
    private long id;
    private String holderName;
    private BigDecimal balance;

    /** Creates an account with an ID, owner name, and starting balance. */
    public Account(long id, String holderName, BigDecimal balance) {
        this.id = id;
        this.holderName = holderName;
        this.balance = balance;
    }

    /** Returns the database ID so DAOs can identify this account. */
    public long getId() {
        return id;
    }

    /** Changes the ID when a database assigns one during insertion. */
    public void setId(long id) {
        this.id = id;
    }

    /** Returns the account holder's display name. */
    public String getHolderName() {
        return holderName;
    }

    /** Updates the account holder's display name. */
    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    /** Returns the current balance for payment and report calculations. */
    public BigDecimal getBalance() {
        return balance;
    }

    /** Updates the balance after a payment is processed. */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
