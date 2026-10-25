package com.example.auth_service.dto.request;

import com.example.auth_service.entity.enums.OtpChannel;
import lombok.Data;

@Data 
public class SignUpRequestDTO {
    private String name;
    private String email;
    private String password;
    private OtpChannel otpChannel;
    private String whatsappNumber;
}
