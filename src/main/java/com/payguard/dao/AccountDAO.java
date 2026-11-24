/**
 * Module: com.payguard.dao.AccountDAO
 * Purpose: Saves, finds, updates, and deletes account records with JDBC.
 * Module lead (reviews and explains this module): Hitakshi
 * Viva focus: JDBC CRUD for accounts
 * Concepts used: JDBC, CRUD, PreparedStatement
 */
package com.payguard.dao;

import com.payguard.model.Account;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Performs account CRUD operations without putting SQL in the menu code. */
public class AccountDAO {

    /** Inserts an account and returns its generated database ID. */
    public long create(Account account) throws SQLException {
        String sql = "INSERT INTO accounts (holder_name, balance) VALUES (?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getHolderName());
            statement.setString(2, account.getBalance().toPlainString());
            statement.executeUpdate();
            // SQLite keeps this value per connection, so read it before closing this connection.
            try (PreparedStatement idStatement = connection.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet results = idStatement.executeQuery()) {
                long id = results.getLong(1);
                account.setId(id);
                return id;
            }
        }
    }

    /** Finds one account by ID and returns null when no matching row exists. */
    public Account findById(long id) throws SQLException {
        String sql = "SELECT id, holder_name, balance FROM accounts WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return readAccount(results);
                }
            }
        }
        return null;
    }

    /** Returns all accounts in ID order for display and CSV export. */
    public List<Account> findAll() throws SQLException {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT id, holder_name, balance FROM accounts ORDER BY id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                accounts.add(readAccount(results));
            }
        }
        return accounts;
    }

    /** Updates the name and balance for the account ID and reports if it existed. */
    public boolean update(Account account) throws SQLException {
        String sql = "UPDATE accounts SET holder_name = ?, balance = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getHolderName());
            statement.setString(2, account.getBalance().toPlainString());
            statement.setLong(3, account.getId());
            return statement.executeUpdate() > 0;
        }
    }

    /** Deletes an account by ID and reports whether a row was removed. */
    public boolean delete(long id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM accounts WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    /** Converts the current result row into the Account model. */
    private Account readAccount(ResultSet results) throws SQLException {
        return new Account(
                results.getLong("id"),
                results.getString("holder_name"),
                new BigDecimal(results.getString("balance")));
    }
}
