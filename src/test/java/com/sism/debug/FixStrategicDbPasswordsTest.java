package com.sism.debug;

import com.sism.entity.AppUser;
import com.sism.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fix passwords in strategic database (not sism_db)
 */
@SpringBootTest
@ActiveProfiles("dev")
public class FixStrategicDbPasswordsTest {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void fixAllUserPasswords() {
        System.out.println("=".repeat(80));
        System.out.println("Fixing ALL User Passwords in Strategic Database");
        System.out.println("=".repeat(80));
        
        String newPassword = "123456";
        String newPasswordHash = passwordEncoder.encode(newPassword);
        
        System.out.println("\nNew password: " + newPassword);
        System.out.println("New hash: " + newPasswordHash);
        System.out.println("Hash length: " + newPasswordHash.length());
        System.out.println();
        
        List<AppUser> allUsers = userRepository.findAll();
        int updatedCount = 0;
        
        for (AppUser user : allUsers) {
            boolean needsUpdate = !passwordEncoder.matches(newPassword, user.getPasswordHash());
            
            if (needsUpdate) {
                System.out.println("Updating: " + user.getUsername() + " (" + user.getRealName() + ")");
                user.setPasswordHash(newPasswordHash);
                userRepository.save(user);
                updatedCount++;
            } else {
                System.out.println("OK: " + user.getUsername());
            }
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Total users updated: " + updatedCount);
        System.out.println("=".repeat(80));
    }
}
