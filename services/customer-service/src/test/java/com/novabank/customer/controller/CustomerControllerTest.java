package com.novabank.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.customer.config.SecurityConfig;
import com.novabank.customer.domain.KycStatus;
import com.novabank.customer.dto.request.UpdateCustomerProfileRequest;
import com.novabank.customer.dto.response.CustomerProfileResponse;
import com.novabank.customer.security.JwtAuthenticationFilter;
import com.novabank.customer.security.JwtService;
import com.novabank.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CustomerControllerTest {
    private static final String AUTH_USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CustomerService customerService;

    @MockBean
    JwtService jwtService;

    @Test
    void unauthenticatedProfileRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidJwtReturns401() throws Exception {
        when(jwtService.parse("bad-token")).thenThrow(new IllegalArgumentException("invalid token"));

        mockMvc.perform(get("/api/customers/me").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = AUTH_USER_ID, roles = "CUSTOMER")
    void authenticatedCustomerRetrievesProfile() throws Exception {
        when(customerService.getProfile(UUID.fromString(AUTH_USER_ID))).thenReturn(profileResponse());

        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.kycStatus").value("VERIFIED"));
    }

    @Test
    @WithMockUser(username = AUTH_USER_ID, roles = "CUSTOMER")
    void invalidEmailIsRejected() throws Exception {
        var request = new UpdateCustomerProfileRequest(
                "Demo",
                "Customer",
                "not-an-email",
                "+1 555 010 2000",
                "100 Fictional Avenue",
                "",
                "Indianapolis",
                "IN",
                "46204",
                "USA"
        );

        mockMvc.perform(patch("/api/customers/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")));
    }

    private CustomerProfileResponse profileResponse() {
        var now = Instant.parse("2026-08-11T12:00:00Z");
        return new CustomerProfileResponse(
                CUSTOMER_ID,
                UUID.fromString(AUTH_USER_ID),
                "Demo",
                "Customer",
                "demo.user@novabank.test",
                "+1 555 010 2000",
                "100 Fictional Avenue",
                "Suite 21",
                "Indianapolis",
                "IN",
                "46204",
                "USA",
                KycStatus.VERIFIED,
                now,
                now,
                0L
        );
    }
}
