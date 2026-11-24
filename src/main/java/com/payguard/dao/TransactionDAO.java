/**
 * Module: com.payguard.dao.TransactionDAO
 * Purpose: Saves, finds, updates, and deletes transaction records with JDBC.
 * Module lead (reviews and explains this module): Hitakshi
 * Viva focus: JDBC transactions and rollback
 * Concepts used: JDBC, CRUD, PreparedStatement, java.time
 */
package com.payguard.dao;

import com.payguard.model.Transaction;
import com.payguard.model.TransactionStatus;
import com.payguard.model.Account;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Performs transaction CRUD operations for the rest of the application. */
public class TransactionDAO {

    /**
     * Saves the final payment result and, for successful payments, both balances in one transaction.
     * A failure in either balance update or the transaction insert rolls back all database changes.
     */
    public void completePayment(Transaction transaction, Account sender, Account receiver) throws SQLException {
        String updateBalanceSql = "UPDATE accounts SET balance = ? WHERE id = ?";
        String insertTransactionSql = "INSERT INTO transactions "
                + "(sender_account_id, receiver_account_id, amount, timestamp, status) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection()) {
            try {
                connection.setAutoCommit(false);

                if (transaction.getStatus() == TransactionStatus.SUCCESS) {
                    try (PreparedStatement debit = connection.prepareStatement(updateBalanceSql)) {
                        debit.setString(1, sender.getBalance().toPlainString());
                        debit.setLong(2, sender.getId());
                        if (debit.executeUpdate() != 1) {
                            throw new SQLException("Sender account balance was not updated.");
                        }
                    }
                    try (PreparedStatement credit = connection.prepareStatement(updateBalanceSql)) {
                        credit.setString(1, receiver.getBalance().toPlainString());
                        credit.setLong(2, receiver.getId());
                        if (credit.executeUpdate() != 1) {
                            throw new SQLException("Receiver account balance was not updated.");
                        }
                    }
                }

                try (PreparedStatement insert = connection.prepareStatement(insertTransactionSql)) {
                    insert.setLong(1, transaction.getSenderAccountId());
                    insert.setLong(2, transaction.getReceiverAccountId());
                    insert.setString(3, transaction.getAmount().toPlainString());
                    insert.setString(4, transaction.getTimestamp().toString());
                    insert.setString(5, transaction.getStatus().name());
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /** Inserts a transaction and returns its generated database ID. */
    public long create(Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions "
                + "(sender_account_id, receiver_account_id, amount, timestamp, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transaction.getSenderAccountId());
            statement.setLong(2, transaction.getReceiverAccountId());
            statement.setString(3, transaction.getAmount().toPlainString());
            statement.setString(4, transaction.getTimestamp().toString());
            statement.setString(5, transaction.getStatus().name());
            statement.executeUpdate();
            // SQLite keeps this value per connection, so read it before closing this connection.
            try (PreparedStatement idStatement = connection.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet results = idStatement.executeQuery()) {
                long id = results.getLong(1);
                transaction.setId(id);
                return id;
            }
        }
    }

    /** Finds a transaction by ID and returns null when it does not exist. */
    public Transaction findById(long id) throws SQLException {
        String sql = "SELECT id, sender_account_id, receiver_account_id, amount, timestamp, status "
                + "FROM transactions WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return readTransaction(results);
                }
            }
        }
        return null;
    }

    /** Returns every transaction in ID order for display and reporting. */
    public List<Transaction> findAll() throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, sender_account_id, receiver_account_id, amount, timestamp, status "
                + "FROM transactions ORDER BY id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                transactions.add(readTransaction(results));
            }
        }
        return transactions;
    }

    /** Returns transactions marked FLAGGED for the flagged-transactions menu. */
    public List<Transaction> findFlagged() throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, sender_account_id, receiver_account_id, amount, timestamp, status "
                + "FROM transactions WHERE status = ? ORDER BY id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TransactionStatus.FLAGGED.name());
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    transactions.add(readTransaction(results));
                }
            }
        }
        return transactions;
    }

    /** Updates all transaction fields and reports whether the ID existed. */
    public boolean update(Transaction transaction) throws SQLException {
        String sql = "UPDATE transactions SET sender_account_id = ?, receiver_account_id = ?, "
                + "amount = ?, timestamp = ?, status = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transaction.getSenderAccountId());
            statement.setLong(2, transaction.getReceiverAccountId());
            statement.setString(3, transaction.getAmount().toPlainString());
            statement.setString(4, transaction.getTimestamp().toString());
            statement.setString(5, transaction.getStatus().name());
            statement.setLong(6, transaction.getId());
            return statement.executeUpdate() > 0;
        }
    }

    /** Deletes a transaction by ID and reports whether a row was removed. */
    public boolean delete(long id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    /** Converts the current result row back into a Transaction model. */
    private Transaction readTransaction(ResultSet results) throws SQLException {
        Transaction transaction = new Transaction(
                results.getLong("id"),
                results.getLong("sender_account_id"),
                results.getLong("receiver_account_id"),
                new BigDecimal(results.getString("amount")));
        transaction.setTimestamp(LocalDateTime.parse(results.getString("timestamp")));
        transaction.setStatus(TransactionStatus.valueOf(results.getString("status")));
        return transaction;
    }
}
