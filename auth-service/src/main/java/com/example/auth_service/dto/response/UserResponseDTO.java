package com.example.auth_service.dto.response;

import com.example.auth_service.entity.enums.Provider;
import com.example.auth_service.entity.enums.Role;
import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private Provider provider;
    private Role role;
}
