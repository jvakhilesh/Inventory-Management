
package com.example.auth_service.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.example.auth_service.entity.enums.OtpChannel;

import lombok.Getter;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS = 3;

    private static final Duration RESEND_COOLDOWN =
            Duration.ofSeconds(60);

    private static final Duration RESEND_WINDOW =
            Duration.ofMinutes(15);

    private final SecureRandom secureRandom = new SecureRandom();

    private final ConcurrentHashMap<String, OtpData> otpStore =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ResendData> resendStore =
            new ConcurrentHashMap<>();

    // Used for the initial signup OTP.
    public String generateOtp(String email, OtpChannel channel) {
        String otp = createOtp();
        storeOtp(email, otp, channel);
        return otp;
    }

    // Generates an OTP without replacing the currently valid OTP.
    public String createOtp() {
        return String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );
    }

    // Call only after the email has been sent successfully.
    public void storeOtp(String email, String otp, OtpChannel channel) {
        String key = normalize(email);

        OtpData data = new OtpData(
                otp,
                Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L),
                channel,
                0
        );

        otpStore.put(key, data);
    }

    public boolean verifyOtp(String email, String submittedOtp) {
        String key = normalize(email);
        OtpData data = otpStore.get(key);

        if (data == null) {
            return false;
        }

        synchronized (data) {
            if (otpStore.get(key) != data) {
                return false;
            }

            if (!Instant.now().isBefore(data.getExpiresAt())) {
                otpStore.remove(key, data);
                return false;
            }

            if (data.getAttempts() >= MAX_ATTEMPTS) {
                otpStore.remove(key, data);
                return false;
            }

            data.incrementAttempts();

            if (submittedOtp == null ||
                    !data.getOtp().equals(submittedOtp.trim())) {
                return false;
            }

            return otpStore.remove(key, data);
        }
    }

    public OtpChannel getChannel(String email) {
        OtpData data = otpStore.get(normalize(email));

        if (data == null) {
            return null;
        }

        if (!Instant.now().isBefore(data.getExpiresAt())) {
            otpStore.remove(normalize(email), data);
            return null;
        }

        return data.getChannel();
    }

    public void removeOtp(String email) {
        otpStore.remove(normalize(email));
    }

    // Call after the initial signup email has been sent.
    // Starts the cooldown without consuming a resend.
    public void recordInitialSend(String email) {
        String key = normalize(email);
        Instant now = Instant.now();

        resendStore.put(key, new ResendData(now, now, 0));
    }

    // Atomically reserves one resend slot.
    public boolean reserveResend(String email) {
        String key = normalize(email);
        Instant now = Instant.now();
        AtomicBoolean reserved = new AtomicBoolean(false);

        resendStore.compute(key, (ignored, data) -> {

            if (data == null ||
                    Duration.between(data.getWindowStart(), now)
                            .compareTo(RESEND_WINDOW) >= 0) {

                reserved.set(true);
                return new ResendData(now, now, 1);
            }

            synchronized (data) {
                if (data.getCount() >= MAX_RESENDS) {
                    return data;
                }

                if (Duration.between(data.getLastSentAt(), now)
                        .compareTo(RESEND_COOLDOWN) < 0) {
                    return data;
                }

                data.increment(now);
                reserved.set(true);
                return data;
            }
        });

        return reserved.get();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Getter
    private static class OtpData {
        private final String otp;
        private final Instant expiresAt;
        private final OtpChannel channel;
        private int attempts;

        OtpData(
                String otp,
                Instant expiresAt,
                OtpChannel channel,
                int attempts) {
            this.otp = otp;
            this.expiresAt = expiresAt;
            this.channel = channel;
            this.attempts = attempts;
        }

        void incrementAttempts() {
            attempts++;
        }
    }

    @Getter
    private static class ResendData {
        private final Instant windowStart;
        private Instant lastSentAt;
        private int count;

        ResendData(
                Instant windowStart,
                Instant lastSentAt,
                int count) {
            this.windowStart = windowStart;
            this.lastSentAt = lastSentAt;
            this.count = count;
        }

        void increment(Instant now) {
            lastSentAt = now;
            count++;
        }
    }
}
