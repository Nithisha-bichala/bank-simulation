 package com.example.bank.controller;

import com.example.bank.model.Transaction;
import com.example.bank.service.TransactionService;
import com.example.bank.model.TransferRequest;
import com.example.bank.service.impl.TransactionServiceImpl;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.example.bank.exception.InsufficientFundsException; 
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Path("/transactions")
public class TransactionController {

    private TransactionService service = new TransactionServiceImpl();
   

    @POST
    @Path("/deposit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deposit(Transaction tx) {
        try {
            Transaction result = service.deposit(tx);
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/withdraw")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response withdraw(Transaction tx) {
        try {
            Transaction result = service.withdraw(tx);
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
    @GET
    @Path("/{accountNumber}")
    @Produces("text/csv") 
    public Response getTransactionsByAccount(@PathParam("accountNumber") String accountNumber) {
        try {
            List<Transaction> list = service.getTransactionsByAccountNumber(accountNumber);
            
           
            String csvData = generateCsv(list); 

            return Response.ok(csvData)
           
                .header("Content-Disposition", "attachment; filename=\"transactions_" + accountNumber + ".csv\"")
                .build();
                
        } catch (Exception e) {

            return Response.status(Response.Status.BAD_REQUEST).entity("Error generating report: " + e.getMessage()).build();
        }
    }

    private String safeString(String s) {
        if (s == null) return "";
        
        return s.replace("\t", " ").replace("\n", " ").replace("\"", "'").trim(); 
    }

    private String generateCsv(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return "Transaction ID,Date,UTR Number,Type,Amount,Final Balance,Description,Account ID\nNo transactions found.";
        }

        StringBuilder csv = new StringBuilder();
        String SEP = ","; 
        String NEWLINE = "\n";
        
        csv.append("sep=,").append(NEWLINE);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"); 

            csv.append("Transaction ID").append(SEP).append("Date").append(SEP)
           .append("UTR Number").append(SEP).append("Type").append(SEP)
           .append("Amount").append(SEP).append("Final Balance").append(SEP)
           .append("Description").append(SEP).append("Account ID").append(SEP)
           .append("Counterparty Account").append(SEP).append("Modified By").append(NEWLINE);

        for (Transaction tx : transactions) {
            
            
            String dateString = (tx.getDateOfTransaction() != null) ? 
                                 tx.getDateOfTransaction().format(formatter) : "N/A";
            
            List<String> fields = List.of(
                String.valueOf(tx.getTransactionId()),
                dateString,
                safeString(tx.getUtrNumber()),
                safeString(tx.getTransactionType()),
                String.format("%.2f", tx.getTransactionAmount()),
                String.format("%.2f", tx.getBalanceAmount()),
                safeString(tx.getDescription()),
                String.valueOf(tx.getAccountId()),
                safeString(tx.getReceiverBy()),
                safeString(tx.getModifiedBy())
            );
            
            csv.append(fields.stream().collect(Collectors.joining(SEP))).append(NEWLINE);
        }
        return csv.toString();
    }
 
 
    @POST
    @Path("/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transfer(TransferRequest req) {
        try {
            
            		service.transferFunds(
                            req.getSenderAccountNumber(),
                            req.getReceiverAccountNumber(),
                            req.getAmount(),
                            req.getModeOfTransaction(),
                            req.getDescription(),
                            req.getMpin() 
                        );
            return Response.ok("Transfer successful").build();
        } catch (Exception e) {
           
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
 

 

}

