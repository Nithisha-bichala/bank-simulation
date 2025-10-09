package com.example.bank.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnection {
	 private static final Logger LOGGER = LoggerFactory.getLogger(DBConnection.class);
	 
    private static final String URL = "jdbc:mysql://localhost:3306/bankdb"; 
    private static final String USER = "root";  
    private static final String PASSWORD = "root"; 

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); 
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            LOGGER.error("MySQL JDBC Driver not found!", e);
            return null;
        } catch (SQLException e) {         
            LOGGER.error("SQL Connection failed for URL: {}", URL, e);
            return null;
        } 
    }
}



    