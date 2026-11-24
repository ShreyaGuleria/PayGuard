/**
 * Module: com.payguard.Main
 * Purpose: Runs the PayGuard console menu and connects the project packages.
 * Module lead (reviews and explains this module): Taani
 * Viva focus: Console flow and payment processing
 * Concepts used: OOP, Thread, JDBC, Streams, CSV, exception handling
 */
package com.payguard;

import com.payguard.dao.AccountDAO;
import com.payguard.dao.DBConnection;
import com.payguard.dao.TransactionDAO;
import com.payguard.exception.InsufficientBalanceException;
import com.payguard.exception.InvalidTransactionException;
import com.payguard.fraud.FraudDetector;
import com.payguard.io.CsvExporter;
import com.payguard.io.CsvImporter;
import com.payguard.model.Account;
import com.payguard.model.Transaction;
import com.payguard.model.TransactionStatus;
import com.payguard.queue.TransactionQueue;
import com.payguard.thread.Consumer;
import com.payguard.thread.Producer;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/** Provides the menu and coordinates account, transaction, and simulation work. */
public class Main {
    private final Scanner scanner = new Scanner(System.in);
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final Map<Long, Account> simulationAccounts = new HashMap<>();

    /** Initializes the database and keeps showing the menu until the user exits. */
    public static void main(String[] args) {
        Main application = new Main();
        try {
            DBConnection.initializeDatabase();
            application.runMenu();
        } catch (SQLException exception) {
            System.out.println("Database error: " + exception.getMessage());
        }
    }

    /** Reads menu choices and routes each choice to a small operation method. */
    private void runMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\nPayGuard");
            System.out.println("1. Add account");
            System.out.println("2. Run simulation");
            System.out.println("3. View accounts");
            System.out.println("4. View transactions");
            System.out.println("5. View flagged transactions");
            System.out.println("6. Export transactions to CSV");
            System.out.println("7. Import accounts from CSV");
            System.out.println("0. Exit");

            int choice = readInt("Choose an option: ");
            try {
                switch (choice) {
                    case 1 -> addAccount();
                    case 2 -> runSimulation();
                    case 3 -> viewAccounts();
                    case 4 -> viewTransactions(false);
                    case 5 -> viewTransactions(true);
                    case 6 -> exportTransactions();
                    case 7 -> importAccounts();
                    case 0 -> running = false;
                    default -> System.out.println("Please choose one of the listed options.");
                }
            } catch (SQLException exception) {
                System.out.println("Database error: " + exception.getMessage());
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            } catch (java.io.IOException exception) {
                System.out.println("File error: " + exception.getMessage());
            }
        }
        System.out.println("PayGuard closed.");
    }

    /** Reads and validates account details before inserting a new account. */
    private void addAccount() throws SQLException {
        System.out.print("Account holder name: ");
        String holderName = scanner.nextLine().trim();
        if (holderName.isEmpty()) {
            throw new IllegalArgumentException("Account holder name cannot be empty.");
        }

        BigDecimal balance = readAmount("Starting balance: ");
        if (balance.signum() < 0) {
            throw new IllegalArgumentException("Starting balance cannot be negative.");
        }
        long id = accountDAO.create(new Account(0, holderName, balance));
        System.out.println("Account created with ID " + id + ".");
    }

    /**
     * Generates transactions, applies fraud rules, then runs two producers and two consumers.
     * The consumers each receive a fixed share so they know when their simulation work ends.
     */
    private void runSimulation() throws SQLException {
        List<Account> accounts = accountDAO.findAll();
        if (accounts.size() < 2) {
            System.out.println("Add at least two accounts before running a simulation.");
            return;
        }

        int transactionCount = readInt("How many transactions should be generated? ");
        if (transactionCount < 0) {
            throw new IllegalArgumentException("Transaction count cannot be negative.");
        }

        List<Transaction> generated = createTransactions(accounts, transactionCount);
        setAccountsForProcessing(accounts);
        BigDecimal totalBefore = totalBalance(accounts);
        List<Transaction> historyAndNewTransactions = new ArrayList<>(transactionDAO.findAll());
        historyAndNewTransactions.addAll(generated);
        FraudDetector detector = new FraudDetector();
        detector.flagSuspiciousTransactions(historyAndNewTransactions);

        TransactionQueue queue = new TransactionQueue(10);
        List<Transaction> firstProducerTransactions = new ArrayList<>();
        List<Transaction> secondProducerTransactions = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            if (i % 2 == 0) {
                firstProducerTransactions.add(generated.get(i));
            } else {
                secondProducerTransactions.add(generated.get(i));
            }
        }
        Thread firstConsumer = new Thread(new Consumer(queue, this::processTransaction,
                (transactionCount + 1) / 2), "consumer-1");
        Thread secondConsumer = new Thread(new Consumer(queue, this::processTransaction,
                transactionCount / 2), "consumer-2");
        Thread firstProducer = new Thread(new Producer(queue, firstProducerTransactions), "producer-1");
        Thread secondProducer = new Thread(new Producer(queue, secondProducerTransactions), "producer-2");

        waitForWorkers(List.of(firstConsumer, secondConsumer, firstProducer, secondProducer));
        BigDecimal totalAfter = totalBalance(accounts);
        System.out.println("Total balance before simulation: " + totalBefore);
        System.out.println("Total balance after simulation:  " + totalAfter);
        System.out.println("Total money preserved: " + (totalBefore.compareTo(totalAfter) == 0));
        System.out.println("Simulation finished.");
    }

    /** Starts each worker and joins them so no simulation threads outlive the menu action. */
    private void waitForWorkers(List<Thread> workers) {
        for (Thread worker : workers) {
            worker.start();
        }

        boolean interrupted = false;
        for (Thread worker : workers) {
            while (worker.isAlive()) {
                try {
                    worker.join();
                } catch (InterruptedException exception) {
                    interrupted = true;
                    for (Thread activeWorker : workers) {
                        activeWorker.interrupt();
                    }
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            System.out.println("Simulation was interrupted; workers have stopped.");
        }
    }

    /** Adds account balances to compare total funds before and after a simulation. */
    private BigDecimal totalBalance(List<Account> accounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (Account account : accounts) {
            total = total.add(account.getBalance());
        }
        return total;
    }

    /** Creates random sender, receiver, and amount values for a simulation run. */
    private List<Transaction> createTransactions(List<Account> accounts, int count) {
        List<Transaction> transactions = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            int senderIndex = random.nextInt(accounts.size());
            int receiverIndex = random.nextInt(accounts.size() - 1);
            if (receiverIndex >= senderIndex) {
                receiverIndex++;
            }
            BigDecimal amount = BigDecimal.valueOf(random.nextInt(100, 2000001), 2);
            transactions.add(new Transaction(0,
                    accounts.get(senderIndex).getId(), accounts.get(receiverIndex).getId(), amount));
        }
        return transactions;
    }

    /** Keeps the same account objects available so all consumers lock shared balances. */
    void setAccountsForProcessing(List<Account> accounts) {
        simulationAccounts.clear();
        for (Account account : accounts) {
            simulationAccounts.put(account.getId(), account);
        }
    }

    /** Processes one queued payment while locking its two accounts in ID order. */
    void processTransaction(Transaction transaction) {
        Account sender = simulationAccounts.get(transaction.getSenderAccountId());
        Account receiver = simulationAccounts.get(transaction.getReceiverAccountId());

        if (transaction.getStatus() == TransactionStatus.FLAGGED) {
            saveFinalStatus(transaction, sender, receiver);
            System.out.println("Transaction flagged: sender " + transaction.getSenderAccountId());
            return;
        }

        if (sender == null || receiver == null) {
            transaction.setStatus(TransactionStatus.FAILED);
            System.out.println("Transaction failed: sender or receiver account was not found.");
            saveFinalStatus(transaction, sender, receiver);
            return;
        }

        Account firstLock = sender.getId() < receiver.getId() ? sender : receiver;
        Account secondLock = sender.getId() < receiver.getId() ? receiver : sender;
        // Lock lower ID first so opposite transfers acquire the same lock order and cannot deadlock.
        synchronized (firstLock) {
            synchronized (secondLock) {
                processWithAccountLocks(transaction, sender, receiver);
            }
        }
    }

    /** Checks funds, commits the payment atomically, and restores memory if the database fails. */
    private void processWithAccountLocks(Transaction transaction, Account sender, Account receiver) {
        BigDecimal oldSenderBalance = sender.getBalance();
        BigDecimal oldReceiverBalance = receiver.getBalance();
        try {
            validateTransaction(transaction);
            if (oldSenderBalance.compareTo(transaction.getAmount()) < 0) {
                throw new InsufficientBalanceException("Sender has insufficient balance.");
            }

            sender.setBalance(oldSenderBalance.subtract(transaction.getAmount()));
            receiver.setBalance(oldReceiverBalance.add(transaction.getAmount()));
            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionDAO.completePayment(transaction, sender, receiver);
        } catch (InvalidTransactionException | InsufficientBalanceException exception) {
            transaction.setStatus(TransactionStatus.FAILED);
            System.out.println("Transaction failed: " + exception.getMessage());
            saveFinalStatus(transaction, sender, receiver);
        } catch (SQLException exception) {
            sender.setBalance(oldSenderBalance);
            receiver.setBalance(oldReceiverBalance);
            transaction.setStatus(TransactionStatus.FAILED);
            System.out.println("Payment was rolled back: " + exception.getMessage());
        }
    }

    /** Rejects invalid amounts and self-transfers before changing account balances. */
    private void validateTransaction(Transaction transaction) throws InvalidTransactionException {
        if (transaction.getAmount() == null || transaction.getAmount().signum() <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero.");
        }
        if (transaction.getSenderAccountId() == transaction.getReceiverAccountId()) {
            throw new InvalidTransactionException("Sender and receiver must be different accounts.");
        }
    }

    /** Saves a failed or flagged transaction without changing account balances. */
    private void saveFinalStatus(Transaction transaction, Account sender, Account receiver) {
        try {
            transactionDAO.completePayment(transaction, sender, receiver);
        } catch (SQLException exception) {
            System.out.println("Could not save transaction: " + exception.getMessage());
        }
    }

    /** Prints account details in a simple table-like console format. */
    private void viewAccounts() throws SQLException {
        for (Account account : accountDAO.findAll()) {
            System.out.println(account.getId() + " | " + account.getHolderName()
                    + " | balance: " + account.getBalance());
        }
    }

    /** Prints all transactions or only those with FLAGGED status. */
    private void viewTransactions(boolean flaggedOnly) throws SQLException {
        List<Transaction> transactions = flaggedOnly
                ? transactionDAO.findFlagged() : transactionDAO.findAll();
        for (Transaction transaction : transactions) {
            System.out.println(transaction.getId() + " | " + transaction.getSenderAccountId()
                    + " -> " + transaction.getReceiverAccountId() + " | " + transaction.getAmount()
                    + " | " + transaction.getTimestamp() + " | " + transaction.getStatus());
        }
    }

    /** Asks for an output path and exports all stored transactions. */
    private void exportTransactions() throws SQLException, java.io.IOException {
        System.out.print("CSV output file path: ");
        Path outputFile = Path.of(scanner.nextLine().trim());
        new CsvExporter().exportTransactions(transactionDAO.findAll(), outputFile);
        System.out.println("Transactions exported.");
    }

    /** Asks for an account CSV path and imports validated rows through AccountDAO. */
    private void importAccounts() throws SQLException, java.io.IOException {
        System.out.print("Account CSV file path: ");
        Path inputFile = Path.of(scanner.nextLine().trim());
        new CsvImporter().importAccounts(inputFile, accountDAO);
    }

    /** Reads a whole-number menu value and asks again if the input is not numeric. */
    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                System.out.println("Enter a whole number.");
            }
        }
    }

    /** Reads a decimal amount and asks again when the text is not a valid number. */
    private BigDecimal readAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return new BigDecimal(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                System.out.println("Enter a valid decimal amount.");
            }
        }
    }
}
