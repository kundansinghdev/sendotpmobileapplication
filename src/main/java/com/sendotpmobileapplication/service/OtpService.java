package com.sendotpmobileapplication.service;

import com.sendotpmobileapplication.config.TwilioConfig;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import com.twilio.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Service class to handle OTP logic
@Service
public class OtpService {
    private final TwilioConfig twilioConfig; // Twilio configuration
    private final Map<String, OtpData> otpMap = new HashMap<>(); // Map to store OTP data
    private final Map<String, Instant> blockedNumbers = new HashMap<>(); // Map to track blocked numbers

    private static final int OTP_EXPIRY_MINUTES = 5; // OTP expiration time
    private static final int MAX_ATTEMPTS = 3; // Max attempts for OTP verification
    private static final long BLOCK_DURATION_HOURS = 24; // Block duration for number after max attempts

    @Autowired
    public OtpService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    // Initializes Twilio SDK
    @PostConstruct
    public void init() {
        Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
    }

    // Method to send OTP to a mobile number
    public String sendOtpToPhone(String mobileNumber) {
        if (isNumberBlocked(mobileNumber)) {
            return "Number is blocked due to multiple incorrect attempts. Please try again after 24 hours.";
        }

        try {
            Verification verification = Verification.creator(
                    twilioConfig.getServiceSid(),
                    mobileNumber,
                    "sms"
            ).create();

            otpMap.put(mobileNumber, new OtpData(verification.getSid(), Instant.now(), 0));
            return "OTP sent successfully";
        } catch (ApiException e) {
            return "Error sending OTP. Please try again later.";
        } catch (Exception e) {
            return "Unexpected error occurred. Please try again later.";
        }
    }

    // Method to verify the provided OTP
    public String verifyOtp(String mobileNumber, String otp) {
        Optional<OtpData> otpDataOpt = Optional.ofNullable(otpMap.get(mobileNumber));

        if (!otpDataOpt.isPresent()) {
            return "OTP not found or expired";
        }

        if (isNumberBlocked(mobileNumber)) {
            return "Number is blocked due to multiple incorrect attempts. Please try again after 24 hours.";
        }

        OtpData otpData = otpDataOpt.get();

        if (Instant.now().isAfter(otpData.getTimestamp().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))) {
            otpMap.remove(mobileNumber);
            return "OTP expired. Please request a new one.";
        }

        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(twilioConfig.getServiceSid(), otp)
                .setTo(mobileNumber)
                .create();

            if ("approved".equals(verificationCheck.getStatus())) {
                otpMap.remove(mobileNumber);
                return "OTP verified successfully! You are now logged in.";
            } else {
                otpData.incrementAttempts();
                if (otpData.getAttempts() >= MAX_ATTEMPTS) {
                    blockNumber(mobileNumber);
                    otpMap.remove(mobileNumber);
                    return "Invalid OTP! Number is now blocked due to multiple incorrect attempts.";
                }
                return "Invalid OTP!";
            }
        } catch (ApiException e) {
            return "Error verifying OTP. Please try again later.";
        } catch (Exception e) {
            return "Unexpected error occurred. Please try again later.";
        }
    }

    // Checks if the number is blocked
    private boolean isNumberBlocked(String mobileNumber) {
        Instant blockedUntil = blockedNumbers.get(mobileNumber);
        if (blockedUntil != null) {
            if (Instant.now().isBefore(blockedUntil)) {
                return true; // Number is still blocked
            } else {
                blockedNumbers.remove(mobileNumber); // Unblock if duration has passed
            }
        }
        return false;
    }

    // Blocks a number for a specified duration
    private void blockNumber(String mobileNumber) {
        blockedNumbers.put(mobileNumber, Instant.now().plus(BLOCK_DURATION_HOURS, ChronoUnit.HOURS));
    }

    // Inner class to store OTP data
    private static class OtpData {
        private final String verificationSid; // SID of the OTP verification
        private final Instant timestamp; // Timestamp when OTP was generated
        private int attempts; // Number of attempts made

        public OtpData(String verificationSid, Instant timestamp, int attempts) {
            this.verificationSid = verificationSid;
            this.timestamp = timestamp;
            this.attempts = attempts;
        }

        public String getVerificationSid() {
            return verificationSid;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public int getAttempts() {
            return attempts;
        }

        public void incrementAttempts() {
            this.attempts++;
        }
    }
}
