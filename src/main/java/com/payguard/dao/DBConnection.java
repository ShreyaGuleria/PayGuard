/**
 * Module: com.payguard.dao.DBConnection
 * Purpose: Opens SQLite connections and creates PayGuard's database tables.
 * Module lead (reviews and explains this module): Hitakshi
 * Viva focus: SQLite connections and schema setup
 * Concepts used: JDBC, SQLite, PreparedStatement
 */
package com.payguard.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Centralizes database setup so all DAOs use the same SQLite file. */
public class DBConnection {
    private static final String DATABASE_URL = "jdbc:sqlite:payguard.db";

    /** Opens a connection to the local PayGuard database file. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    /** Creates the tables when the application starts, leaving existing data intact. */
    public static void initializeDatabase() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement accountTable = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS accounts ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "holder_name TEXT NOT NULL, "
                            + "balance TEXT NOT NULL)")) {
                accountTable.executeUpdate();
            }

            try (PreparedStatement transactionTable = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS transactions ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "sender_account_id INTEGER NOT NULL, "
                            + "receiver_account_id INTEGER NOT NULL, "
                            + "amount TEXT NOT NULL, "
                            + "timestamp TEXT NOT NULL, "
                            + "status TEXT NOT NULL, "
                            + "FOREIGN KEY(sender_account_id) REFERENCES accounts(id), "
                            + "FOREIGN KEY(receiver_account_id) REFERENCES accounts(id))")) {
                transactionTable.executeUpdate();
            }
        }
    }
}
