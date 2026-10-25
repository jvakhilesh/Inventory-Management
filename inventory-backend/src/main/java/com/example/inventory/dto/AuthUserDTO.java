
package com.example.inventory.dto;

public record AuthUserDTO(
        Long id,
        String name,
        String email,
        String provider,
        String role
) {
}
