package com.novabank.customer.controller;

import com.novabank.customer.dto.request.UpdateCustomerPreferenceRequest;
import com.novabank.customer.dto.request.UpdateCustomerProfileRequest;
import com.novabank.customer.dto.response.CustomerPreferenceResponse;
import com.novabank.customer.dto.response.CustomerProfileResponse;
import com.novabank.customer.service.CustomerService;
import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/me")
    CustomerProfileResponse me(Authentication authentication) {
        return customerService.getProfile(currentUserId(authentication));
    }

    @PatchMapping("/me")
    CustomerProfileResponse updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateCustomerProfileRequest request,
            HttpServletRequest servletRequest
    ) {
        return customerService.updateProfile(currentUserId(authentication), request, servletRequest.getHeader(Correlation.HEADER_NAME));
    }

    @GetMapping("/me/preferences")
    CustomerPreferenceResponse preferences(Authentication authentication) {
        return customerService.getPreferences(currentUserId(authentication));
    }

    @PatchMapping("/me/preferences")
    CustomerPreferenceResponse updatePreferences(
            Authentication authentication,
            @Valid @RequestBody UpdateCustomerPreferenceRequest request,
            HttpServletRequest servletRequest
    ) {
        return customerService.updatePreferences(currentUserId(authentication), request, servletRequest.getHeader(Correlation.HEADER_NAME));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
