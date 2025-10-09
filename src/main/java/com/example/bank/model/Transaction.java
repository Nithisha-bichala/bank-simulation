

package com.example.bank.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class Transaction {
    private int transactionId;
    private String utrNumber;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateOfTransaction;
    private double transactionAmount;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime debitedDate;
    
    @JsonProperty("account_id")  
    private int accountId;
    
   
    private String accountNumber; 
    
    private double balanceAmount;
    private String description;
    private String modifiedBy;
    private String receiverBy;
    private String transactionType;   
    private String modeOfTransaction; 

   
 // Getters & Setters
    public int getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getUtrNumber() {
        return utrNumber;
    }
    public void setUtrNumber(String utrNumber) {
        this.utrNumber = utrNumber;
    }

    public LocalDateTime getDateOfTransaction() {
        return dateOfTransaction;
    }
    public void setDateOfTransaction(LocalDateTime dateOfTransaction) {
        this.dateOfTransaction = dateOfTransaction;
    }

    public double getTransactionAmount() {
        return transactionAmount;
    }
    public void setTransactionAmount(double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public LocalDateTime getDebitedDate() {
        return debitedDate;
    }
    public void setDebitedDate(LocalDateTime debitedDate) {
        this.debitedDate = debitedDate;
    }

    public int getAccountId() {
        return accountId;
    }
    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

   
    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    public double getBalanceAmount() {
        return balanceAmount;
    }
    public void setBalanceAmount(double balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getReceiverBy() {
        return receiverBy;
    }
    public void setReceiverBy(String receiverBy) {
        this.receiverBy = receiverBy;
    }

    public String getTransactionType() {
        return transactionType;
    }
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getModeOfTransaction() {
        return modeOfTransaction;
    }
    public void setModeOfTransaction(String modeOfTransaction) {
        this.modeOfTransaction = modeOfTransaction;
    }

    
}
