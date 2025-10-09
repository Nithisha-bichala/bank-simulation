
package com.example.bank.service;

import com.example.bank.model.Transaction;
import com.example.bank.exception.InsufficientFundsException;
import java.sql.SQLException;
import java.util.List;

public interface TransactionService {
    
    Transaction deposit(Transaction tx) throws SQLException;
    Transaction withdraw(Transaction tx) throws SQLException, InsufficientFundsException; 

    List<Transaction> getTransactionsByAccountNumber(String accountNumber) throws SQLException;

    void transferFunds(String senderAccountNumber, String receiverAccountNumber, 
                       double amount, String mode, String description) 
            throws SQLException, InsufficientFundsException;
}
