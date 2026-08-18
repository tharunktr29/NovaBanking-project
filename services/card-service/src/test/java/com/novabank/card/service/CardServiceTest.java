package com.novabank.card.service;

import com.novabank.card.domain.*;
import com.novabank.card.dto.request.UpdateCardControlsRequest;
import com.novabank.card.dto.response.CardResponse;
import com.novabank.card.exception.CardException;
import com.novabank.card.mapper.CardMapper;
import com.novabank.card.repository.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CardServiceTest {
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CARD_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private final BankCardRepository cardRepository = mock(BankCardRepository.class);
    private final CardControlsRepository controlsRepository = mock(CardControlsRepository.class);
    private final CreditDetailsRepository creditDetailsRepository = mock(CreditDetailsRepository.class);
    private final CardLifecycleHistoryRepository historyRepository = mock(CardLifecycleHistoryRepository.class);
    private final ReplacementRequestRepository replacementRepository = mock(ReplacementRequestRepository.class);
    private final AccountClient accountClient = mock(AccountClient.class);
    private final OutboxService outboxService = mock(OutboxService.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final CardService service = new CardService(
            cardRepository,
            controlsRepository,
            creditDetailsRepository,
            historyRepository,
            replacementRepository,
            new CardMapper(),
            accountClient,
            outboxService,
            idempotencyService
    );

    @Test
    void pendingCardCanBeActivated() {
        var card = card(CardStatus.PENDING_ACTIVATION);
        when(cardRepository.findByIdAndCustomerId(CARD_ID, CUSTOMER_ID)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(BankCard.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(controlsRepository.findByCardId(CARD_ID)).thenReturn(Optional.of(controls()));
        when(creditDetailsRepository.findByCardId(CARD_ID)).thenReturn(Optional.empty());
        when(accountClient.getAccount(any(), any())).thenReturn(Optional.empty());
        when(idempotencyService.run(eq(CUSTOMER_ID), eq(CARD_ID), eq("ACTIVATE_CARD"), eq("key-12345"), isNull(), eq(CardResponse.class), any()))
                .thenAnswer(invocation -> invocation.<Supplier<CardResponse>>getArgument(6).get());

        var response = service.activate(CUSTOMER_ID, CARD_ID, "key-12345", "22222222-2222-2222-2222-222222222222", "Bearer token");

        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.getActivatedAt()).isNotNull();
        verify(historyRepository).save(any(CardLifecycleHistory.class));
        verify(outboxService).add(eq(card), eq(CardAction.ACTIVATED), eq(CardStatus.PENDING_ACTIVATION), anyString(), any(Instant.class));
    }

    @Test
    void invalidActivationTransitionIsRejected() {
        when(idempotencyService.run(eq(CUSTOMER_ID), eq(CARD_ID), eq("ACTIVATE_CARD"), eq("key-12345"), isNull(), eq(CardResponse.class), any()))
                .thenAnswer(invocation -> invocation.<Supplier<CardResponse>>getArgument(6).get());
        when(cardRepository.findByIdAndCustomerId(CARD_ID, CUSTOMER_ID)).thenReturn(Optional.of(card(CardStatus.ACTIVE)));

        assertThatThrownBy(() -> service.activate(CUSTOMER_ID, CARD_ID, "key-12345", "22222222-2222-2222-2222-222222222222", "Bearer token"))
                .isInstanceOf(CardException.class)
                .hasMessageContaining("not in a state");
    }

    @Test
    void anotherCustomersCardIsNotFound() {
        when(cardRepository.findByIdAndCustomerId(CARD_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(CUSTOMER_ID, CARD_ID, "Bearer token"))
                .isInstanceOf(CardException.class)
                .hasMessageContaining("Card was not found");
    }

    @Test
    void invalidAtmLimitIsRejectedWhenAtmWithdrawalsAreDisabled() {
        when(idempotencyService.run(eq(CUSTOMER_ID), eq(CARD_ID), eq("UPDATE_CARD_CONTROLS"), eq("key-12345"), any(), eq(com.novabank.card.dto.response.CardControlsResponse.class), any()))
                .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(6).get());
        when(cardRepository.findByIdAndCustomerId(CARD_ID, CUSTOMER_ID)).thenReturn(Optional.of(card(CardStatus.ACTIVE)));

        var request = new UpdateCardControlsRequest(true, true, false, false, new BigDecimal("100.00"), new BigDecimal("10.00"), "USD");

        assertThatThrownBy(() -> service.updateControls(CUSTOMER_ID, CARD_ID, request, "key-12345", "22222222-2222-2222-2222-222222222222"))
                .isInstanceOf(CardException.class)
                .hasMessageContaining("Daily ATM limit");
    }

    private BankCard card(CardStatus status) {
        var card = new BankCard();
        card.setId(CARD_ID);
        card.setCustomerId(CUSTOMER_ID);
        card.setAccountId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        card.setCardReference("CARD-TEST");
        card.setCardType(CardType.DEBIT);
        card.setCardNetwork(CardNetwork.VISA);
        card.setStatus(status);
        card.setCardholderName("Demo User");
        card.setLastFour("1881");
        card.setMaskedCardNumber("**** **** **** 1881");
        card.setExpirationMonth(11);
        card.setExpirationYear(2029);
        card.setExpiresAt(Instant.parse("2029-11-30T23:59:59Z"));
        return card;
    }

    private CardControls controls() {
        var controls = new CardControls();
        controls.setCardId(CARD_ID);
        controls.setOnlinePurchasesEnabled(true);
        controls.setContactlessEnabled(true);
        controls.setInternationalPurchasesEnabled(false);
        controls.setAtmWithdrawalsEnabled(false);
        controls.setDailyPurchaseLimit(new BigDecimal("500.00"));
        controls.setDailyAtmLimit(BigDecimal.ZERO);
        controls.setCurrency("USD");
        return controls;
    }
}
