package com.example.auth_service.dto.request;

import com.example.auth_service.entity.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequestDTO {

    @NotNull(message = "Role is required")
    private Role role;
}
