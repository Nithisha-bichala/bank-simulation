package com.example.bank.tables;

import com.example.bank.db.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class CustomerTable {
    public static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS customer_details (" +
                "customer_id INT AUTO_INCREMENT PRIMARY KEY," +
                "customer_name VARCHAR(100) NOT NULL," +
                "username VARCHAR(100) NOT NULL UNIQUE," +
                "password VARCHAR(100) NOT NULL," +
                "aadhar_number VARCHAR(20) NOT NULL UNIQUE," +
                "permanent_address VARCHAR(255)," +
                "state VARCHAR(50)," +
                "country VARCHAR(50)," +
                "city VARCHAR(50)," +
                "email VARCHAR(100) NOT NULL UNIQUE," +
                "phone_number VARCHAR(20)," +
                "status VARCHAR(20)," +
                "dob DATE," +
                "age INT," +
                "created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "modified_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "gender VARCHAR(10)," +
                "father_name VARCHAR(100)," +
                "mother_name VARCHAR(100)" +
                ")";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Customer Table Created Successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
