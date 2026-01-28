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

@SpringBootTest
@ActiveProfiles("dev")
public class FixFunctionalPasswordsTest {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    public void fixFunctionalDeptPasswords() {
        System.out.println("=".repeat(80));
        System.out.println("Fixing Functional Department User Passwords");
        System.out.println("=".repeat(80));
        
        // 生成新的密码哈希
        String newPassword = "123456";
        String newPasswordHash = passwordEncoder.encode(newPassword);
        
        System.out.println("\nNew password hash generated:");
        System.out.println("Password: " + newPassword);
        System.out.println("Hash: " + newPasswordHash);
        System.out.println("Hash length: " + newPasswordHash.length());
        System.out.println("Verification: " + passwordEncoder.matches(newPassword, newPasswordHash));
        System.out.println();
        
        List<AppUser> allUsers = userRepository.findAll();
        int updatedCount = 0;
        
        for (AppUser user : allUsers) {
            if (user.getOrg() != null && 
                (user.getOrg().getOrgType().name().contains("FUNCTION") || 
                 user.getOrg().getOrgType().name().equals("FUNCTIONAL_DEPT"))) {
                
                // 检查密码是否需要更新
                boolean needsUpdate = !passwordEncoder.matches(newPassword, user.getPasswordHash());
                
                if (needsUpdate) {
                    System.out.println("Updating user: " + user.getUsername() + " (" + user.getRealName() + ")");
                    user.setPasswordHash(newPasswordHash);
                    userRepository.save(user);
                    updatedCount++;
                } else {
                    System.out.println("Skipping user: " + user.getUsername() + " (password already correct)");
                }
            }
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Password Fix Complete");
        System.out.println("Total users updated: " + updatedCount);
        System.out.println("=".repeat(80));
        
        // 验证更新
        System.out.println("\nVerifying updates...");
        allUsers = userRepository.findAll();
        int verifiedCount = 0;
        int failedCount = 0;
        
        for (AppUser user : allUsers) {
            if (user.getOrg() != null && 
                (user.getOrg().getOrgType().name().contains("FUNCTION") || 
                 user.getOrg().getOrgType().name().equals("FUNCTIONAL_DEPT"))) {
                
                boolean matches = passwordEncoder.matches(newPassword, user.getPasswordHash());
                if (matches) {
                    verifiedCount++;
                } else {
                    failedCount++;
                    System.out.println("FAILED: " + user.getUsername());
                }
            }
        }
        
        System.out.println("\nVerification Results:");
        System.out.println("Verified: " + verifiedCount);
        System.out.println("Failed: " + failedCount);
        
        if (failedCount == 0) {
            System.out.println("\n✓ All functional department passwords are now correct!");
        } else {
            System.out.println("\n✗ Some passwords still need fixing!");
        }
    }
}
