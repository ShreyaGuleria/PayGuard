/**
 * Module: com.payguard.ConcurrencyCheck
 * Purpose: Demonstrates that concurrent consumers cannot overspend one account.
 * Module lead (reviews and explains this module): Savleen
 * Viva focus: Concurrent consumers and safe balances
 * Concepts used: Thread, Runnable, synchronized, JDBC, wait/notify
 */
package com.payguard;

import com.payguard.dao.AccountDAO;
import com.payguard.dao.DBConnection;
import com.payguard.model.Account;
import com.payguard.model.Transaction;
import com.payguard.queue.TransactionQueue;
import com.payguard.thread.Consumer;
import com.payguard.thread.Producer;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** Runs a repeatable two-payment scenario without requiring a test framework. */
public class ConcurrencyCheck {

    /** Creates two fresh accounts, runs two consumers, and prints final results. */
    public static void main(String[] args) throws SQLException, InterruptedException {
        DBConnection.initializeDatabase();
        AccountDAO accountDAO = new AccountDAO();
        Account sender = new Account(0, "Concurrency Check Sender", new BigDecimal("1000.00"));
        Account receiver = new Account(0, "Concurrency Check Receiver", new BigDecimal("0.00"));
        accountDAO.create(sender);
        accountDAO.create(receiver);

        Transaction firstPayment = new Transaction(0, sender.getId(), receiver.getId(),
                new BigDecimal("800.00"));
        Transaction secondPayment = new Transaction(0, sender.getId(), receiver.getId(),
                new BigDecimal("800.00"));
        List<Transaction> payments = List.of(firstPayment, secondPayment);

        Main application = new Main();
        application.setAccountsForProcessing(List.of(sender, receiver));
        TransactionQueue queue = new TransactionQueue(2);
        Thread firstConsumer = new Thread(new Consumer(queue, application::processTransaction, 1));
        Thread secondConsumer = new Thread(new Consumer(queue, application::processTransaction, 1));
        Thread producer = new Thread(new Producer(queue, payments));

        firstConsumer.start();
        secondConsumer.start();
        producer.start();
        producer.join();
        firstConsumer.join();
        secondConsumer.join();

        System.out.println("Sender balance: " + sender.getBalance());
        System.out.println("Receiver balance: " + receiver.getBalance());
        System.out.println("Transaction 1: " + firstPayment.getStatus());
        System.out.println("Transaction 2: " + secondPayment.getStatus());
    }
}
