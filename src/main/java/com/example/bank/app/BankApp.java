package com.example.bank.app;

import com.example.bank.tables.CustomerTable;
import com.example.bank.tables.AccountTable;
import com.example.bank.tables.TransactionTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BankApp {
	 private static final Logger LOGGER = LoggerFactory.getLogger(BankApp.class);
    public static void main(String[] args) {
    	  LOGGER.info("Starting Bank Simulation Project...");

          
          CustomerTable.createTable();
          AccountTable.createTable();
          TransactionTable.createTable();
          
          LOGGER.info("All Database Tables Checked/Created Successfully!");
       
    }
}


