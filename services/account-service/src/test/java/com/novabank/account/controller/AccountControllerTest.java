package com.novabank.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.account.config.SecurityConfig;
import com.novabank.account.domain.AccountStatus;
import com.novabank.account.domain.AccountType;
import com.novabank.account.dto.request.UpdateAccountNicknameRequest;
import com.novabank.account.dto.response.AccountBalanceResponse;
import com.novabank.account.dto.response.AccountResponse;
import com.novabank.account.security.JwtAuthenticationFilter;
import com.novabank.account.security.JwtService;
import com.novabank.account.service.AccountService;
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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AccountControllerTest {
    private static final String CUSTOMER_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID ACCOUNT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AccountService accountService;

    @MockBean
    JwtService jwtService;

    @Test
    void unauthenticatedAccountsRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidJwtReturns401() throws Exception {
        when(jwtService.parse("bad-token")).thenThrow(new IllegalArgumentException("invalid token"));

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = CUSTOMER_ID, roles = "CUSTOMER")
    void customerRetrievesCheckingAndSavingsAccounts() throws Exception {
        when(accountService.getAccounts(UUID.fromString(CUSTOMER_ID))).thenReturn(List.of(accountResponse(AccountType.CHECKING), accountResponse(AccountType.SAVINGS)));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountType").value("CHECKING"))
                .andExpect(jsonPath("$[0].balance.currentBalance").value(4286.42))
                .andExpect(content().string(not(containsString("123456789"))));
    }

    @Test
    void invalidNicknameIsRejected() throws Exception {
        mockMvc.perform(patch("/api/accounts/{accountId}/nickname", ACCOUNT_ID)
                        .with(user(CUSTOMER_ID).roles("CUSTOMER"))
                        .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateAccountNicknameRequest("x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private AccountResponse accountResponse(AccountType accountType) {
        var now = Instant.parse("2026-08-11T12:00:00Z");
        return new AccountResponse(
                accountType == AccountType.CHECKING ? ACCOUNT_ID : UUID.fromString("55555555-5555-5555-5555-555555555555"),
                UUID.fromString(CUSTOMER_ID),
                accountType,
                accountType == AccountType.CHECKING ? "**** 4821" : "**** 7710",
                accountType == AccountType.CHECKING ? "Daily Checking" : "Emergency Savings",
                AccountStatus.ACTIVE,
                "USD",
                LocalDate.of(2024, 1, 16),
                now,
                now,
                0L,
                new AccountBalanceResponse(
                        UUID.randomUUID(),
                        accountType == AccountType.CHECKING ? ACCOUNT_ID : UUID.fromString("55555555-5555-5555-5555-555555555555"),
                        new BigDecimal("4286.42"),
                        new BigDecimal("4036.42"),
                        new BigDecimal("250.00"),
                        BigDecimal.ZERO,
                        now,
                        0L
                )
        );
    }
}
