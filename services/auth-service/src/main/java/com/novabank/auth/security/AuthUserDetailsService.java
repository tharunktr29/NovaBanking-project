package com.novabank.auth.security;

import com.novabank.auth.repository.UserCredentialRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {
    private final UserCredentialRepository userCredentialRepository;

    public AuthUserDetailsService(UserCredentialRepository userCredentialRepository) {
        this.userCredentialRepository = userCredentialRepository;
    }

    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) {
        var user = userCredentialRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .accountLocked(user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.Instant.now()))
                .disabled(!user.isEnabled())
                .build();
    }
}
