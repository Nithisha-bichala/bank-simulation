package com.example.bank.service.impl;

import com.example.bank.db.DBConnection;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Customer;
import com.example.bank.model.Transaction;
import com.example.bank.service.alert.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito; 

import java.sql.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @InjectMocks 
    @Spy 
    TransactionServiceImpl service; 

    @Mock 
    NotificationService mockNotificationService; 
    @Mock(lenient = true) Connection mockConnection;
    @Mock(lenient = true) PreparedStatement mockPreparedStatement;
    @Mock(lenient = true) ResultSet mockResultSet;
    @Mock(lenient = true) ResultSet mockGeneratedKeysRs;
    
    MockedStatic<DBConnection> mockedDbConnection;

    private final String TEST_ACC_NO_1 = "ACC100";
    private final String TEST_ACC_NO_2 = "ACC200";
    private final String TEST_ACC_NO_NONEXIST = "ACC999";
    private final int TEST_ACC_ID_1 = 10;
    private final int TEST_ACC_ID_2 = 20;
    private final int TEST_CUST_ID_1 = 1;
    private final double INITIAL_BALANCE = 5000.00;
    private final String MOCK_UTR = "MOCKUTR123456789";

    @BeforeEach
    void setUp() throws SQLException {
        try {
            java.lang.reflect.Field field = TransactionServiceImpl.class.getDeclaredField("notificationService");
            field.setAccessible(true);
            field.set(service, mockNotificationService);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject mockNotificationService via reflection.", e);
        }

        // 2. Mock static DBConnection
        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() {
        if (mockedDbConnection != null) {
            mockedDbConnection.close();
        }
    }
    
    // Helper Methods
    private void setupPreparedStatementMocks() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);
        when(DBConnection.getConnection()).thenReturn(mockConnection).thenReturn(mockConnection);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
    }
    
    /* Mocks fetching the generated transaction ID (ID 99) */
    private void mockGeneratedKeys() throws SQLException {
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockGeneratedKeysRs);
        when(mockGeneratedKeysRs.next()).thenReturn(true).thenReturn(false);
        when(mockGeneratedKeysRs.getInt(1)).thenReturn(99); 
    }
    
    // --- DEPOSIT TESTS ---

    @Test
    void testDeposit_Success() throws Exception {
        setupPreparedStatementMocks();
        doReturn(MOCK_UTR).when(service).generateUTR(); 
        
        double depositAmount = 1000.00;
        double finalBalance = INITIAL_BALANCE + depositAmount;

        // Mock executeQuery chain for: ID -> Final Bal -> Cust ID -> Cust Details
        when(mockPreparedStatement.executeQuery())
            .thenReturn(mockResultSet) 
            .thenReturn(mockResultSet)
            .thenReturn(mockResultSet)
            .thenReturn(mockResultSet);
            
        // Mock ResultSet behavior 
        when(mockResultSet.next()).thenReturn(true, true, true, true).thenReturn(false); 
        when(mockResultSet.getInt(eq("account_id"))).thenReturn(TEST_ACC_ID_1);
        when(mockResultSet.getDouble(anyInt())).thenReturn(finalBalance);
        when(mockResultSet.getInt(eq("customer_id"))).thenReturn(TEST_CUST_ID_1);
        when(mockResultSet.getString(eq("customer_name"))).thenReturn("Test Customer");
        when(mockResultSet.getString(eq("email"))).thenReturn("test@example.com");

        mockGeneratedKeys();

        Transaction tx = new Transaction();
        tx.setAccountNumber(TEST_ACC_NO_1);
        tx.setTransactionAmount(depositAmount);
        
        service.deposit(tx);

        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockPreparedStatement, times(4)).executeQuery(); 
        verify(mockPreparedStatement, times(2)).executeUpdate(); 
        verify(mockNotificationService, times(1)).sendCreditNotification(any(Customer.class), any(Transaction.class));
        verify(mockConnection, times(1)).commit();
    }
    
    // --- WITHDRAWAL TESTS ---

    @Test
    void testWithdraw_Success() throws Exception {
        setupPreparedStatementMocks();
        doReturn(MOCK_UTR).when(service).generateUTR();
        
        double withdrawAmount = 500.00;
        double finalBalance = INITIAL_BALANCE - withdrawAmount;

        // Mock executeQuery chain: ID -> Balance Check -> Final Bal -> Cust ID -> Cust Details
        when(mockPreparedStatement.executeQuery())
            .thenReturn(mockResultSet) // 1. SELECT account_id
            .thenReturn(mockResultSet) // 2. SELECT balance (Check)
            .thenReturn(mockResultSet) // 3. SELECT balance (Final)
            .thenReturn(mockResultSet) // 4. SELECT customer_id (Notification)
            .thenReturn(mockResultSet); // 5. SELECT customer_details (Notification)
            
        when(mockResultSet.next()).thenReturn(true, true, true, true, true).thenReturn(false); 
        when(mockResultSet.getInt(eq("account_id"))).thenReturn(TEST_ACC_ID_1);
        when(mockResultSet.getDouble(anyInt()))
            .thenReturn(INITIAL_BALANCE) 
            .thenReturn(finalBalance);   

        when(mockResultSet.getInt(eq("customer_id"))).thenReturn(TEST_CUST_ID_1);
        when(mockResultSet.getString(eq("customer_name"))).thenReturn("Test Customer");
        when(mockResultSet.getString(eq("email"))).thenReturn("test@example.com");
        
        mockGeneratedKeys();

        Transaction tx = new Transaction();
        tx.setAccountNumber(TEST_ACC_NO_1);
        tx.setTransactionAmount(withdrawAmount);

        service.withdraw(tx);

        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockPreparedStatement, times(5)).executeQuery(); 
        verify(mockPreparedStatement, times(2)).executeUpdate(); 
        verify(mockNotificationService, times(1)).sendDebitNotification(any(Customer.class), any(Transaction.class));
        verify(mockConnection, times(1)).commit();
    }

    @Test
    void testWithdraw_InsufficientFunds_ThrowsException() throws SQLException {
        setupPreparedStatementMocks();
        double withdrawAmount = 6000.00; 

        // Mock executeQuery chain: ID Fetch -> Balance Check (5000)
        when(mockPreparedStatement.executeQuery())
            .thenReturn(mockResultSet) 
            .thenReturn(mockResultSet); 
        
        when(mockResultSet.next()).thenReturn(true, true).thenReturn(false);
        when(mockResultSet.getInt(eq("account_id"))).thenReturn(TEST_ACC_ID_1);
        when(mockResultSet.getDouble(anyInt())).thenReturn(INITIAL_BALANCE); 

        Transaction tx = new Transaction();
        tx.setAccountNumber(TEST_ACC_NO_1);
        tx.setTransactionAmount(withdrawAmount);

        assertThrows(InsufficientFundsException.class, () -> {
            service.withdraw(tx);
        });

        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockConnection, times(1)).rollback();
        verify(mockPreparedStatement, never()).executeUpdate(); 
        verify(mockConnection, never()).commit();
    }
    
    // Withdrawal - Account Not Found
    
    @Test
    void testWithdraw_AccountNotFound_ThrowsSQLException() throws SQLException {
        setupPreparedStatementMocks();

        // 1. Mock SELECT account_id to return NO rows
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); 
        
        Transaction tx = new Transaction();
        tx.setAccountNumber(TEST_ACC_NO_NONEXIST);
        tx.setTransactionAmount(100.00);

        // Expect SQLException thrown by getAccountIdByNumber helper
        assertThrows(SQLException.class, () -> {
            service.withdraw(tx);
        }, "Should throw SQLException if account ID is not found.");

        // Verify rollback and no updates
        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockConnection, times(1)).rollback();
        verify(mockPreparedStatement, times(1)).executeQuery(); // Only the failed ID check
        verify(mockConnection, never()).commit();
    }
    @Test
    void testTransfer_Success() throws Exception {
      
        when(DBConnection.getConnection())
                .thenReturn(mockConnection) 
                .thenReturn(mockConnection) 
                .thenReturn(mockConnection) 
                .thenReturn(mockConnection) 
                .thenReturn(mockConnection) 
                .thenReturn(mockConnection) 
                .thenReturn(mockConnection) 
                .thenReturn(mockConnection) 
                .thenReturn(mockConnection)
                .thenReturn(mockConnection)
                .thenReturn(mockConnection)
                .thenReturn(mockConnection); 

        setupPreparedStatementMocks();
        // Mock UTR calls for debit and credit transactions
        doReturn(MOCK_UTR + "D").doReturn(MOCK_UTR + "C").when(service).generateUTR();

        double amount = 500.00;
        double finalSenderBalance = INITIAL_BALANCE - amount;
        double finalReceiverBalance = INITIAL_BALANCE + amount;

  
        when(mockPreparedStatement.executeQuery())
            // 1. Sender ID (getAccountIdByNumber)
            .thenReturn(mockResultSet) 
            // 2. Receiver ID (getAccountIdByNumber)
            .thenReturn(mockResultSet) 
            // 3. Sender Balance Check (getBalance)
            .thenReturn(mockResultSet)
            // 4. Sender ID (Re-fetch for Notification)
            .thenReturn(mockResultSet)
            // 5. Receiver ID (Re-fetch for Notification)
            .thenReturn(mockResultSet)
            // 6. Sender Cust ID (getCustomerByAccountId - step 1)
            .thenReturn(mockResultSet)
            // 7. Sender Cust Details (getCustomerByAccountId - step 2)
            .thenReturn(mockResultSet)
            // 8. Receiver Cust ID (getCustomerByAccountId - step 1)
            .thenReturn(mockResultSet)
            // 9. Receiver Cust Details (getCustomerByAccountId - step 2)
            .thenReturn(mockResultSet)
            // 10. Sender Final Balance (getBalance for Notification)
            .thenReturn(mockResultSet) 
            // 11. Receiver Final Balance (getBalance for Notification)
            .thenReturn(mockResultSet); 

        when(mockResultSet.next()).thenReturn(true, true, true, true, true, true, true, true, true, true, true).thenReturn(false);
         
        when(mockResultSet.getInt(eq("account_id"))).thenReturn(TEST_ACC_ID_1).thenReturn(TEST_ACC_ID_2)
                                                    .thenReturn(TEST_ACC_ID_1).thenReturn(TEST_ACC_ID_2);
        
        when(mockResultSet.getDouble(anyInt()))
            .thenReturn(INITIAL_BALANCE)      
            .thenReturn(finalSenderBalance)   
            .thenReturn(finalReceiverBalance); 

        when(mockResultSet.getInt(eq("customer_id"))).thenReturn(TEST_CUST_ID_1).thenReturn(TEST_CUST_ID_1);
        when(mockResultSet.getString(eq("customer_name"))).thenReturn("Test Customer");
        when(mockResultSet.getString(eq("email"))).thenReturn("test@example.com");

        mockGeneratedKeys();

        // --- Execution & Verification ---
        service.transferFunds(TEST_ACC_NO_1, TEST_ACC_NO_2, amount, "NEFT", "Test Transfer");

        // Assertions
        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockConnection, times(1)).commit();
        
        // Verifications
        verify(mockPreparedStatement, times(11)).executeQuery(); 
        verify(mockPreparedStatement, times(4)).executeUpdate(); 
        
        verify(mockNotificationService, times(1)).sendDebitNotification(any(Customer.class), any(Transaction.class));
        verify(mockNotificationService, times(1)).sendCreditNotification(any(Customer.class), any(Transaction.class));
    }
    // Transfer - Receiver Account Not Found (Rollback)
        @Test
    void testTransfer_ReceiverNotFound_RollsBack() throws Exception {
        setupPreparedStatementMocks();
        double amount = 100.00;

        // Mock executeQuery chain: 
        when(mockPreparedStatement.executeQuery())
            // 1. Sender ID: SUCCESS (Returns ID 10)
            .thenReturn(mockResultSet) 
            // 2. Receiver ID: FAIL (Returns no rows)
            .thenReturn(mockResultSet) 
            // 3. Sender Balance Check
            .thenReturn(mockResultSet); 

        // Mock ResultSet behavior:
        when(mockResultSet.next())
            .thenReturn(true)  // For Sender ID (Success)
            .thenReturn(false); // For Receiver ID (Failure, throws SQLException)

        // Mock Sender ID fetch result
        when(mockResultSet.getInt(eq("account_id"))).thenReturn(TEST_ACC_ID_1);

        // Execute and assert the expected exception (SQLException thrown by getAccountIdByNumber)
        assertThrows(SQLException.class, () -> {
            service.transferFunds(TEST_ACC_NO_1, TEST_ACC_NO_NONEXIST, amount, "NEFT", "Test Transfer");
        }, "Should throw SQLException if receiver account is not found.");

        // Verify rollback and no balance updates
        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockConnection, times(1)).rollback();
        verify(mockPreparedStatement, times(2)).executeQuery(); // Only the two ID lookups executed
        verify(mockPreparedStatement, never()).executeUpdate(); // No deduction or credit occurred
        verify(mockConnection, never()).commit();
    }
    // Transfer - Insufficient Funds
    
    @Test
    void testTransfer_InsufficientFundsCheck_ThrowsException() throws Exception {
        setupPreparedStatementMocks();
        double amount = 6000.00; 

        // Mock executeQuery chain:
        when(mockPreparedStatement.executeQuery())
            .thenReturn(mockResultSet) 
            .thenReturn(mockResultSet) 
            .thenReturn(mockResultSet); 
        
        // Mock ResultSet behavior:
        when(mockResultSet.next()).thenReturn(true, true, true).thenReturn(false);
        
        // Mock ID fetches (1, 2)
        when(mockResultSet.getInt(eq("account_id"))).thenReturn(TEST_ACC_ID_1).thenReturn(TEST_ACC_ID_2);
        
        // Mock Balance value (3)
        when(mockResultSet.getDouble(anyInt())).thenReturn(INITIAL_BALANCE); 

        // Execute and assert the expected exception
        assertThrows(InsufficientFundsException.class, () -> {
            service.transferFunds(TEST_ACC_NO_1, TEST_ACC_NO_2, amount, "NEFT", "Overdraft transfer");
        }, "Should throw InsufficientFundsException immediately.");

        // Verify rollback and no updates
        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockConnection, times(1)).rollback();
        verify(mockPreparedStatement, times(3)).executeQuery(); // ID, ID, Balance Check
        verify(mockPreparedStatement, never()).executeUpdate(); // No deduction occurred
        verify(mockConnection, never()).commit();
        verify(mockNotificationService, never()).sendDebitNotification(any(), any());
    }
   
}