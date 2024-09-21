package com.sendotpmobileapplication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Configuration class to bind Twilio properties from application.properties
@Configuration
@ConfigurationProperties(prefix = "twilio")
public class TwilioConfig {
    private String accountSid; // Twilio Account SID
    private String authToken;  // Twilio Auth Token
    private String serviceSid; // Twilio Service SID for SMS

    // Getters and Setters
    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getServiceSid() {
        return serviceSid;
    }

    public void setServiceSid(String serviceSid) {
        this.serviceSid = serviceSid;
    }
}
