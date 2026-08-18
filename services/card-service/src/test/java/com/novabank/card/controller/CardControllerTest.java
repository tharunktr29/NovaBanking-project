package com.novabank.card.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.card.config.SecurityConfig;
import com.novabank.card.domain.CardNetwork;
import com.novabank.card.domain.CardStatus;
import com.novabank.card.domain.CardType;
import com.novabank.card.dto.PageResponse;
import com.novabank.card.dto.request.ReplacementRequestDto;
import com.novabank.card.dto.response.CardControlsSummaryResponse;
import com.novabank.card.dto.response.CardResponse;
import com.novabank.card.dto.response.CreditSummaryResponse;
import com.novabank.card.security.JwtAuthenticationFilter;
import com.novabank.card.security.JwtService;
import com.novabank.card.service.CardQueryParser;
import com.novabank.card.service.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CardQueryParser.class})
class CardControllerTest {
    private static final String CUSTOMER_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID CARD_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CardService cardService;

    @MockBean
    JwtService jwtService;

    @Test
    void unauthenticatedCardsRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidJwtReturns401() throws Exception {
        when(jwtService.parse("bad-token")).thenThrow(new IllegalArgumentException("invalid token"));

        mockMvc.perform(get("/api/cards").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = CUSTOMER_ID, roles = "CUSTOMER")
    void customerRetrievesMaskedCardList() throws Exception {
        when(cardService.list(any(), any(), any())).thenReturn(new PageResponse<>(List.of(sampleCard()), 0, 20, 1, 1, true, true, "createdAt,desc"));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cardType").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 9090"))
                .andExpect(content().string(not(containsString("4111111111111111"))))
                .andExpect(content().string(not(containsString("cardReference"))))
                .andExpect(content().string(not(containsString("customerId"))));
    }

    @Test
    void activationRequiresIdempotencyHeader() throws Exception {
        mockMvc.perform(post("/api/cards/{cardId}/activate", CARD_ID)
                        .with(user(CUSTOMER_ID).roles("CUSTOMER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_HEADER"));
    }

    @Test
    void replacementReasonIsValidated() throws Exception {
        mockMvc.perform(post("/api/cards/{cardId}/replacement-requests", CARD_ID)
                        .with(user(CUSTOMER_ID).roles("CUSTOMER"))
                        .header("Idempotency-Key", "valid-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReplacementRequestDto(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private CardResponse sampleCard() {
        return new CardResponse(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                CardType.CREDIT,
                CardNetwork.VISA,
                CardStatus.ACTIVE,
                "Demo User",
                "**** **** **** 9090",
                "9090",
                5,
                2030,
                Instant.parse("2026-08-11T12:00:00Z"),
                Instant.parse("2030-05-31T23:59:59Z"),
                new CardControlsSummaryResponse(true, true, false, false, new BigDecimal("2500.00"), BigDecimal.ZERO, "USD"),
                new CreditSummaryResponse(new BigDecimal("7500.00"), new BigDecimal("1250.35"), new BigDecimal("6249.65"), new BigDecimal("35.00"), LocalDate.of(2026, 9, 15), "USD", Instant.parse("2026-08-11T12:00:00Z")),
                null,
                null
        );
    }
}
