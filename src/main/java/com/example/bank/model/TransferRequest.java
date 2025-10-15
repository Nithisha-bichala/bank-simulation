package com.example.bank.model;

public class TransferRequest {
  
	private String senderAccountNumber;
    private String receiverAccountNumber;
    private double amount;
    private String modeOfTransaction;
    private String description;
    private String mpin;

    
    public String getSenderAccountNumber() {
        return senderAccountNumber;
    }
    public void setSenderAccountNumber(String senderAccountNumber) {
        this.senderAccountNumber = senderAccountNumber;
    }

    public String getReceiverAccountNumber() {
        return receiverAccountNumber;
    }
    public void setReceiverAccountNumber(String receiverAccountNumber) {
        this.receiverAccountNumber = receiverAccountNumber;
    }

    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getModeOfTransaction() {
        return modeOfTransaction;
    }
    public void setModeOfTransaction(String modeOfTransaction) {
        this.modeOfTransaction = modeOfTransaction;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getMpin() { return mpin; }
    public void setMpin(String mpin) { this.mpin = mpin; }
}
