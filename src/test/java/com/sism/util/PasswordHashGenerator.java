package com.sism.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility to generate BCrypt password hashes
 * Run this to generate password hashes for test users
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hash for password "123456"
        String password = "123456";
        String hash = encoder.encode(password);
        
        System.out.println("=== BCrypt Password Hash Generator ===");
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("\nVerification test:");
        System.out.println("Matches: " + encoder.matches(password, hash));
        
        // Test with the existing hash from database
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        System.out.println("\nTesting existing hash from database:");
        System.out.println("Hash: " + existingHash);
        System.out.println("Matches '123456': " + encoder.matches(password, existingHash));
    }
}
