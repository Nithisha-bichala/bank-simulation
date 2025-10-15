package com.example.bank.controller;

import com.example.bank.model.Customer;
import com.example.bank.db.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.*;

@Path("/customer")
public class CustomerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerController.class);
    


    @POST
    @Path("/login") 
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Customer loginRequest) { 
        // SQL checks if a row exists with the matching username AND password
        String sql = "SELECT customer_id, customer_name FROM customer_details WHERE username=? AND password=?";

        // Basic validation
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            return Response.status(400).entity("Username and password are required.").build();
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, loginRequest.getUsername());
            ps.setString(2, loginRequest.getPassword());
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                // Authentication Successful: Return a minimal success response
                Customer authenticatedUser = new Customer();
                authenticatedUser.setCustomerId(rs.getInt("customer_id"));
                authenticatedUser.setCustomerName(rs.getString("customer_name"));
                
                // Return 200 OK with the user's data
                return Response.ok(authenticatedUser).build(); 
            } else {
                // Authentication Failed
                return Response.status(401).entity("Invalid username or password.").build();
            }

        } catch (SQLException e) {
            return Response.status(500).entity("Server authentication error.").build();
        }
    }

    // 1. POST: Create customer
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCustomer(Customer customer) {
        String checkSql = "SELECT COUNT(*) FROM customer_details WHERE username=? OR email=? OR aadhar_number=?";
        
        String insertSql = "INSERT INTO customer_details " +
                    "(customer_name, username, password, aadhar_number, permanent_address, state, country, city, email, phone_number, status, dob, age, gender, father_name, mother_name, mpin) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; 

        // --- 1. Validation Checks ---
        if (customer.getPhoneNumber() == null || !customer.getPhoneNumber().matches("\\d{10}")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid 10-digit phone number").build();
        }
        if (customer.getEmail() == null || !customer.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid email format").build();
        }
        if (customer.getMpin() == null || !customer.getMpin().matches("^\\d{4}$")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("MPIN must be exactly 4 digits").build();
        }

        try (Connection conn = DBConnection.getConnection()) {
            // 2. Duplicate Check
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, customer.getUsername());
                checkPs.setString(2, customer.getEmail());
                checkPs.setString(3, customer.getAadharNumber());
                ResultSet rsCheck = checkPs.executeQuery();
                if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                    LOGGER.warn("Attempt to create duplicate customer: {}", customer.getUsername());
                    return Response.status(500).entity("Duplicate username, email, or aadhar number exists.").build();
                }
            }

            // 3. Insert new record
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                ps.setString(i++, customer.getCustomerName());
                ps.setString(i++, customer.getUsername());
                ps.setString(i++, customer.getPassword());
                ps.setString(i++, customer.getAadharNumber());
                ps.setString(i++, customer.getPermanentAddress());
                ps.setString(i++, customer.getState());
                ps.setString(i++, customer.getCountry());
                ps.setString(i++, customer.getCity());
                ps.setString(i++, customer.getEmail());
                ps.setString(i++, customer.getPhoneNumber());
                ps.setString(i++, customer.getStatus());
                ps.setDate(i++, customer.getDob());
                ps.setInt(i++, customer.getAge()); 
                ps.setString(i++, customer.getGender());
                ps.setString(i++, customer.getFatherName());
                ps.setString(i++, customer.getMotherName());
                ps.setString(i++, customer.getMpin()); 

                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    customer.setCustomerId(rs.getInt(1));
                }
                LOGGER.info("Customer created successfully with ID: {}", customer.getCustomerId());
                return Response.status(201).entity(customer).build();
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error during customer creation.", e);
            return Response.status(500).entity("Database Error: " + e.getMessage()).build();
        }
    }


    // 2. GET: Fetch customer by ID
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomer(@PathParam("id") int id) {
        String sql = "SELECT * FROM customer_details WHERE customer_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Customer c = new Customer();

                return Response.ok(c).build();
            } else {
                return Response.status(404).entity("Customer not found").build();
            }

        } catch (Exception e) {
            LOGGER.error("Error fetching customer ID {}.", id, e);
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        }
    }

    // 3. PUT: Update customer
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCustomer(@PathParam("id") int id, Customer customer) {
        String sql = "UPDATE customer_details SET customer_name=?, username=?, password=?, aadhar_number=?, permanent_address=?, state=?, country=?, city=?, email=?, phone_number=?, status=?, dob=?, age=?, gender=?, father_name=?, mother_name=?, mpin=? WHERE customer_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, customer.getCustomerName());
            ps.setString(i++, customer.getUsername());
            ps.setString(i++, customer.getPassword());
            ps.setString(i++, customer.getAadharNumber());
            ps.setString(i++, customer.getPermanentAddress());
            ps.setString(i++, customer.getState());
            ps.setString(i++, customer.getCountry());
            ps.setString(i++, customer.getCity());
            ps.setString(i++, customer.getEmail());
            ps.setString(i++, customer.getPhoneNumber());
            ps.setString(i++, customer.getStatus());
            ps.setDate(i++, customer.getDob());
            ps.setInt(i++, customer.getAge());
            ps.setString(i++, customer.getGender());
            ps.setString(i++, customer.getFatherName());
            ps.setString(i++, customer.getMotherName());
            ps.setString(i++, customer.getMpin());
            ps.setInt(i++, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                LOGGER.info("Customer ID {} updated successfully.", id);
                return Response.ok("Customer updated").build();
            } else {
                return Response.status(404).entity("Customer not found").build();
            }

        } catch (Exception e) {
            LOGGER.error("Error updating customer ID {}.", id, e);
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        }
    }
    
   
    // 4. DELETE: Delete customer
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteCustomer(@PathParam("id") int id) {
        String sql = "DELETE FROM customer_details WHERE customer_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                return Response.ok("Customer deleted").build();
            } else {
                return Response.status(404).entity("Customer not found").build();
            }


        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        }
    }

    
}