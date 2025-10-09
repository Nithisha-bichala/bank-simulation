package com.example.bank.controller;

import com.example.bank.model.Customer;
import com.example.bank.db.DBConnection;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.*;

@Path("/customer")
public class CustomerController {

    // 1. POST: Create customer
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createCustomer(Customer customer) {
	    String checkSql = "SELECT COUNT(*) FROM customer_details WHERE username=? OR email=? OR aadhar_number=?";
	    String insertSql = "INSERT INTO customer_details " +
	            "(customer_name, username, password, aadhar_number, permanent_address, state, country, city, email, phone_number, status, dob, age, gender, father_name, mother_name) " +
	            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	    // Basic validations
	    if (customer.getPhoneNumber() == null || !customer.getPhoneNumber().matches("\\d{10}")) {
	        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid phone number").build();
	    }
	    if (customer.getEmail() == null || !customer.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
	        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid email").build();
	    }

	    try (Connection conn = DBConnection.getConnection()) {
	        //  Duplicate check
	        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
	            checkPs.setString(1, customer.getUsername());
	            checkPs.setString(2, customer.getEmail());
	            checkPs.setString(3, customer.getAadharNumber());
	            ResultSet rsCheck = checkPs.executeQuery();
	            if (rsCheck.next() && rsCheck.getInt(1) > 0) {
	              
	                return Response.status(500).entity("Duplicate customer").build();
	            }
	        }

	       
	        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
	            ps.setString(1, customer.getCustomerName());
	            ps.setString(2, customer.getUsername());
	            ps.setString(3, customer.getPassword());
	            ps.setString(4, customer.getAadharNumber());
	            ps.setString(5, customer.getPermanentAddress());
	            ps.setString(6, customer.getState());
	            ps.setString(7, customer.getCountry());
	            ps.setString(8, customer.getCity());
	            ps.setString(9, customer.getEmail());
	            ps.setString(10, customer.getPhoneNumber());
	            ps.setString(11, customer.getStatus());
	            ps.setDate(12, customer.getDob());
	            ps.setInt(13, customer.getAge());
	            ps.setString(14, customer.getGender());
	            ps.setString(15, customer.getFatherName());
	            ps.setString(16, customer.getMotherName());

	            ps.executeUpdate();
	            ResultSet rs = ps.getGeneratedKeys();
	            if (rs.next()) {
	                customer.setCustomerId(rs.getInt(1));
	            }
	            return Response.status(201).entity(customer).build();
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return Response.status(500).entity("Error: " + e.getMessage()).build();
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
                c.setCustomerId(rs.getInt("customer_id"));
                c.setCustomerName(rs.getString("customer_name"));
                c.setUsername(rs.getString("username"));
                c.setPassword(rs.getString("password"));
                c.setAadharNumber(rs.getString("aadhar_number"));
                c.setPermanentAddress(rs.getString("permanent_address"));
                c.setState(rs.getString("state"));
                c.setCountry(rs.getString("country"));
                c.setCity(rs.getString("city"));
                c.setEmail(rs.getString("email"));
                c.setPhoneNumber(rs.getString("phone_number"));
                c.setStatus(rs.getString("status"));
                c.setDob(rs.getDate("dob"));
                c.setAge(rs.getInt("age"));
                c.setGender(rs.getString("gender"));
                c.setFatherName(rs.getString("father_name"));
                c.setMotherName(rs.getString("mother_name"));

                return Response.ok(c).build();
            } else {
                return Response.status(404).entity("Customer not found").build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        }
    }

    // 3. PUT: Update customer
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCustomer(@PathParam("id") int id, Customer customer) {
        String sql = "UPDATE customer_details SET customer_name=?, username=?, password=?, aadhar_number=?, permanent_address=?, state=?, country=?, city=?, email=?, phone_number=?, status=?, dob=?, age=?, gender=?, father_name=?, mother_name=? WHERE customer_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customer.getCustomerName());
            ps.setString(2, customer.getUsername());
            ps.setString(3, customer.getPassword());
            ps.setString(4, customer.getAadharNumber());
            ps.setString(5, customer.getPermanentAddress());
            ps.setString(6, customer.getState());
            ps.setString(7, customer.getCountry());
            ps.setString(8, customer.getCity());
            ps.setString(9, customer.getEmail());
            ps.setString(10, customer.getPhoneNumber());
            ps.setString(11, customer.getStatus());
            ps.setDate(12, customer.getDob());
            ps.setInt(13, customer.getAge());
            ps.setString(14, customer.getGender());
            ps.setString(15, customer.getFatherName());
            ps.setString(16, customer.getMotherName());
            ps.setInt(17, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                return Response.ok("Customer updated").build();
            } else {
                return Response.status(404).entity("Customer not found").build();
            }

        } catch (Exception e) {
            e.printStackTrace();
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
