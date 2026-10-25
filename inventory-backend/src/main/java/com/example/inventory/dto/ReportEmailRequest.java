package com.example.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ReportEmailRequest {
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Enter a valid email address")
    private String recipientEmail;

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
}
