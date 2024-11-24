package com.sendotpmobileapplication.controller;

import com.sendotpmobileapplication.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing OTP-related operations.
 * Provides endpoints to send OTPs to users and verify them.
 */
@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    /**
     * Constructor to inject the OTP service.
     *
     * @param otpService the service responsible for OTP operations.
     */
    @Autowired
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Endpoint to send an OTP to the specified mobile number.
     *
     * @param request a map containing the mobile number (key: "mobileNumber").
     * @return a string indicating the status of the OTP sending process.
     */
    @PostMapping("/send")
    public String sendOtp(@RequestBody Map<String, String> request) {
        // Extract the mobile number from the request
        String mobileNumber = request.get("mobileNumber");

        // Delegate the request to the service layer and return the response
        return otpService.sendOtpToPhone(mobileNumber);
    }

    /**
     * Endpoint to verify a given OTP for a specified mobile number.
     *
     * @param request a map containing:
     *                - "mobileNumber": the user's mobile number.
     *                - "otp": the OTP to verify.
     * @return a string indicating whether the OTP verification was successful or not.
     */
    @PostMapping("/verify")
    public String verifyOtp(@RequestBody Map<String, String> request) {
        // Extract the mobile number and OTP from the request
        String mobileNumber = request.get("mobileNumber");
        String otp = request.get("otp");

        // Delegate the verification to the service layer and return the response
        return otpService.verifyOtp(mobileNumber, otp);
    }
}
