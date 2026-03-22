package com.sism.iam.application;

import com.sism.iam.application.dto.CurrentUser;
import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = userRepository.findRoleCodesByUserId(user.getId()).stream()
                .map(this::toAuthority)
                .collect(Collectors.toList());

        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                null, // email 字段在新版本中已移除，使用 null
                user.getOrgId(),
                authorities
        );
    }

    private SimpleGrantedAuthority toAuthority(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return new SimpleGrantedAuthority("ROLE_UNKNOWN");
        }
        return roleCode.startsWith("ROLE_")
                ? new SimpleGrantedAuthority(roleCode)
                : new SimpleGrantedAuthority("ROLE_" + roleCode);
    }
}
