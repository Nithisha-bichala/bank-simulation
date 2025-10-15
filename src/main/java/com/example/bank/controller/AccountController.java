package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.db.DBConnection;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.*;

@Path("/account")
public class AccountController {

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createAccount(Account account) {
	    String sql = "INSERT INTO account_details " +
	            "(customer_id, account_type, bank_name, branch, balance, status, account_number, ifsc_code, name_on_account, phone_linked_with_bank, saving_amount) " +
	            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	    //  Basic validation
	    if (account.getBalance() < 0) {
	        return Response.status(Response.Status.BAD_REQUEST).entity("Balance cannot be negative").build();
	    }
	    if (account.getAccountNumber() == null || account.getAccountNumber().trim().isEmpty()) {
	        return Response.status(Response.Status.BAD_REQUEST).entity("Account number required").build();
	    }
	    if (!account.getAccountNumber().matches("^ACC\\d{5}$")) {
	        return Response.status(400).entity("Invalid account number format").build();
	    }


	    // Verify customer exists
	    String checkCustomerSql = "SELECT customer_id FROM customer_details WHERE customer_id = ?";
	    try (Connection conn = DBConnection.getConnection();
	         PreparedStatement checkPs = conn.prepareStatement(checkCustomerSql)) {
	        checkPs.setInt(1, account.getCustomerId());
	        ResultSet r = checkPs.executeQuery();
	        if (!r.next()) {
	            return Response.status(Response.Status.BAD_REQUEST).entity("Customer not found").build();
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return Response.status(500).entity("DB Error while checking customer: " + e.getMessage()).build();
	    }

	    //  Insert account
	    try (Connection conn = DBConnection.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

	        ps.setInt(1, account.getCustomerId());
	        ps.setString(2, account.getAccountType());
	        ps.setString(3, account.getBankName());
	        ps.setString(4, account.getBranch());
	        ps.setDouble(5, account.getBalance());
	        ps.setString(6, account.getStatus());
	        ps.setString(7, account.getAccountNumber());
	        ps.setString(8, account.getIfscCode());
	        ps.setString(9, account.getNameOnAccount());
	        ps.setString(10, account.getPhoneLinkedWithBank());
	        ps.setDouble(11, account.getSavingAmount());

	        ps.executeUpdate();

	        // Retrieve auto-generated account_id
	        ResultSet rs = ps.getGeneratedKeys();
	        if (rs.next()) {
	            account.setAccountId(rs.getInt(1));
	        }

	        return Response.status(201).entity(account).build();

	    } catch (SQLIntegrityConstraintViolationException e) {
	        return Response.status(500).entity("Account number already exists").build();
	    } 
	    
	    catch (SQLException e) {
	        e.printStackTrace();
	        return Response.status(500).entity("Error: " + e.getMessage()).build();
	    }
	}
   
	
	
    @GET
    @Path("/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccount(@PathParam("accountNumber") String accountNumber) {
        String sql = "SELECT * FROM account_details WHERE account_number = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Account account = new Account();
                account.setAccountId(rs.getInt("account_id"));
                account.setCustomerId(rs.getInt("customer_id"));
                account.setAccountType(rs.getString("account_type"));
                account.setBankName(rs.getString("bank_name"));
                account.setBranch(rs.getString("branch"));
                account.setBalance(rs.getDouble("balance"));
                account.setStatus(rs.getString("status"));
                account.setAccountNumber(rs.getString("account_number"));
                account.setIfscCode(rs.getString("ifsc_code"));
                account.setNameOnAccount(rs.getString("name_on_account"));
                account.setPhoneLinkedWithBank(rs.getString("phone_linked_with_bank"));
                account.setSavingAmount(rs.getDouble("saving_amount"));

                return Response.ok(account).build();
            } else {
                return Response.status(404).entity("Account not found").build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        }
    }

    // ------------------- 3. PUT: Update account -------------------
    @PUT
    @Path("/{accountNumber}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccount(@PathParam("accountNumber") String accountNumber, Account account) {
        String sql = "UPDATE account_details SET customer_id=?, account_type=?, bank_name=?, branch=?, balance=?, status=?, ifsc_code=?, name_on_account=?, phone_linked_with_bank=?, saving_amount=? " +
                "WHERE account_number=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, account.getCustomerId());
            ps.setString(2, account.getAccountType());
            ps.setString(3, account.getBankName());
            ps.setString(4, account.getBranch());
            ps.setDouble(5, account.getBalance());
            ps.setString(6, account.getStatus());
            ps.setString(7, account.getIfscCode());
            ps.setString(8, account.getNameOnAccount());
            ps.setString(9, account.getPhoneLinkedWithBank());
            ps.setDouble(10, account.getSavingAmount());
            ps.setString(11, accountNumber);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                return Response.ok("Account updated successfully").build();
            } else {
                return Response.status(404).entity("Account not found").build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        }
    }

    // ------------------- 4. DELETE: Delete account -------------------
    @DELETE
    @Path("/{accountNumber}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteAccount(@PathParam("accountNumber") String accountNumber) {
        String sql = "DELETE FROM account_details WHERE account_number=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);
            int rowsDeleted = ps.executeUpdate();

            if (rowsDeleted > 0) {
                return Response.ok("Account deleted successfully").build();
            } else {
                return Response.status(404).entity("Account not found").build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        }
    }
}
