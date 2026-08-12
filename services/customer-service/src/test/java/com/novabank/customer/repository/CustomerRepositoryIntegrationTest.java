package com.novabank.customer.repository;

import com.novabank.customer.domain.KycStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryIntegrationTest {
    private static final UUID AUTH_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    CustomerProfileRepository profileRepository;

    @Autowired
    CustomerPreferenceRepository preferenceRepository;

    @Test
    void seededCustomerProfileIsFoundByAuthUserId() {
        var profile = profileRepository.findByAuthUserId(AUTH_USER_ID).orElseThrow();

        assertThat(profile.getId()).isEqualTo(CUSTOMER_ID);
        assertThat(profile.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(profile.getEmail()).isEqualTo("demo.user@novabank.test");
    }

    @Test
    void seededPreferencesAreFoundByCustomerId() {
        var preference = preferenceRepository.findByCustomerId(CUSTOMER_ID).orElseThrow();

        assertThat(preference.isSecurityAlerts()).isTrue();
        assertThat(preference.isPaperlessStatements()).isTrue();
    }
}
