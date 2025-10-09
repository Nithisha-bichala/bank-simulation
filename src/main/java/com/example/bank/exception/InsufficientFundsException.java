package com.example.bank.exception;


public class InsufficientFundsException extends Exception {
    
    
    public InsufficientFundsException() {
        super("Transaction failed due to insufficient funds.");
    }

   
    public InsufficientFundsException(String message) {
        super(message);
    }
}