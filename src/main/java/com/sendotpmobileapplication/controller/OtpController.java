package com.sendotpmobileapplication.controller;

import com.sendotpmobileapplication.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// REST controller for handling OTP-related requests
@RestController
@RequestMapping("/api/otp")
public class OtpController {
    private final OtpService otpService;

    @Autowired
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    // Endpoint to send OTP
    @PostMapping("/send")
    public String sendOtp(@RequestBody Map<String, String> request) {
        String mobileNumber = request.get("mobileNumber");
        return otpService.sendOtpToPhone(mobileNumber);
    }

    // Endpoint to verify OTP
    @PostMapping("/verify")
    public String verifyOtp(@RequestBody Map<String, String> request) {
        String mobileNumber = request.get("mobileNumber");
        String otp = request.get("otp");
        return otpService.verifyOtp(mobileNumber, otp);
    }
}
