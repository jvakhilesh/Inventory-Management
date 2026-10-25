package com.example.auth_service.handlers;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.auth_service.entity.User;
import com.example.auth_service.entity.enums.Provider;
import com.example.auth_service.service.GitHubUserService;
import com.example.auth_service.service.JwtService;
import com.example.auth_service.service.UserServiceImp;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserServiceImp userServiceImp;
    private final JwtService jwtService;
    private final GitHubUserService gitHubUserService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication)throws IOException, ServletException {
        OAuth2AuthenticationToken oToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oToken.getPrincipal();
        String registrationId = oToken.getAuthorizedClientRegistrationId();
        String email;
        String name;
        Provider provider;
        switch (registrationId) {
            case "google":
                provider = Provider.GOOGLE;
                email = oAuth2User.getAttribute("email");
                name = oAuth2User.getAttribute("name");
                break;
            case "github":
                provider = Provider.GITHUB;
                email = gitHubUserService.getEmail(oToken);
                name = oAuth2User.getAttribute("name");
                if (name == null || name.isBlank()) {
                    name = oAuth2User.getAttribute("login");
                }
                break;
            case "facebook":
                provider = Provider.FACEBOOK;
                email = oAuth2User.getAttribute("email");
                name = oAuth2User.getAttribute("name");
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported OAuth provider: " + registrationId
                );
        }
        if (email == null || email.isBlank()) {
            throw new RuntimeException(
                    "Email not found from " + registrationId
            );
        }
        if (name == null || name.isBlank()) {
            name = email;
        }
        User user = userServiceImp.findByEmail(email);
        if (user == null) {
                User newUser = User.builder()
                        .name(name)
                        .email(email)
                        .provider(provider)
                        .isActive(true)
                        .build();
                user = userServiceImp.save(newUser);
        } else {
                user.setProvider(provider);
                user.setActive(true);
                user = userServiceImp.save(user);
        }
        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        // Access Token Cookie
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // Local development: HTTP
        accessCookie.setPath("/");
        // Refresh Token Cookie
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Local development: HTTP
        refreshCookie.setPath("/");
        // Add cookies to response
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
        // Redirect to frontend
        String frontEndUrl = "http://localhost:8081/";
        getRedirectStrategy().sendRedirect(request, response, frontEndUrl);
    }
}