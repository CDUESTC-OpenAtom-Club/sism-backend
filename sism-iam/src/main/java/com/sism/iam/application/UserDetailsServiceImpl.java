package com.sism.iam.application;

import com.sism.iam.application.service.ContactInfoPolicy;
import com.sism.shared.application.dto.CurrentUser;
import com.sism.iam.domain.user.User;
import com.sism.iam.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String account = ContactInfoPolicy.normalizeAccount(username);
        User user = findUserByAccount(account)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = userRepository.findRoleCodesByUserId(user.getId()).stream()
                .map(this::toAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getEmail(),
                user.getOrgId(),
                authorities
        );
    }

    private java.util.Optional<User> findUserByAccount(String account) {
        if (ContactInfoPolicy.looksLikeEmail(account)) {
            return userRepository.findByEmail(account);
        }
        if (ContactInfoPolicy.looksLikePhone(account)) {
            return userRepository.findByPhone(account);
        }
        return userRepository.findByUsername(account);
    }

    private SimpleGrantedAuthority toAuthority(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        return roleCode.startsWith("ROLE_")
                ? new SimpleGrantedAuthority(roleCode)
                : new SimpleGrantedAuthority("ROLE_" + roleCode);
    }
}
