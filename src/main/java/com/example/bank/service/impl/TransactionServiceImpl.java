 package com.example.bank.service.impl;

import com.example.bank.db.DBConnection;
import com.example.bank.model.Transaction;
import com.example.bank.service.TransactionService;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.service.alert.NotificationService; 
import com.example.bank.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class TransactionServiceImpl implements TransactionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionServiceImpl.class);
    
    private final NotificationService notificationService = new NotificationService(); 
    
    private void checkMpin(Connection conn, String accountNumber, String mpin) throws SQLException {
        if (mpin == null) {
            LOGGER.warn("MPIN missing for transaction on account {}", accountNumber);
            throw new SQLException("MPIN required for this transaction.");
        }
        
        String sql = "SELECT c.mpin FROM customer_details c JOIN account_details a ON c.customer_id = a.customer_id WHERE a.account_number = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedMpin = rs.getString("mpin");
                if (!mpin.equals(storedMpin)) {
                    LOGGER.warn("Invalid MPIN provided for account {}", accountNumber);
                    throw new SQLException("Invalid MPIN provided.");
                }
                LOGGER.info("MPIN verified successfully for account {}.", accountNumber);
            } else {
                throw new SQLException("Account owner not found for MPIN check.");
            }
        }
    }
    
    private int getAccountIdByNumber(Connection conn, String accountNumber) throws SQLException {
        String sql = "SELECT account_id FROM account_details WHERE account_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("account_id");
            }
            LOGGER.warn("Account ID lookup failed for number: {}", accountNumber);
            throw new SQLException("Account not found for number: " + accountNumber);
        }
    }
    
    @Override
    public Transaction deposit(Transaction tx) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            
         
            tx.setAccountId(getAccountIdByNumber(conn, tx.getAccountNumber()));
            // Update balance
            String updateBalance = "UPDATE account_details SET balance = balance + ? WHERE account_id=?";
            try (PreparedStatement ps = conn.prepareStatement(updateBalance)) {
                ps.setDouble(1, tx.getTransactionAmount());
                ps.setInt(2, tx.getAccountId());
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    throw new SQLException("Account not found");
                }
            }

            // Insert transaction
            String insertTx = "INSERT INTO transaction_details " +
                    "(utr_number, date_of_transaction, transaction_amount, debited_date, account_id, balance_amount, description, modified_by, receiver_by, transaction_type, mode_of_transaction) " +
                    "VALUES (?, ?, ?, ?, ?, (SELECT balance FROM account_details WHERE account_id=?), ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(insertTx, Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime now = LocalDateTime.now();
                
                String utr = generateUTR();
                tx.setUtrNumber(utr);
                ps.setString(1, utr);
                ps.setTimestamp(2, Timestamp.valueOf(now)); 
                ps.setDouble(3, tx.getTransactionAmount());
                ps.setTimestamp(4, Timestamp.valueOf(now)); 
                ps.setInt(5, tx.getAccountId());
                ps.setInt(6, tx.getAccountId()); 
                ps.setString(7, tx.getDescription());
                String modifiedBy = tx.getModifiedBy() != null ? tx.getModifiedBy() : "System";
                String receiverBy = tx.getReceiverBy() != null ? tx.getReceiverBy() : "Bank";

                ps.setString(8, modifiedBy);   
                ps.setString(9, receiverBy);   
              
                ps.setString(10, "deposit");
                ps.setString(11, tx.getModeOfTransaction());

                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) tx.setTransactionId(rs.getInt(1));
            }

            //  Set details back to response
            tx.setDateOfTransaction(LocalDateTime.now());
            tx.setDebitedDate(LocalDateTime.now());
            tx.setTransactionType("deposit");
            
         //  Explicitly set the fields that might have been null in the request or set by default logic.
            if (tx.getModifiedBy() == null) {
                tx.setModifiedBy("System");
            }
            if (tx.getReceiverBy() == null) {
                tx.setReceiverBy("Bank");
            }
            
            String getBal = "SELECT balance FROM account_details WHERE account_id=?";
            try (PreparedStatement ps = conn.prepareStatement(getBal)) {
                ps.setInt(1, tx.getAccountId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) tx.setBalanceAmount(rs.getDouble(1));
            }

            conn.commit();
            LOGGER.info("Deposit successful for Account {}. Amount: {}", tx.getAccountNumber(), tx.getTransactionAmount()); 
                        
         //  NOTIFICATION TRIGGER for CREATING FUNDS
            try (Connection connEmail = DBConnection.getConnection()) { // Use a new connection for lookup
                Customer customer = getCustomerByAccountId(connEmail, tx.getAccountId());
                notificationService.sendCreditNotification(customer, tx);
            } catch (SQLException e) {
            	LOGGER.error("Email skipped: Could not fetch customer details for account {}.", tx.getAccountNumber(), e); 
            } 
            
            return tx;
        }
    }
    

    @Override
    public Transaction withdraw(Transaction tx) throws SQLException, InsufficientFundsException {
    	 Connection conn = null;
        try{
        	 conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            checkMpin(conn, tx.getAccountNumber(), tx.getMpin());
            
            tx.setAccountId(getAccountIdByNumber(conn, tx.getAccountNumber()));
            //  Check balance
            double currentBalance = 0;
            String getBalance = "SELECT balance FROM account_details WHERE account_id=?";
            try (PreparedStatement ps = conn.prepareStatement(getBalance)) {
                ps.setInt(1, tx.getAccountId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) currentBalance = rs.getDouble(1);
                else throw new SQLException("Account not found");
            }
            
            if (currentBalance < tx.getTransactionAmount()) {
                throw new InsufficientFundsException("Insufficient balance in account number: " + tx.getAccountNumber());
            }
            
            //  Deduct balance
            String updateBalance = "UPDATE account_details SET balance = balance - ? WHERE account_id=?";
            try (PreparedStatement ps = conn.prepareStatement(updateBalance)) {
                ps.setDouble(1, tx.getTransactionAmount());
                ps.setInt(2, tx.getAccountId());
                ps.executeUpdate();
            }

            // Insert transaction
            String insertTx = "INSERT INTO transaction_details " +
                    "(utr_number, date_of_transaction, transaction_amount, debited_date, account_id, balance_amount, description, modified_by, receiver_by, transaction_type, mode_of_transaction) " +
                    "VALUES (?, ?, ?, ?, ?, (SELECT balance FROM account_details WHERE account_id=?), ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(insertTx, Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime now = LocalDateTime.now();

                String utr = generateUTR();
                tx.setUtrNumber(utr);
                
             // Determine final values for modifiedBy and receiverBy for DB
                ps.setString(1, utr);
                ps.setTimestamp(2, Timestamp.valueOf(now));
                ps.setDouble(3, tx.getTransactionAmount());
                ps.setTimestamp(4, Timestamp.valueOf(now));
                ps.setInt(5, tx.getAccountId());
                ps.setInt(6, tx.getAccountId());
                ps.setString(7, tx.getDescription());
                ps.setString(8, tx.getModifiedBy() != null ? tx.getModifiedBy() : "System");
                ps.setString(9, tx.getReceiverBy() != null ? tx.getReceiverBy() : "ATM");
                ps.setString(10, "withdraw");
                ps.setString(11, tx.getModeOfTransaction());

                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) tx.setTransactionId(rs.getInt(1));
            }

            // Update response object
            tx.setDateOfTransaction(LocalDateTime.now());
            tx.setDebitedDate(LocalDateTime.now());
            tx.setTransactionType("withdraw");
            
         //  Explicitly set the fields on the returned object if they were null
            if (tx.getModifiedBy() == null) {
                tx.setModifiedBy("System");
            }
            if (tx.getReceiverBy() == null) {
                tx.setReceiverBy("ATM"); 
            }
            

            String getBal = "SELECT balance FROM account_details WHERE account_id=?";
            try (PreparedStatement ps = conn.prepareStatement(getBal)) {
                ps.setInt(1, tx.getAccountId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) tx.setBalanceAmount(rs.getDouble(1));
            }

            conn.commit();
            LOGGER.info("Withdrawal successful for Account {}. Amount: {}", tx.getAccountNumber(), tx.getTransactionAmount()); 
            
            
         // NOTIFICATION TRIGGER for DEBITED FUNDS
            try (Connection connEmail = DBConnection.getConnection()) {
                Customer customer = getCustomerByAccountId(connEmail, tx.getAccountId());
                notificationService.sendDebitNotification(customer, tx);
            } catch (SQLException e) {
            	 LOGGER.error("Email skipped: Could not fetch customer details for account {}.", tx.getAccountNumber(), e); 
            } 
            
            return tx;
        }
        catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    LOGGER.error("Transaction rolled back for withdrawal attempt on Account {}.", tx.getAccountNumber(), e); 
                } catch (SQLException rollbackEx) {
                    LOGGER.error("Rollback failed.", rollbackEx); 
                }
                
            }
            if (e instanceof InsufficientFundsException) {
                throw (InsufficientFundsException) e;
            }
            throw (SQLException) e; 
            
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("Connection close failed: " + closeEx.getMessage());
                }
            }
        }
        
    }

    
    @Override
    public List<Transaction> getTransactionsByAccountNumber(String accountNumber) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        
 
        String sql = "SELECT t.*, a.account_number FROM transaction_details t " +
                     "JOIN account_details a ON a.account_id = t.account_id " +
                     "WHERE a.account_number=?";
      
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Transaction tx = new Transaction();
                tx.setTransactionId(rs.getInt("transaction_id"));
                tx.setUtrNumber(rs.getString("utr_number"));

                Timestamp ts = rs.getTimestamp("date_of_transaction");
                if (ts != null) {
                    tx.setDateOfTransaction(ts.toLocalDateTime());
                }

                Timestamp ds = rs.getTimestamp("debited_date");
                if (ds != null) {
                    tx.setDebitedDate(ds.toLocalDateTime());
                }

                tx.setTransactionAmount(rs.getDouble("transaction_amount"));
                tx.setAccountId(rs.getInt("account_id"));
                tx.setBalanceAmount(rs.getDouble("balance_amount"));
                tx.setDescription(rs.getString("description"));
                tx.setTransactionType(rs.getString("transaction_type"));
                tx.setModeOfTransaction(rs.getString("mode_of_transaction"));
                tx.setAccountNumber(rs.getString("account_number"));
                tx.setModifiedBy(rs.getString("modified_by"));
                tx.setReceiverBy(rs.getString("receiver_by"));
                list.add(tx);
            }
        }
        return list;
    }
    //  transferFunds
    @Override
    public void transferFunds(String senderAccountNumber, String receiverAccountNumber, 
                              double amount, String mode, String description, String mpin) 
            throws SQLException, InsufficientFundsException { // 💡 New Signature
        
       
            Connection conn = null;
            try {
                conn = DBConnection.getConnection();
                conn.setAutoCommit(false);
                checkMpin(conn, senderAccountNumber, mpin);

            int senderId = getAccountIdByNumber(conn, senderAccountNumber);
            int receiverId = getAccountIdByNumber(conn, receiverAccountNumber);

            // 2. Check sender balance
            double senderBalance = getBalance(conn, senderId);
            if (senderBalance < amount) {
                throw new InsufficientFundsException("Insufficient balance in sender account number: " + senderAccountNumber);
            }

            // 3. Deduct from sender
            updateBalance(conn, senderId, -amount);

            // 4. Credit to receiver
            updateBalance(conn, receiverId, amount);

            // 5. Insert debit transaction (sender)
            String debitUtr= insertTransaction(conn, generateUTR(),senderAccountNumber, senderId, amount, "withdraw", mode, description, "Transfer to " + receiverAccountNumber);

            // 6. Insert credit transaction (receiver)
            String creditUtr = insertTransaction(conn, generateUTR(), receiverAccountNumber, receiverId, amount, "deposit", mode, description, "Received from " + senderAccountNumber);

            conn.commit();
            LOGGER.info("Transfer successful from {} to {}. Amount: {}", senderAccountNumber, receiverAccountNumber, amount);

            //  NOTIFICATION TRIGGER BLOCK	
            try (Connection connEmail = DBConnection.getConnection()) {
                
                // Re-fetch Account IDs 
                 senderId = getAccountIdByNumber(connEmail, senderAccountNumber);
                 receiverId = getAccountIdByNumber(connEmail, receiverAccountNumber);

                // 1. Fetch Sender and Receiver Details
                Customer senderCustomer = getCustomerByAccountId(connEmail, senderId);
                Customer receiverCustomer = getCustomerByAccountId(connEmail, receiverId);

                
                
                double senderFinalBalance = getBalance(connEmail, senderId);
                double receiverFinalBalance = getBalance(connEmail, receiverId);
                
                // 2. Sender Notification (Debit)
                Transaction debitTx = new Transaction();
                debitTx.setAccountNumber(senderAccountNumber);
                debitTx.setTransactionAmount(amount);
                debitTx.setDescription("Transfer to " + description);
                debitTx.setBalanceAmount(senderFinalBalance); // Use final balance
                debitTx.setUtrNumber(debitUtr);
                
                notificationService.sendDebitNotification(senderCustomer, debitTx); 

                // 3. Receiver Notification (Credit)
                Transaction creditTx = new Transaction();
                creditTx.setAccountNumber(receiverAccountNumber);
                creditTx.setTransactionAmount(amount);
                creditTx.setDescription("Received from " + description);
                creditTx.setBalanceAmount(receiverFinalBalance); // Use final balance
                creditTx.setUtrNumber(creditUtr);
                
                notificationService.sendCreditNotification(receiverCustomer, creditTx); 
                
            } catch (Exception e) {
                System.err.println("Email notification failed during transfer: " + e.getMessage());
            }
          
            
        } 
            catch (Exception e) {
                     if (conn != null) {
                    try {
                        conn.rollback();
                        LOGGER.error("Transfer rolled back from {} to {}.", senderAccountNumber, receiverAccountNumber, e); 
                    } catch (SQLException rollbackEx) {
                        LOGGER.error("Rollback failed.", rollbackEx);
                    }
                    
                }
               
                if (e instanceof InsufficientFundsException) {
                    throw (InsufficientFundsException) e;
                }
                throw new SQLException("Transfer failed due to database error: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException closeEx) {
                    	LOGGER.error("Connection close failed.", closeEx);
                    }
                }
            }
            
            
        }
    
    
    //  helper methods
    private double getBalance(Connection conn, int accountId) throws SQLException {
        String sql = "SELECT balance FROM account_details WHERE account_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
            LOGGER.warn("Balance fetch failed for account ID: {}", accountId);
            throw new SQLException("Account not found: " + accountId);
        }
    }

    private void updateBalance(Connection conn, int accountId, double delta) throws SQLException {
        String sql = "UPDATE account_details SET balance = balance + ? WHERE account_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, delta);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    private String insertTransaction(Connection conn, String utr,String accountNumber, int accountId, double amount,
                                   String type, String mode, String description, String receiver) throws SQLException {
        String sql = "INSERT INTO transaction_details " +
                "(utr_number, transaction_amount, account_id, balance_amount, description, transaction_type, mode_of_transaction, receiver_by, date_of_transaction, debited_date, modified_by) " +
                "VALUES (?, ?, ?, (SELECT balance FROM account_details WHERE account_id=?), ?, ?, ?, ?, NOW(), NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, utr);
            ps.setDouble(2, amount);
            ps.setInt(3, accountId);
            ps.setInt(4, accountId);
            ps.setString(5, description);
            ps.setString(6, type);
            ps.setString(7, mode);
            ps.setString(8, receiver);
            ps.setString(9, "System"); 
            ps.executeUpdate();
            return utr; 
        
        }
    }
    
    private Customer getCustomerByAccountId(Connection conn, int accountId) throws SQLException {
        //  Get customer_id from account_details
        String getCustomerIdSql = "SELECT customer_id FROM account_details WHERE account_id = ?";
        int customerId = 0;
        try (PreparedStatement ps = conn.prepareStatement(getCustomerIdSql)) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) customerId = rs.getInt("customer_id");
            else throw new SQLException("Customer ID not found for account ID: " + accountId);
        }

        //  Get full Customer details 
        String getCustomerSql = "SELECT customer_name, email FROM customer_details WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(getCustomerSql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Customer customer = new Customer();
                customer.setCustomerName(rs.getString("customer_name"));
                customer.setEmail(rs.getString("email"));
                return customer;
            }
            throw new SQLException("Customer details not found for ID: " + customerId);
        }
    }
    public String generateUTR() { 
        return "UTR" + String.format("%012d", new java.util.Random().nextLong(1_000_000_000_000L));
    }

}


