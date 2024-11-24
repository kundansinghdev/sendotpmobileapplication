package com.sendotpmobileapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Send OTP Mobile Application.
 * This is the entry point of the Spring Boot application.
 *
 * The `@SpringBootApplication` annotation enables:
 * - Auto-configuration of Spring Boot
 * - Component scanning for Spring-managed beans
 * - Additional Spring Boot configuration
 */
@SpringBootApplication
public class SendotpmobileapplicationApplication {

    /**
     * The main method that launches the Spring Boot application.
     *
     * @param args command-line arguments passed during application startup
     */
    public static void main(String[] args) {
        // Launch the application
        SpringApplication.run(SendotpmobileapplicationApplication.class, args);
    }
}
