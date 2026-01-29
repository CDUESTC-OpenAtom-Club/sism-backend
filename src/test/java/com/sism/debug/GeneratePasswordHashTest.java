package com.sism.debug;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
public class GeneratePasswordHashTest {
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void generatePasswordHash() {
        String password = "123456";
        String hash = passwordEncoder.encode(password);
        
        System.out.println("\n=== Password Hash Generation ===");
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("Hash length: " + hash.length());
        System.out.println("Verification: " + passwordEncoder.matches(password, hash));
        System.out.println("================================\n");
        
        // 测试现有的hash
        String existingHash = "$2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRGecsBe1vcHy";
        System.out.println("Testing existing hash ($2b$ prefix):");
        System.out.println("Hash: " + existingHash);
        System.out.println("Matches: " + passwordEncoder.matches(password, existingHash));
        System.out.println();
        
        String newHash = "$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi";
        System.out.println("Testing new hash ($2a$ prefix):");
        System.out.println("Hash: " + newHash);
        System.out.println("Matches: " + passwordEncoder.matches(password, newHash));
    }
}
