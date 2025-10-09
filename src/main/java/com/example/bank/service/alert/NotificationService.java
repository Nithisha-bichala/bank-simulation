package com.example.bank.service.alert;

import com.example.bank.model.Transaction;
import com.example.bank.model.Customer;
import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);
	
    private static final String HOST = "smtp.gmail.com"; 
    private static final String USERNAME = "banksimulator06@gmail.com";
    private static final String PASSWORD = "vhalzudirribvhep"; 
    private static final int PORT = 587; 

    private final Session session;

    public NotificationService() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); 
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", String.valueOf(PORT));
        
        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
    }

    private void sendEmail(String recipientEmail, String subject, String body) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME, "Bank Simulator"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject(subject);
            message.setContent(body, "text/html");

            Transport.send(message);
            LOGGER.info("Email notification sent successfully to: {}", recipientEmail);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
        	LOGGER.error("Failed to send email to {}. SMTP Error: {}", recipientEmail, e.getMessage(), e);
        }
    }

    //  For Deposits/Credits
    public void sendCreditNotification(Customer customer, Transaction tx) {
        String subject = "Account Credited: $" + String.format("%.2f", tx.getTransactionAmount());
        String body = String.format(
            "<h3>Transaction Successful: Funds Credited</h3>" +
            "<p>Dear %s,</p>" +
            "<p>Your account **%s** has been credited with **$%.2f**.</p>" +
            "<p>New Balance: $%.2f</p>" +
            "<p>UTR: %s</p>",
            customer.getCustomerName(),
            tx.getAccountNumber(),
            tx.getTransactionAmount(),
            tx.getBalanceAmount(),
            tx.getUtrNumber()
        );
        sendEmail(customer.getEmail(), subject, body);
    }
    
    //  For Withdrawals/Debits
    public void sendDebitNotification(Customer customer, Transaction tx) {
        String subject = "Account Debited: $" + String.format("%.2f", tx.getTransactionAmount());
        String body = String.format(
            "<h3>Transaction Successful: Funds Debited</h3>" +
            "<p>Dear %s,</p>" +
            "<p>Your account **%s** has been debited by **$%.2f** for **%s**.</p>" +
            "<p>New Balance: $%.2f</p>" +
            "<p>UTR: %s</p>",
            customer.getCustomerName(),
            tx.getAccountNumber(),
            tx.getTransactionAmount(),
            tx.getDescription(),
            tx.getBalanceAmount(),
            tx.getUtrNumber()
        );
        sendEmail(customer.getEmail(), subject, body);
    }
}