package com.novabank.auth.config;

import com.novabank.auth.domain.UserCredential;
import com.novabank.auth.domain.UserRole;
import com.novabank.auth.repository.UserCredentialRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Configuration
public class DataSeeder {
    public static final UUID DEMO_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Bean
    CommandLineRunner seedDemoUser(UserCredentialRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByUsernameIgnoreCase("demo.user")) {
                return;
            }
            var user = new UserCredential();
            user.setId(DEMO_USER_ID);
            user.setUsername("demo.user");
            user.setEmail("demo.user@novabank.test");
            user.setPasswordHash(passwordEncoder.encode("NovaBankDemo!2026"));
            user.setRole(UserRole.CUSTOMER);
            userRepository.save(user);
        };
    }
}
