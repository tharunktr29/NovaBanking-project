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
            if (!userRepository.existsByUsernameIgnoreCase("demo.user")) {
                var user = new UserCredential();
                user.setId(DEMO_USER_ID);
                user.setUsername("demo.user");
                user.setEmail("demo.user@novabank.test");
                user.setPasswordHash(passwordEncoder.encode("NovaBankDemo!2026"));
                user.setRole(UserRole.CUSTOMER);
                userRepository.save(user);
            }
            seed(userRepository, passwordEncoder, "support.agent", "support.agent@novabank.test", UserRole.SUPPORT_AGENT, "22222222-2222-2222-2222-222222222222");
            seed(userRepository, passwordEncoder, "fraud.analyst", "fraud.analyst@novabank.test", UserRole.FRAUD_ANALYST, "33333333-3333-3333-3333-333333333333");
            seed(userRepository, passwordEncoder, "operations.admin", "operations.admin@novabank.test", UserRole.OPERATIONS_ADMIN, "44444444-4444-4444-4444-444444444444");
        };
    }

    private static void seed(UserCredentialRepository repository, PasswordEncoder encoder, String username, String email, UserRole role, String id) {
        if (repository.existsByUsernameIgnoreCase(username)) return;
        var user = new UserCredential(); user.setId(UUID.fromString(id)); user.setUsername(username); user.setEmail(email);
        user.setPasswordHash(encoder.encode("NovaBankDemo!2026")); user.setRole(role); repository.save(user);
    }
}
