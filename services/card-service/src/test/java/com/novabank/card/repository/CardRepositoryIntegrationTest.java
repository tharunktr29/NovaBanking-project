package com.novabank.card.repository;

import com.novabank.card.domain.CardStatus;
import com.novabank.card.domain.CardType;
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
class CardRepositoryIntegrationTest {
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("99999999-9999-9999-9999-999999999998");
    private static final UUID ACTIVE_DEBIT_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID CREDIT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    BankCardRepository cardRepository;

    @Autowired
    CreditDetailsRepository creditDetailsRepository;

    @Test
    void seedDataContainsDemoDebitAndCreditCards() {
        var cards = cardRepository.findAll();

        assertThat(cards).extracting("cardType").contains(CardType.DEBIT, CardType.CREDIT);
        assertThat(cards).allMatch(card -> card.getCustomerId().equals(CUSTOMER_ID));
        assertThat(cards).allMatch(card -> card.getMaskedCardNumber().startsWith("****"));
    }

    @Test
    void ownershipLookupDoesNotRevealAnotherCustomersCard() {
        assertThat(cardRepository.findByIdAndCustomerId(ACTIVE_DEBIT_ID, OTHER_CUSTOMER_ID)).isEmpty();
    }

    @Test
    void creditAvailableCreditInvariantIsSeededCorrectly() {
        var credit = creditDetailsRepository.findByCardId(CREDIT_ID).orElseThrow();
        assertThat(credit.getAvailableCredit()).isEqualByComparingTo(credit.getCreditLimit().subtract(credit.getCurrentBalance()));
        assertThat(cardRepository.findById(CREDIT_ID).orElseThrow().getStatus()).isEqualTo(CardStatus.ACTIVE);
    }
}
