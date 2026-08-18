package com.novabank.payment.controller;

import com.novabank.payment.config.SecurityConfig;
import com.novabank.payment.security.JwtAuthenticationFilter;
import com.novabank.payment.security.JwtService;
import com.novabank.payment.service.IdempotencyService;
import com.novabank.payment.service.PaymentQueryParser;
import com.novabank.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PaymentControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    PaymentService paymentService;
    @MockBean
    PaymentQueryParser queryParser;
    @MockBean
    IdempotencyService idempotencyService;
    @MockBean
    JwtService jwtService;

    @Test
    void unauthenticatedPaymentListReturns401() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
