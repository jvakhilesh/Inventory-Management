package com.example.auth_service.dto.request;

import lombok.Data;

@Data
public class ResendOtpRequestDTO {
    private String email;
}