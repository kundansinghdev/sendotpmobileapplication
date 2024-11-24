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

/**
 * Service class to handle OTP generation, sending, and verification logic.
 * Integrates with Twilio for sending and verifying OTPs via SMS.
 */
@Service
public class OtpService {
    private final TwilioConfig twilioConfig; // Configuration for Twilio integration
    private final Map<String, OtpData> otpMap = new HashMap<>(); // Store OTP data with mobile numbers
    private final Map<String, Instant> blockedNumbers = new HashMap<>(); // Store blocked numbers with timestamps

    // Constants for OTP expiry, retry attempts, and block duration
    private static final int OTP_EXPIRY_MINUTES = 5; 
    private static final int MAX_ATTEMPTS = 3; 
    private static final long BLOCK_DURATION_HOURS = 24; 

    /**
     * Constructor to inject Twilio configuration.
     *
     * @param twilioConfig Twilio configuration properties
     */
    @Autowired
    public OtpService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    /**
     * Initialize the Twilio SDK with account credentials.
     */
    @PostConstruct
    public void init() {
        Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
    }

    /**
     * Sends an OTP to the specified mobile number using Twilio.
     *
     * @param mobileNumber the mobile number to send the OTP
     * @return a message indicating the result of the OTP sending process
     */
    public String sendOtpToPhone(String mobileNumber) {
        if (isNumberBlocked(mobileNumber)) {
            return "Number is blocked due to multiple incorrect attempts. Please try again after 24 hours.";
        }

        try {
            // Request Twilio to send OTP via SMS
            Verification verification = Verification.creator(
                    twilioConfig.getServiceSid(),
                    mobileNumber,
                    "sms"
            ).create();

            // Store OTP details in the map
            otpMap.put(mobileNumber, new OtpData(verification.getSid(), Instant.now(), 0));
            return "OTP sent successfully";
        } catch (ApiException e) {
            return "Error sending OTP. Please try again later.";
        } catch (Exception e) {
            return "Unexpected error occurred. Please try again later.";
        }
    }

    /**
     * Verifies the provided OTP against the stored data.
     *
     * @param mobileNumber the mobile number associated with the OTP
     * @param otp          the OTP entered by the user
     * @return a message indicating whether the verification was successful or not
     */
    public String verifyOtp(String mobileNumber, String otp) {
        Optional<OtpData> otpDataOpt = Optional.ofNullable(otpMap.get(mobileNumber));

        if (!otpDataOpt.isPresent()) {
            return "OTP not found or expired";
        }

        if (isNumberBlocked(mobileNumber)) {
            return "Number is blocked due to multiple incorrect attempts. Please try again after 24 hours.";
        }

        OtpData otpData = otpDataOpt.get();

        // Check if the OTP is expired
        if (Instant.now().isAfter(otpData.getTimestamp().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))) {
            otpMap.remove(mobileNumber);
            return "OTP expired. Please request a new one.";
        }

        try {
            // Verify OTP using Twilio
            VerificationCheck verificationCheck = VerificationCheck.creator(twilioConfig.getServiceSid(), otp)
                    .setTo(mobileNumber)
                    .create();

            if ("approved".equals(verificationCheck.getStatus())) {
                otpMap.remove(mobileNumber); // Remove OTP data on successful verification
                return "OTP verified successfully! You are now logged in.";
            } else {
                otpData.incrementAttempts();
                if (otpData.getAttempts() >= MAX_ATTEMPTS) {
                    blockNumber(mobileNumber); // Block the number after exceeding retry limit
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

    /**
     * Checks if the given mobile number is currently blocked.
     *
     * @param mobileNumber the mobile number to check
     * @return true if the number is blocked, false otherwise
     */
    private boolean isNumberBlocked(String mobileNumber) {
        Instant blockedUntil = blockedNumbers.get(mobileNumber);
        if (blockedUntil != null) {
            if (Instant.now().isBefore(blockedUntil)) {
                return true; // The number is still within the blocked period
            } else {
                blockedNumbers.remove(mobileNumber); // Unblock if the block duration has passed
            }
        }
        return false;
    }

    /**
     * Blocks a mobile number for a specified duration.
     *
     * @param mobileNumber the mobile number to block
     */
    private void blockNumber(String mobileNumber) {
        blockedNumbers.put(mobileNumber, Instant.now().plus(BLOCK_DURATION_HOURS, ChronoUnit.HOURS));
    }

    /**
     * Inner class to store OTP-related data.
     */
    private static class OtpData {
        private final String verificationSid; // The SID of the Twilio verification
        private final Instant timestamp; // The time when the OTP was created
        private int attempts; // Number of verification attempts made

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
