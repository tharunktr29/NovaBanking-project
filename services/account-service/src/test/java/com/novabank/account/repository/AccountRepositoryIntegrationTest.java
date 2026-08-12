package com.novabank.account.repository;

import com.novabank.account.domain.AccountStatus;
import com.novabank.account.domain.AccountType;
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
class AccountRepositoryIntegrationTest {
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID CHECKING_ACCOUNT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountBalanceRepository balanceRepository;

    @Test
    void seededCustomerRetrievesCheckingAndSavingsAccounts() {
        var accounts = accountRepository.findByCustomerIdOrderByAccountTypeAscOpenedDateAsc(CUSTOMER_ID);

        assertThat(accounts).extracting("accountType").containsExactly(AccountType.CHECKING, AccountType.SAVINGS);
        assertThat(accounts).allMatch(account -> account.getStatus() == AccountStatus.ACTIVE);
        assertThat(accounts).allMatch(account -> account.getMaskedAccountNumber().startsWith("****"));
    }

    @Test
    void ownershipLookupDoesNotRevealAnotherCustomersAccount() {
        assertThat(accountRepository.findByIdAndCustomerId(CHECKING_ACCOUNT_ID, OTHER_CUSTOMER_ID)).isEmpty();
    }

    @Test
    void seededBalanceIsLoadedFromPostgres() {
        var balance = balanceRepository.findByAccountId(CHECKING_ACCOUNT_ID).orElseThrow();

        assertThat(balance.getCurrentBalance()).isEqualByComparingTo("4286.42");
        assertThat(balance.getAvailableBalance()).isEqualByComparingTo("4036.42");
        assertThat(balance.getPendingDebitAmount()).isEqualByComparingTo("250.00");
    }
}
