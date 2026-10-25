package com.example.auth_service.service;

import java.util.Locale;
import java.util.Optional;

import com.example.auth_service.entity.enums.OtpChannel;
import com.example.auth_service.entity.enums.Role;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.auth_service.dto.request.LoginRequestDTO;
import com.example.auth_service.dto.request.SignUpRequestDTO;
import com.example.auth_service.dto.response.ResponseDTO;
import com.example.auth_service.dto.response.UserResponseDTO;
import com.example.auth_service.entity.User;
import com.example.auth_service.entity.enums.Provider;
import com.example.auth_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserServiceImp userServiceImp;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${app.security.admin-email}")
    private String adminEmail;

    public ResponseDTO login(LoginRequestDTO loginDTO){
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
        );
        User user = (User) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new ResponseDTO(user.getId(),accessToken,refreshToken);
    }

    public UserResponseDTO signup(SignUpRequestDTO signUp){
        if (signUp.getOtpChannel() == null) {
            throw new IllegalArgumentException(
                    "Please select an OTP delivery channel"
            );
        }
        // WhatsApp delivery will be implemented separately.
        if (signUp.getOtpChannel() != OtpChannel.EMAIL) {
            throw new IllegalArgumentException(
                    "WhatsApp OTP is not available yet. Please select EMAIL."
            );
        }
        if (signUp.getEmail() == null ||
                signUp.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String email = signUp.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isPresent()){
            throw new BadCredentialsException("user already exists");
        }
        User toBeCreate = modelMapper.map(signUp, User.class);
        toBeCreate.setEmail(email);
        toBeCreate.setProvider(Provider.LOCAL);
        toBeCreate.setRole(Role.USER);
        toBeCreate.setPassword(passwordEncoder.encode(signUp.getPassword()));
        toBeCreate.setActive(false);
        userRepository.save(toBeCreate);
        String otp = otpService.generateOtp(
                email,
                OtpChannel.EMAIL
        );
        try {
            emailService.sendOtp(email, otp);
            otpService.recordInitialSend(email);
        } catch (MailException exception) {
            otpService.removeOtp(email);
            userRepository.delete(toBeCreate);

            throw new IllegalStateException(
                    "Unable to send verification email. Please try again."
            );
        }
        return modelMapper.map(toBeCreate,UserResponseDTO.class);
    }

    public ResponseDTO verifyOtp(String email, String submittedOtp) {
        if (email == null || email.isBlank() ||
                submittedOtp == null || submittedOtp.isBlank()) {
            throw new BadCredentialsException(
                    "Email and OTP are required"
            );
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or OTP"
                        )
                );
        if (user.isActive() || user.getProvider() != Provider.LOCAL) {
            throw new BadCredentialsException(
                    "This account cannot be verified through signup OTP"
            );
        }
        if (otpService.getChannel(normalizedEmail) != OtpChannel.EMAIL) {
            throw new BadCredentialsException(
                    "OTP is missing or expired"
            );
        }
        if (!otpService.verifyOtp(normalizedEmail, submittedOtp)) {
            throw new BadCredentialsException(
                    "Invalid or expired OTP"
            );
        }
        user.setActive(true);
        if (user.getEmail().equalsIgnoreCase(adminEmail.trim())) {
            user.setRole(Role.ADMIN);
        } else {
            user.setRole(Role.USER);
        }
        userRepository.save(user);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new ResponseDTO(
                user.getId(),
                accessToken,
                refreshToken
        );
    }



    public void resendOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or account"));

        if (user.isActive() || user.getProvider() != Provider.LOCAL) {
            throw new BadCredentialsException(
                    "OTP resend is not available for this account");
        }

        if (!otpService.reserveResend(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "Please wait before requesting another OTP, or try again later");
        }

        String newOtp = otpService.createOtp();

        try {
            emailService.sendOtp(normalizedEmail, newOtp);
        } catch (MailException exception) {
            // Keep the previous OTP valid if email delivery fails.
            throw new IllegalStateException(
                    "Unable to send verification email. Please try again.");
        }

        // Replace the previous OTP only after successful email sending.
        otpService.storeOtp(normalizedEmail, newOtp, OtpChannel.EMAIL);
    }




    public ResponseDTO refreshToken(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        Long userId = jwtService.getUserId(refreshToken);
        User user = userServiceImp.getUserById(userId);
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is not active");
        }
        String accessToken = jwtService.generateAccessToken(user);
        return new ResponseDTO(
                user.getId(),
                accessToken,
                refreshToken
        );
    }

    public UserResponseDTO getCurrentUser(Authentication authentication) {
        if (authentication == null ||
                !(authentication.getPrincipal() instanceof User)) {

            throw new BadCredentialsException("User is not authenticated");
        }
        User user = (User) authentication.getPrincipal();
        return modelMapper.map(user, UserResponseDTO.class);
    }

    public java.util.List<String> getEmployeeEmails() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE && u.isActive())
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
    }

    public String getAdminEmail() {
        return adminEmail;
    }
}
