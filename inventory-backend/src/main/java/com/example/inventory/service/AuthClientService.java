
package com.example.inventory.service;

import java.util.Collections;
import java.util.List;

import com.example.inventory.dto.AuthUserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthClientService {

    private final RestClient authRestClient;

    public AuthUserDTO getCurrentUser(String token) {
        RestClient.RequestHeadersSpec<?> request =
                authRestClient.get().uri("/auth/me");

        request.header(
                HttpHeaders.COOKIE,
                "accessToken=" + token
        );

        return request.retrieve()
                .body(AuthUserDTO.class);
    }

    public List<String> getStockAlertRecipients() {
        try {
            return authRestClient.get()
                    .uri("/auth/internal/stock-alert-recipients")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() {});
        } catch (Exception ex) {
            log.error("Failed to fetch alert recipients from Auth Service: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
