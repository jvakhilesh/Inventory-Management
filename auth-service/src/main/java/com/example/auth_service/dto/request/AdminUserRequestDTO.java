package com.example.auth_service.dto.request;

import com.example.auth_service.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserRequestDTO {
    @NotBlank @Size(max = 50) private String name;
    @NotBlank @Email @Size(max = 50) private String email;
    @Size(min = 8, max = 100) private String password;
    @NotNull private Role role;

    public void setPassword(String password) {
        this.password = (password == null || password.isBlank()) ? null : password;
    }
}
