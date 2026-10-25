package com.example.auth_service.controller;

import java.util.Arrays;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth_service.dto.request.LoginRequestDTO;
import com.example.auth_service.dto.request.SignUpRequestDTO;
import com.example.auth_service.dto.response.LoginResponseDTO;
import com.example.auth_service.dto.response.ResponseDTO;
import com.example.auth_service.dto.response.UserResponseDTO;
import com.example.auth_service.service.AuthService;
import com.example.auth_service.dto.request.VerifyOtpRequestDTO;
import com.example.auth_service.dto.request.ResendOtpRequestDTO;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.Authentication;
import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO,HttpServletResponse response){
        ResponseDTO responseDTO = authService.login(loginRequestDTO);
        Cookie accessCookie = new Cookie("accessToken", responseDTO.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        Cookie refreshCookie = new Cookie("refreshToken", responseDTO.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(responseDTO.getId());
        return ResponseEntity.ok(loginResponseDTO);
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDTO> signUp(@RequestBody SignUpRequestDTO signUpDTO){
        UserResponseDTO user = authService.signup(signUpDTO);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponseDTO> verifyOtp(@RequestBody VerifyOtpRequestDTO request, HttpServletResponse response) {
        ResponseDTO responseDTO = authService.verifyOtp(
                request.getEmail(),
                request.getOtp()
        );
        Cookie accessCookie = new Cookie("accessToken", responseDTO.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        response.addCookie(accessCookie);
        Cookie refreshCookie = new Cookie("refreshToken", responseDTO.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        response.addCookie(refreshCookie);
        return ResponseEntity.ok(new LoginResponseDTO(responseDTO.getId())
        );
    }


    @PostMapping("/resend-otp")
    public ResponseEntity<String> resendOtp(
            @RequestBody ResendOtpRequestDTO request) {

        authService.resendOtp(request.getEmail());

        return ResponseEntity.ok(
                "If the account is eligible, a new verification OTP has been sent."
        );
    }


    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(csrfToken);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(HttpServletRequest request,HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new AuthenticationServiceException(
                    "Refresh token not found inside cookie"
            );
        }
        String refreshToken = Arrays.stream(cookies)
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() ->
                        new AuthenticationServiceException(
                                "Refresh token not found inside cookie"
                        ));
        ResponseDTO responseDTO = authService.refreshToken(refreshToken);
        Cookie accessCookie = new Cookie("accessToken", responseDTO.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        response.addCookie(accessCookie);
        return ResponseEntity.ok(
                new LoginResponseDTO(responseDTO.getId())
        );
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        // Clear cookies FIRST to ensure they're removed from the response
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", "")
                .httpOnly(false)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        ResponseCookie sessionCookie = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

        // Clear the security context after cookies are sent
        SecurityContextHolder.clearContext();

        // Note: We skip SecurityContextLogoutHandler to avoid the 302 redirect.
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Cookie c1 = new Cookie("accessToken", "");
        c1.setPath("/");
        c1.setMaxAge(0);
        c1.setHttpOnly(true);
        response.addCookie(c1);

        Cookie c2 = new Cookie("refreshToken", "");
        c2.setPath("/");
        c2.setMaxAge(0);
        c2.setHttpOnly(true);
        response.addCookie(c2);

        Cookie c3 = new Cookie("XSRF-TOKEN", "");
        c3.setPath("/");
        c3.setMaxAge(0);
        c3.setHttpOnly(false);
        response.addCookie(c3);

        Cookie c4 = new Cookie("JSESSIONID", "");
        c4.setPath("/");
        c4.setMaxAge(0);
        c4.setHttpOnly(true);
        response.addCookie(c4);

        return ResponseEntity.ok("Logout successful");
    }


    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        UserResponseDTO user = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/internal/stock-alert-recipients")
    public ResponseEntity<java.util.List<String>> getStockAlertRecipients() {
        java.util.Set<String> recipients = new java.util.LinkedHashSet<>();
        String adminEmail = authService.getAdminEmail();
        if (adminEmail != null && !adminEmail.isBlank()) {
            recipients.add(adminEmail.trim());
        }
        recipients.addAll(authService.getEmployeeEmails());
        return ResponseEntity.ok(new java.util.ArrayList<>(recipients));
    }
}
