package com.example.auth_service.service;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GitHubUserService {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestClient.Builder restClientBuilder;

    public String getEmail(OAuth2AuthenticationToken authentication) {

        OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName()
                );

        if (authorizedClient == null) {
            throw new RuntimeException("GitHub OAuth client not found");
        }

        String accessToken =
                authorizedClient.getAccessToken().getTokenValue();

        List<Map<String, Object>> emails = restClientBuilder
                .baseUrl("https://api.github.com")
                .build()
                .get()
                .uri("/user/emails")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<
                        List<Map<String, Object>>>() {});

        if (emails == null || emails.isEmpty()) {
            throw new RuntimeException("No email found from GitHub");
        }

        for (Map<String, Object> email : emails) {

            Boolean primary = (Boolean) email.get("primary");
            Boolean verified = (Boolean) email.get("verified");

            if (Boolean.TRUE.equals(primary)
                    && Boolean.TRUE.equals(verified)) {

                return (String) email.get("email");
            }
        }

        throw new RuntimeException("No verified primary email found from GitHub");
    }
}