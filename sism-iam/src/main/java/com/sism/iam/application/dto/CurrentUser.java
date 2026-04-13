package com.sism.iam.application.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * CurrentUser - 用于 Spring Security 的当前用户信息
 * 配合 @AuthenticationPrincipal 使用，提供用户的基本信息
 */
public class CurrentUser implements UserDetails {

    private static final String PASSWORD_PLACEHOLDER = "[PROTECTED]";

    private final Long id;
    private final String username;
    private final String realName;
    private final String email;
    private final Long orgId;
    private final Collection<? extends GrantedAuthority> authorities;

    public CurrentUser(Long id, String username, String realName, String email, Long orgId,
                       Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.email = email;
        this.orgId = orgId;
        this.authorities = authorities;
    }

    public Long getId() {
        return id;
    }

    public String getRealName() {
        return realName;
    }

    public String getEmail() {
        return email;
    }

    public Long getOrgId() {
        return orgId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return PASSWORD_PLACEHOLDER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "CurrentUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", realName='" + realName + '\'' +
                ", email='" + email + '\'' +
                ", orgId=" + orgId +
                '}';
    }
}
