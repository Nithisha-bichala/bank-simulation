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
    
    
    private final String TEST_MPIN = "1234"; 
    private final String INVALID_MPIN = "0000"; 
    private final String DB_MPIN_COLUMN = "mpin";

    @BeforeEach
    void setUp() throws SQLException {
        // 1. PURE JAVA REFLECTION to inject mockNotificationService
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
    
    // --- Helper Methods ---
    
   
    private void setupPreparedStatementMocks() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);
        when(DBConnection.getConnection()).thenReturn(mockConnection).thenReturn(mockConnection);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
    }
    
 
    private void mockGeneratedKeys() throws SQLException {
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockGeneratedKeysRs);
        when(mockGeneratedKeysRs.next()).thenReturn(true).thenReturn(false);
        when(mockGeneratedKeysRs.getInt(1)).thenReturn(99); 
    }
    
   
    private void mockMpinCheck(String expectedMpin) throws SQLException {
      
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString(eq(DB_MPIN_COLUMN))).thenReturn(expectedMpin);
    }

    // --- DEPOSIT TESTS  ---

    @Test
    void testDeposit_Success() throws Exception {
        setupPreparedStatementMocks();
        doReturn(MOCK_UTR).when(service).generateUTR(); 
        
        double depositAmount = 1000.00;
        double finalBalance = INITIAL_BALANCE + depositAmount;

        // Mock executeQuery chain: ID -> Final Bal -> Cust ID -> Cust Details
        when(mockPreparedStatement.executeQuery())
            .thenReturn(mockResultSet) 
            .thenReturn(mockResultSet)
            .thenReturn(mockResultSet)
            .thenReturn(mockResultSet);
            
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
        verify(mockConnection, times(1)).commit(); // MPIN check is NOT performed for deposit in the service implementation provided.
    }
    
    // --- WITHDRAWAL TESTS ---

    @Test
    void testWithdraw_Success() throws Exception {
        setupPreparedStatementMocks();
        doReturn(MOCK_UTR).when(service).generateUTR();
        
        double withdrawAmount = 500.00;
        double finalBalance = INITIAL_BALANCE - withdrawAmount;

        // MOCK SEQUENCE: MPIN -> ID -> Balance Check -> Final Bal -> Cust ID -> Cust Details
        when(mockPreparedStatement.executeQuery())
            .thenReturn(mockResultSet) // 1. MPIN Check (Success)
            .thenReturn(mockResultSet) // 2. ID
            .thenReturn(mockResultSet) // 3. Balance Check
            .thenReturn(mockResultSet) // 4. Final Bal
            .thenReturn(mockResultSet) // 5. Cust ID
            .thenReturn(mockResultSet); // 6. Cust Details
            
        when(mockResultSet.next()).thenReturn(true, true, true, true, true, true).thenReturn(false); 
        
        // Mock MPIN Check result (Execution 1)
        when(mockResultSet.getString(eq(DB_MPIN_COLUMN))).thenReturn(TEST_MPIN); 

        // Subsequent Mocks
        when(mockResultSet.getInt(eq("account_id"))).thenReturn(TEST_ACC_ID_1);
        when(mockResultSet.getDouble(anyInt()))
            .thenReturn(INITIAL_BALANCE) // Balance Check
            .thenReturn(finalBalance);   // Final Balance

        when(mockResultSet.getInt(eq("customer_id"))).thenReturn(TEST_CUST_ID_1);
        when(mockResultSet.getString(eq("customer_name"))).thenReturn("Test Customer");
        when(mockResultSet.getString(eq("email"))).thenReturn("test@example.com");
        
        mockGeneratedKeys();

        Transaction tx = new Transaction();
        tx.setAccountNumber(TEST_ACC_NO_1);
        tx.setTransactionAmount(withdrawAmount);
        tx.setMpin(TEST_MPIN); 

        service.withdraw(tx);

        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockPreparedStatement, times(6)).executeQuery();
        verify(mockPreparedStatement, times(2)).executeUpdate(); 
        verify(mockNotificationService, times(1)).sendDebitNotification(any(Customer.class), any(Transaction.class));
        verify(mockConnection, times(1)).commit();
    }
    
    @Test
    void testWithdraw_InvalidMpin_ThrowsSQLException() throws SQLException {
        setupPreparedStatementMocks();
        double withdrawAmount = 100.00; 

        // MOCK SEQUENCE: Only the MPIN check is run before throwing.
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet); 
        
        // Mock MPIN Check result: DB has a valid MPIN, but the input is wrong.
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString(eq(DB_MPIN_COLUMN))).thenReturn(TEST_MPIN); 

        Transaction tx = new Transaction();
        tx.setAccountNumber(TEST_ACC_NO_1);
        tx.setTransactionAmount(withdrawAmount);
        tx.setMpin(INVALID_MPIN);

        assertThrows(SQLException.class, () -> {
            service.withdraw(tx);
        }, "Should throw SQLException because MPIN is invalid.");

        // Verify rollback and no financial updates
        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockConnection, times(1)).rollback(); // Rollback in catch block
        verify(mockPreparedStatement, times(1)).executeQuery(); // Only MPIN check runs
        verify(mockPreparedStatement, never()).executeUpdate(); 
        verify(mockConnection, never()).commit();
    }
    
    @Test
    void testWithdraw_MissingMpin_ThrowsSQLException() throws SQLException {
        setupPreparedStatementMocks();
        double withdrawAmount = 100.00; 

        Transaction tx = new Transaction();
        tx.setAccountNumber(TEST_ACC_NO_1);
        tx.setTransactionAmount(withdrawAmount);
        tx.setMpin(null); 

        // Execute and assert the exception thrown by checkMpin on null
        assertThrows(SQLException.class, () -> {
            service.withdraw(tx);
        }, "Should throw SQLException because MPIN is required.");

        // Verify rollback and no financial updates (rollback happens due to exception)
        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockConnection, times(1)).rollback();
        verify(mockPreparedStatement, never()).executeQuery(); // Query is skipped
        verify(mockPreparedStatement, never()).executeUpdate();
        verify(mockConnection, never()).commit();
    }

    // --- TRANSFER TESTS ---
    
    @Test
    void testTransfer_Success() throws Exception {
        setupPreparedStatementMocks();
        doReturn(MOCK_UTR + "D").doReturn(MOCK_UTR + "C").when(service).generateUTR();

        double amount = 500.00;
        double finalSenderBalance = INITIAL_BALANCE - amount;
        double finalReceiverBalance = INITIAL_BALANCE + amount;

        when(mockPreparedStatement.executeQuery())
            // 1. MPIN Check (Success)
            .thenReturn(mockResultSet) 
            // 2. Sender ID 
            .thenReturn(mockResultSet) 
            // 3. Receiver ID 
            .thenReturn(mockResultSet) 
            // 4. Sender Balance Check 
            .thenReturn(mockResultSet)
            // 5. Sender ID (Re-fetch for Notification)
            .thenReturn(mockResultSet)
            // 6. Receiver ID (Re-fetch for Notification)
            .thenReturn(mockResultSet)
            // 7-10. Customer Lookups 
            .thenReturn(mockResultSet).thenReturn(mockResultSet).thenReturn(mockResultSet).thenReturn(mockResultSet)
            // 11. Sender Final Balance 
            .thenReturn(mockResultSet) 
            // 12. Receiver Final Balance 
            .thenReturn(mockResultSet); 

        when(mockResultSet.next()).thenReturn(true, true, true, true, true, true, true, true, true, true, true, true).thenReturn(false);
        
      
        when(mockResultSet.getString(eq(DB_MPIN_COLUMN))).thenReturn(TEST_MPIN);
        

        // Mock ID fetches (2, 3, 5, 6)
        when(mockResultSet.getInt(eq("account_id"))).thenReturn(TEST_ACC_ID_1).thenReturn(TEST_ACC_ID_2)
                                                    .thenReturn(TEST_ACC_ID_1).thenReturn(TEST_ACC_ID_2);
        
        // Mock Balance values (4, 11, 12)
        when(mockResultSet.getDouble(anyInt()))
            .thenReturn(INITIAL_BALANCE)      
            .thenReturn(finalSenderBalance)   
            .thenReturn(finalReceiverBalance); 

        // Mock Customer IDs/Details (7, 8, 9, 10 - must be mocked explicitly as it uses getInt/getString again)
        when(mockResultSet.getInt(eq("customer_id"))).thenReturn(TEST_CUST_ID_1).thenReturn(TEST_CUST_ID_1);
        when(mockResultSet.getString(eq("customer_name"))).thenReturn("Test Customer");
        when(mockResultSet.getString(eq("email"))).thenReturn("test@example.com");

        mockGeneratedKeys(); 

        // Execute: Pass the correct TEST_MPIN
        service.transferFunds(TEST_ACC_NO_1, TEST_ACC_NO_2, amount, "NEFT", "Test Transfer", TEST_MPIN);

        // Verify flow
        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockPreparedStatement, times(12)).executeQuery(); 
        verify(mockPreparedStatement, times(4)).executeUpdate(); 
        verify(mockNotificationService, times(1)).sendDebitNotification(any(Customer.class), any(Transaction.class));
        verify(mockNotificationService, times(1)).sendCreditNotification(any(Customer.class), any(Transaction.class));
        verify(mockConnection, times(1)).commit();
    }
   
}