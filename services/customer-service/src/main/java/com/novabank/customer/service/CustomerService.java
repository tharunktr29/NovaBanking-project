package com.novabank.customer.service;

import com.novabank.customer.dto.request.UpdateCustomerPreferenceRequest;
import com.novabank.customer.dto.request.UpdateCustomerProfileRequest;
import com.novabank.customer.dto.response.CustomerPreferenceResponse;
import com.novabank.customer.dto.response.CustomerProfileResponse;
import com.novabank.customer.event.CustomerEventPublisher;
import com.novabank.customer.exception.CustomerException;
import com.novabank.customer.mapper.CustomerMapper;
import com.novabank.customer.repository.CustomerPreferenceRepository;
import com.novabank.customer.repository.CustomerProfileRepository;
import com.novabank.shared.events.BankingEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class CustomerService {
    private final CustomerProfileRepository profileRepository;
    private final CustomerPreferenceRepository preferenceRepository;
    private final CustomerMapper mapper;
    private final CustomerEventPublisher eventPublisher;

    public CustomerService(
            CustomerProfileRepository profileRepository,
            CustomerPreferenceRepository preferenceRepository,
            CustomerMapper mapper,
            CustomerEventPublisher eventPublisher
    ) {
        this.profileRepository = profileRepository;
        this.preferenceRepository = preferenceRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(UUID authUserId) {
        return mapper.toProfileResponse(profile(authUserId));
    }

    @Transactional
    public CustomerProfileResponse updateProfile(UUID authUserId, UpdateCustomerProfileRequest request, String correlationId) {
        var profile = profile(authUserId);
        mapper.updateProfile(profile, request);
        var saved = profileRepository.save(profile);
        eventPublisher.publish("customer.profile-updated", BankingEvent.v1(
                "CustomerProfileUpdated",
                correlationId,
                saved.getAuthUserId(),
                saved.getId(),
                Map.of("updatedFields", "profile")
        ));
        return mapper.toProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerPreferenceResponse getPreferences(UUID authUserId) {
        var profile = profile(authUserId);
        return mapper.toPreferenceResponse(preference(profile.getId()));
    }

    @Transactional
    public CustomerPreferenceResponse updatePreferences(UUID authUserId, UpdateCustomerPreferenceRequest request, String correlationId) {
        var profile = profile(authUserId);
        var preference = preference(profile.getId());
        mapper.updatePreference(preference, request);
        var saved = preferenceRepository.save(preference);
        eventPublisher.publish("customer.preferences-updated", BankingEvent.v1(
                "CustomerPreferencesUpdated",
                correlationId,
                profile.getAuthUserId(),
                profile.getId(),
                Map.of("paperlessStatements", saved.isPaperlessStatements())
        ));
        return mapper.toPreferenceResponse(saved);
    }

    private com.novabank.customer.domain.CustomerProfile profile(UUID authUserId) {
        return profileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new CustomerException(HttpStatus.NOT_FOUND, "CUSTOMER_PROFILE_NOT_FOUND", "Customer profile was not found"));
    }

    private com.novabank.customer.domain.CustomerPreference preference(UUID customerId) {
        return preferenceRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerException(HttpStatus.NOT_FOUND, "CUSTOMER_PREFERENCES_NOT_FOUND", "Customer preferences were not found"));
    }
}
