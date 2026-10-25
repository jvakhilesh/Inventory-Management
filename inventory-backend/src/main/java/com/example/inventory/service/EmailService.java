package com.example.inventory.service;

import com.example.inventory.entity.Product;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendLowStockAlert(String recipient, List<Product> products) {
        sendLowStockAlert(List.of(recipient), products);
    }

    public void sendLowStockAlert(List<String> recipients, List<Product> products) {
        if (recipients == null || recipients.isEmpty() || products == null || products.isEmpty()) return;
        StringBuilder body = new StringBuilder("Low-stock alert for the following products:\n\n");
        for (Product product : products) {
            int reorderQuantity = Math.max(0, 50 - product.getQuantity());
            body.append("Product: ").append(product.getProductName()).append('\n')
                    .append("Current quantity: ").append(product.getQuantity()).append('\n')
                    .append("Threshold: ").append(product.getThreshold()).append('\n')
                    .append("Suggested quantity to reach 50: ").append(reorderQuantity).append("\n\n");
        }
        String bodyText = body.toString();
        for (String recipient : recipients) {
            if (recipient == null || recipient.isBlank()) continue;
            try {
                sendText(recipient.trim(), "Inventory System - Low Stock Alert", bodyText);
                log.info("Low-stock alert email sent to {}", recipient.trim());
            } catch (Exception ex) {
                log.error("Failed to send low-stock alert to {}: {}", recipient, ex.getMessage());
            }
        }
    }

    public void sendReport(String recipient, byte[] csv, String fileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (fromEmail != null && !fromEmail.isBlank()) helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject("Daily Inventory Report");
            helper.setText("Attached is your latest inventory report.");
            helper.addAttachment(fileName, new ByteArrayResource(csv), "text/csv");
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            throw new IllegalStateException("Could not send inventory report email", ex);
        }
    }

    private void sendText(String recipient, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            if (fromEmail != null && !fromEmail.isBlank()) helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            throw new IllegalStateException("Could not send stock alert email", ex);
        }
    }
}
