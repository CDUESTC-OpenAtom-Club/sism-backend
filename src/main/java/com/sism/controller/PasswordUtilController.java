package com.sism.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary utility controller for password hash generation
 * REMOVE THIS IN PRODUCTION!
 */
@RestController
@RequestMapping("/util")
@RequiredArgsConstructor
public class PasswordUtilController {

    private final PasswordEncoder passwordEncoder;

    @PostMapping("/hash-password")
    public Map<String, Object> hashPassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String hash = passwordEncoder.encode(password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("matches", passwordEncoder.matches(password, hash));
        
        return response;
    }
    
    @PostMapping("/verify-password")
    public Map<String, Object> verifyPassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String hash = request.get("hash");
        
        Map<String, Object> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("matches", passwordEncoder.matches(password, hash));
        
        return response;
    }
}
