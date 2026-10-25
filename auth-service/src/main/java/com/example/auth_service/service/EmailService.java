package com.example.auth_service.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtp(String recipientEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Your Signup Verification OTP");
        message.setText(
                "Your OTP for account verification is: " + otp
                        + "\n\nThis OTP expires in 5 minutes."
                        + "\nIf you did not request this OTP, please ignore this email."
        );
        mailSender.send(message);
    }
}