package com.novabank.customer.service;

import com.novabank.customer.domain.CustomerPreference;
import com.novabank.customer.domain.CustomerProfile;
import com.novabank.customer.domain.KycStatus;
import com.novabank.customer.dto.request.UpdateCustomerPreferenceRequest;
import com.novabank.customer.dto.request.UpdateCustomerProfileRequest;
import com.novabank.customer.event.CustomerEventPublisher;
import com.novabank.customer.mapper.CustomerMapper;
import com.novabank.customer.repository.CustomerPreferenceRepository;
import com.novabank.customer.repository.CustomerProfileRepository;
import com.novabank.shared.events.BankingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    private static final UUID AUTH_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    CustomerProfileRepository profileRepository;

    @Mock
    CustomerPreferenceRepository preferenceRepository;

    @Mock
    CustomerEventPublisher eventPublisher;

    @Test
    void authenticatedCustomerRetrievesTheirProfile() {
        var service = service();
        when(profileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(profile()));

        var response = service.getProfile(AUTH_USER_ID);

        assertThat(response.id()).isEqualTo(CUSTOMER_ID);
        assertThat(response.authUserId()).isEqualTo(AUTH_USER_ID);
        assertThat(response.kycStatus()).isEqualTo(KycStatus.VERIFIED);
    }

    @Test
    void customerUpdatesContactInformationAndPublishesEvent() {
        var service = service();
        var profile = profile();
        when(profileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);

        var request = new UpdateCustomerProfileRequest(
                "  Dana  ",
                "  Parker ",
                "DANA.PARKER@NOVABANK.TEST ",
                "+1 555 010 3000",
                "200 Fictional Road",
                "",
                "Indianapolis",
                "IN",
                "46204",
                "USA"
        );

        var response = service.updateProfile(AUTH_USER_ID, request, "corr-1");

        assertThat(response.firstName()).isEqualTo("Dana");
        assertThat(response.email()).isEqualTo("dana.parker@novabank.test");
        assertThat(response.addressLine2()).isNull();
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("customer.profile-updated"), any(BankingEvent.class));
    }

    @Test
    void customerUpdatesPreferencesAndPublishesEvent() {
        var service = service();
        var profile = profile();
        var preference = preference();
        when(profileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(profile));
        when(preferenceRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(preference));
        when(preferenceRepository.save(preference)).thenReturn(preference);

        var request = new UpdateCustomerPreferenceRequest(true, true, false, true, false, true, false);

        var response = service.updatePreferences(AUTH_USER_ID, request, "corr-2");

        assertThat(response.smsAlerts()).isTrue();
        assertThat(response.pushAlerts()).isFalse();
        assertThat(response.paperlessStatements()).isFalse();
        var eventCaptor = ArgumentCaptor.forClass(BankingEvent.class);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("customer.preferences-updated"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().customerId()).isEqualTo(AUTH_USER_ID);
    }

    private CustomerService service() {
        return new CustomerService(profileRepository, preferenceRepository, new CustomerMapper(), eventPublisher);
    }

    private CustomerProfile profile() {
        var profile = new CustomerProfile();
        profile.setId(CUSTOMER_ID);
        profile.setAuthUserId(AUTH_USER_ID);
        profile.setFirstName("Demo");
        profile.setLastName("Customer");
        profile.setEmail("demo.user@novabank.test");
        profile.setPhone("+1 555 010 2000");
        profile.setAddressLine1("100 Fictional Avenue");
        profile.setAddressLine2("Suite 21");
        profile.setCity("Indianapolis");
        profile.setState("IN");
        profile.setPostalCode("46204");
        profile.setCountry("USA");
        profile.setKycStatus(KycStatus.VERIFIED);
        return profile;
    }

    private CustomerPreference preference() {
        var preference = new CustomerPreference();
        preference.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        preference.setCustomerId(CUSTOMER_ID);
        preference.setEmailAlerts(true);
        preference.setSmsAlerts(false);
        preference.setPushAlerts(true);
        preference.setSecurityAlerts(true);
        preference.setPaymentAlerts(true);
        preference.setLowBalanceAlerts(true);
        preference.setPaperlessStatements(true);
        return preference;
    }
}
