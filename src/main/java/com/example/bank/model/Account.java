package com.example.bank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Account {

    @JsonProperty("account_id")
    private int accountId;

    @JsonProperty("customer_id")
    private int customerId;

    @JsonProperty("account_type")
    private String accountType;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("branch")
    private String branch;

    @JsonProperty("balance")
    private double balance;

    @JsonProperty("status")
    private String status;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("ifsc_code")
    private String ifscCode;

    @JsonProperty("name_on_account")
    private String nameOnAccount;

    @JsonProperty("phone_linked_with_bank")
    private String phoneLinkedWithBank;

    @JsonProperty("saving_amount")
    private double savingAmount;

    // ------------------- Constructors -------------------
    public Account() {}

    public Account(int accountId, int customerId, String accountType, String bankName, String branch,
                   double balance, String status, String accountNumber, String ifscCode, String nameOnAccount,
                   String phoneLinkedWithBank, double savingAmount) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.accountType = accountType;
        this.bankName = bankName;
        this.branch = branch;
        this.balance = balance;
        this.status = status;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.nameOnAccount = nameOnAccount;
        this.phoneLinkedWithBank = phoneLinkedWithBank;
        this.savingAmount = savingAmount;
    }

    // ------------------- Getters and Setters -------------------
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getNameOnAccount() { return nameOnAccount; }
    public void setNameOnAccount(String nameOnAccount) { this.nameOnAccount = nameOnAccount; }

    public String getPhoneLinkedWithBank() { return phoneLinkedWithBank; }
    public void setPhoneLinkedWithBank(String phoneLinkedWithBank) { this.phoneLinkedWithBank = phoneLinkedWithBank; }

    public double getSavingAmount() { return savingAmount; }
    public void setSavingAmount(double savingAmount) { this.savingAmount = savingAmount; }

    @Override
    public String toString() {
        return ""; 
    }
}
