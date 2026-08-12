package com.novabank.customer.mapper;

import com.novabank.customer.domain.CustomerPreference;
import com.novabank.customer.domain.CustomerProfile;
import com.novabank.customer.dto.request.UpdateCustomerPreferenceRequest;
import com.novabank.customer.dto.request.UpdateCustomerProfileRequest;
import com.novabank.customer.dto.response.CustomerPreferenceResponse;
import com.novabank.customer.dto.response.CustomerProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {
    public CustomerProfileResponse toProfileResponse(CustomerProfile profile) {
        return new CustomerProfileResponse(
                profile.getId(),
                profile.getAuthUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getAddressLine1(),
                profile.getAddressLine2(),
                profile.getCity(),
                profile.getState(),
                profile.getPostalCode(),
                profile.getCountry(),
                profile.getKycStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getVersion()
        );
    }

    public CustomerPreferenceResponse toPreferenceResponse(CustomerPreference preference) {
        return new CustomerPreferenceResponse(
                preference.getId(),
                preference.getCustomerId(),
                preference.isEmailAlerts(),
                preference.isSmsAlerts(),
                preference.isPushAlerts(),
                preference.isSecurityAlerts(),
                preference.isPaymentAlerts(),
                preference.isLowBalanceAlerts(),
                preference.isPaperlessStatements(),
                preference.getCreatedAt(),
                preference.getUpdatedAt(),
                preference.getVersion()
        );
    }

    public void updateProfile(CustomerProfile profile, UpdateCustomerProfileRequest request) {
        profile.setFirstName(clean(request.firstName()));
        profile.setLastName(clean(request.lastName()));
        profile.setEmail(clean(request.email()).toLowerCase());
        profile.setPhone(cleanNullable(request.phone()));
        profile.setAddressLine1(clean(request.addressLine1()));
        profile.setAddressLine2(cleanNullable(request.addressLine2()));
        profile.setCity(clean(request.city()));
        profile.setState(clean(request.state()).toUpperCase());
        profile.setPostalCode(clean(request.postalCode()));
        profile.setCountry(clean(request.country()));
    }

    public void updatePreference(CustomerPreference preference, UpdateCustomerPreferenceRequest request) {
        preference.setEmailAlerts(request.emailAlerts());
        preference.setSmsAlerts(request.smsAlerts());
        preference.setPushAlerts(request.pushAlerts());
        preference.setSecurityAlerts(request.securityAlerts());
        preference.setPaymentAlerts(request.paymentAlerts());
        preference.setLowBalanceAlerts(request.lowBalanceAlerts());
        preference.setPaperlessStatements(request.paperlessStatements());
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String cleanNullable(String value) {
        var cleaned = clean(value);
        return cleaned == null || cleaned.isBlank() ? null : cleaned;
    }
}
