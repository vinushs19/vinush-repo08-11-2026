package com.college.resourcefinder.security;

import com.college.resourcefinder.entity.Role;
import com.college.resourcefinder.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security {@link UserDetails} adapter for the application's {@link User} entity.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        String roleName = user.getRole() != null ? user.getRole().toUpperCase() : Role.STUDENT.name();
        String authority = roleName.startsWith(Role.ROLE_PREFIX) ? roleName : Role.ROLE_PREFIX + roleName;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(authority));
    }

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getUserId();
    }

    public String getRole() {
        return user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
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
        return Boolean.TRUE.equals(user.getIsActive());
    }
}
