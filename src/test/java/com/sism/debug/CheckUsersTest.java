package com.sism.debug;

import com.sism.entity.AppUser;
import com.sism.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("dev")
public class CheckUsersTest {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @org.springframework.transaction.annotation.Transactional
    public void checkFunctionalDeptUsers() {
        System.out.println("=".repeat(80));
        System.out.println("Checking Functional Department Users");
        System.out.println("=".repeat(80));
        
        List<AppUser> allUsers = userRepository.findAll();
        
        for (AppUser user : allUsers) {
            if (user.getOrg() != null && 
                (user.getOrg().getOrgType().name().contains("FUNCTION") || 
                 user.getOrg().getOrgType().name().equals("FUNCTIONAL_DEPT"))) {
                
                System.out.println("\nUsername: " + user.getUsername());
                System.out.println("Real Name: " + user.getRealName());
                System.out.println("Org: " + user.getOrg().getOrgName());
                System.out.println("Org Type: " + user.getOrg().getOrgType());
                System.out.println("Is Active: " + user.getIsActive());
                System.out.println("Password Hash Length: " + user.getPasswordHash().length());
                System.out.println("Password Hash Prefix: " + user.getPasswordHash().substring(0, Math.min(20, user.getPasswordHash().length())));
                
                // Test password verification
                boolean matches123456 = passwordEncoder.matches("123456", user.getPasswordHash());
                System.out.println("Password '123456' matches: " + matches123456);
                
                System.out.println("-".repeat(80));
            }
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("BCrypt Test");
        System.out.println("=".repeat(80));
        String testPassword = "123456";
        String encoded = passwordEncoder.encode(testPassword);
        System.out.println("Test password: " + testPassword);
        System.out.println("Encoded: " + encoded);
        System.out.println("Encoded length: " + encoded.length());
        System.out.println("Verification: " + passwordEncoder.matches(testPassword, encoded));
    }
}
