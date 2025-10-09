package com.example.bank.tables;

import com.example.bank.db.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class TransactionTable {
    public static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS transaction_details (" +
                "transaction_id INT AUTO_INCREMENT PRIMARY KEY," +
                "utr_number VARCHAR(50) UNIQUE," +
                "date_of_transaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "transaction_amount DECIMAL(15,2)," +
                "debited_date TIMESTAMP NULL," +
                "account_id INT," +
                "balance_amount DECIMAL(15,2)," +
                "description VARCHAR(255)," +
                "modified_by VARCHAR(100)," +
                "receiver_by VARCHAR(100)," +
                "transaction_type VARCHAR(50)," +  
                "mode_of_transaction VARCHAR(50)," + 
                "FOREIGN KEY (account_id) REFERENCES account_details(account_id)" +
                ")";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println(" Transaction Table Created Successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
