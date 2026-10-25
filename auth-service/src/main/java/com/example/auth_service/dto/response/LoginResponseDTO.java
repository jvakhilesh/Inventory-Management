package com.example.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder 
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor  
public class LoginResponseDTO {
    private Long id;
}
