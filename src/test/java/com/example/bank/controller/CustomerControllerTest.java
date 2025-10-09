package com.example.bank.controller;

import com.example.bank.db.DBConnection;
import com.example.bank.model.Customer;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension; 
import org.junit.jupiter.api.AfterEach; 

import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @InjectMocks
    CustomerController controller;

    // Mock dependencies: Applying lenient=true to avoid UnnecessaryStubbingException
    @Mock(lenient = true) 
    Connection mockConnection;
    @Mock(lenient = true)
    PreparedStatement mockPreparedStatement;
    @Mock(lenient = true)
    ResultSet mockResultSet; 
    
    MockedStatic<DBConnection> mockedDbConnection;

    @BeforeEach
    void setUp() throws SQLException {
        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
        
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet); 
    }
    
    @AfterEach
    void tearDown() {
        if (mockedDbConnection != null) {
            mockedDbConnection.close();
        }
    }
    
    // Helper method to create a valid customer object
    private Customer createValidCustomer() {
        Customer c = new Customer();
        c.setCustomerName("Alice");
        c.setUsername("alice_unique_1");
        c.setPassword("Pass@123");
        c.setPhoneNumber("9876543210");
        c.setEmail("alice_unique1@example.com");
        c.setAge(25);
        c.setDob(java.sql.Date.valueOf("1998-01-01")); 
        c.setAadharNumber("111122223333");
        c.setStatus("Active");
        return c;
    }

    // Valid Customer
    @Test
    void testCreateCustomer_Valid() throws SQLException {
        Customer c = createValidCustomer();
        
        ResultSet mockGeneratedKeysRs = mock(ResultSet.class);
        
        when(mockResultSet.next()) 
            .thenReturn(true)  
            .thenReturn(false); 
        
        when(mockResultSet.getInt(1)).thenReturn(0);   
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockGeneratedKeysRs);
        when(mockGeneratedKeysRs.next()) 
            .thenReturn(true)  // Start reading generated key
            .thenReturn(false); // End reading generated key
            
        when(mockGeneratedKeysRs.getInt(1)).thenReturn(500); 
        

        Response resp = controller.createCustomer(c);
        
        assertEquals(201, resp.getStatus(), "Valid customer should be created");
        // Check that the returned Customer object has the ID set by the controller
        assertEquals(500, ((Customer) resp.getEntity()).getCustomerId(), "Customer ID should be set");
        
        verify(mockPreparedStatement, times(1)).executeUpdate();
        verify(mockPreparedStatement, times(1)).getGeneratedKeys();
        verify(mockGeneratedKeysRs, times(1)).getInt(1); 
    }

    // Fetch existing customer
    @Test
    void testGetCustomer_Valid() throws SQLException {
        // Mock the ResultSet to contain a valid customer record
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt("customer_id")).thenReturn(501);
        when(mockResultSet.getString("customer_name")).thenReturn("Bob");
        when(mockResultSet.getString("email")).thenReturn("bob@example.com");
        
        Response resp = controller.getCustomer(501);
        
        assertEquals(200, resp.getStatus(), "Customer should be found");
        Customer fetchedCustomer = (Customer) resp.getEntity();
        assertEquals(501, fetchedCustomer.getCustomerId());
        assertEquals("Bob", fetchedCustomer.getCustomerName());
    }

    // Invalid phone number - Controller validation
    @Test
    void testCreateCustomer_InvalidPhone() throws SQLException {
        Customer c = createValidCustomer();
        c.setPhoneNumber("123"); // invalid format
        
        Response resp = controller.createCustomer(c);
        
        assertEquals(400, resp.getStatus(), "Invalid phone should return 400");
        // Verify database was never touched due to early validation failure
        verifyNoInteractions(mockConnection); 
    }

    // Invalid email - Controller validation
    @Test
    void testCreateCustomer_InvalidEmail() throws SQLException {
        Customer c = createValidCustomer();
        c.setEmail("invalid-email"); // invalid format

        Response resp = controller.createCustomer(c);
        
        assertEquals(400, resp.getStatus(), "Invalid email should return 400");
        verifyNoInteractions(mockConnection); // No DB connection attempted
    }

    //Duplicate username/email - Database check failure
    @Test
    void testCreateCustomer_Duplicate() throws SQLException {
        Customer c = createValidCustomer();
        
        
        when(mockResultSet.next()).thenReturn(true).thenReturn(false); 
        when(mockResultSet.getInt(1)).thenReturn(1); 
        
        Response resp = controller.createCustomer(c);
        
        assertEquals(500, resp.getStatus(), "Duplicate check should return 500");
        verify(mockPreparedStatement, times(1)).executeQuery(); 
        verify(mockPreparedStatement, never()).executeUpdate(); 
    }

    // Non-existent customer - GET
    @Test
    void testGetCustomer_NotFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        
        Response resp = controller.getCustomer(999999); 
        
        assertEquals(404, resp.getStatus(), "Non-existent customer should return 404");
        verify(mockPreparedStatement, times(1)).executeQuery();
    }
}
