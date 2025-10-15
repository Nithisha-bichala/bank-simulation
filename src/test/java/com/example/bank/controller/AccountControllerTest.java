package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.db.DBConnection;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach; 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt; 
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@SuppressWarnings("MockitoUnnecessaryStubbing") 
class AccountControllerTest {

    @InjectMocks
    AccountController controller;

    // Mock dependencies: Using lenient=true to allow stubbing in @BeforeEach
    @Mock(lenient = true)
    Connection mockConnection;
    @Mock(lenient = true)
    PreparedStatement mockCheckPs;
    @Mock(lenient = true)
    PreparedStatement mockInsertPs;
    @Mock(lenient = true)
    ResultSet mockResultSet; 
    
    // Static mock variable for DBConnection.getConnection()
    MockedStatic<DBConnection> mockedDbConnection; 
    
    // Helper to set up common mock behaviors before each test
    @BeforeEach
    void setUp() throws SQLException {
        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
        
        // Mock the statement used for customer checking/simple GET 
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockCheckPs);

        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockInsertPs);
        
        // Mock the execution of the simple check/get statement
        when(mockCheckPs.executeQuery()).thenReturn(mockResultSet); 
    }
    
    
    @AfterEach
    void tearDown() {
        if (mockedDbConnection != null) {
            mockedDbConnection.close();
        }
    }
    
    //Create valid account
    @Test
    void testCreateAccount_Valid() throws SQLException {
        ResultSet mockGeneratedKeysRs = mock(ResultSet.class); 
        
        Account a = new Account();
        a.setAccountNumber("ACC10001");
        a.setCustomerId(5);
        a.setAccountType("Savings");
        a.setBalance(5000);
        
        // 1. Mock Customer Exists Check (mockCheckPs.executeQuery): Returns true
        when(mockResultSet.next()).thenReturn(true).thenReturn(false); 
        
        // 2. Mock Account Insert (mockInsertPs.executeUpdate): Returns 1
        when(mockInsertPs.executeUpdate()).thenReturn(1);
        
        // 3. Mock Generated Keys (mockInsertPs.getGeneratedKeys): Returns our specific mock
        when(mockInsertPs.getGeneratedKeys()).thenReturn(mockGeneratedKeysRs);
        
        // 4. Stub Generated Keys Behavior: Returns 101
        when(mockGeneratedKeysRs.next()).thenReturn(true).thenReturn(false); 
        when(mockGeneratedKeysRs.getInt(1)).thenReturn(101); 

        Response resp = controller.createAccount(a);
        
        assertEquals(201, resp.getStatus(), "Valid account should be created");
        // Check that the returned Account object has the ID set by the controller
        assertEquals(101, ((Account) resp.getEntity()).getAccountId(), "Account ID should be set from generated keys");
        
        verify(mockCheckPs, times(1)).executeQuery();
        verify(mockInsertPs, times(1)).executeUpdate();
        verify(mockInsertPs, times(1)).getGeneratedKeys();
    }

    // Fetch existing account
    @Test
    void testGetAccount_Valid() throws SQLException {
        // GET calls prepareStatement(String sql)
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt("account_id")).thenReturn(202);
        when(mockResultSet.getString("account_number")).thenReturn("ACC10002");
        when(mockResultSet.getDouble("balance")).thenReturn(10000.0);

        Response resp = controller.getAccount("ACC10002");
        
        assertEquals(200, resp.getStatus(), "Account should be found");
        Account fetchedAccount = (Account) resp.getEntity();
        assertEquals("ACC10002", fetchedAccount.getAccountNumber());
        assertEquals(202, fetchedAccount.getAccountId());
    }

    // Invalid account number (non-digit/invalid format) - Controller validation
    @Test
    void testCreateAccount_InvalidAccountNumber() {
        Account a = new Account();
        a.setAccountNumber("ABC123"); 
        a.setCustomerId(5);
        a.setAccountType("Savings");
        a.setBalance(5000);

        Response resp = controller.createAccount(a);
        assertEquals(400, resp.getStatus(), "Invalid account number should fail");
        
        verifyNoInteractions(mockConnection);
    }
    
    // Duplicate account number - Database Constraint Violation
    @Test
    void testCreateAccount_DuplicateAccountNumber() throws SQLException {
        Account a = new Account();
        a.setAccountNumber("ACC10010");
        a.setCustomerId(6);
        a.setBalance(5000);
        
        when(mockResultSet.next()).thenReturn(true).thenReturn(false); 
        
        when(mockInsertPs.executeUpdate()).thenThrow(new SQLIntegrityConstraintViolationException("Duplicate key error"));

        Response resp = controller.createAccount(a);
        
        assertEquals(500, resp.getStatus(), "Duplicate account number should fail with 500 DB error");
        verify(mockCheckPs, times(1)).executeQuery(); // Customer check occurred
        verify(mockInsertPs, times(1)).executeUpdate(); // Insert was attempted
    }

    //Non-existent customerId - Database Check Failure
    @Test
    void testCreateAccount_NonExistentCustomer() throws SQLException {
        Account a = new Account();
        a.setAccountNumber("ACC10005");
        a.setCustomerId(9999);
        a.setBalance(5000);

        when(mockResultSet.next()).thenReturn(false);

        Response resp = controller.createAccount(a);
        
        assertEquals(400, resp.getStatus(), "Non-existent customer should fail");
        verify(mockCheckPs, times(1)).executeQuery(); 
        verify(mockInsertPs, never()).executeUpdate(); 
    }
    
    // Account Not Found (GET)
    @Test
    void testGetAccount_NotFound() throws SQLException {
        
        when(mockResultSet.next()).thenReturn(false);

        Response resp = controller.getAccount("NONEXISTENT");
        
        assertEquals(404, resp.getStatus(), "Non-existent account should return 404");
        verify(mockCheckPs, times(1)).executeQuery();
    }
}
