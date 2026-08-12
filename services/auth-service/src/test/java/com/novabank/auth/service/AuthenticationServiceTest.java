package com.novabank.auth.service;

import com.novabank.auth.domain.LoginAttempt;
import com.novabank.auth.domain.UserCredential;
import com.novabank.auth.domain.UserRole;
import com.novabank.auth.exception.AuthException;
import com.novabank.auth.repository.LoginAttemptRepository;
import com.novabank.auth.repository.PasswordResetTokenRepository;
import com.novabank.auth.repository.UserCredentialRepository;
import com.novabank.auth.security.JwtService;
import com.novabank.auth.web.dto.ChangePasswordRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    UserCredentialRepository userRepository;

    @Mock
    LoginAttemptRepository loginAttemptRepository;

    @Mock
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    RefreshTokenService refreshTokenService;

    @Mock
    AuditService auditService;

    @Mock
    BankingEventPublisher eventPublisher;

    @Test
    void passwordChangeVerifiesCurrentPasswordAndRevokesRefreshTokens() {
        var user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NovaBankDemo!2026", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("NovaBankDemo!2027", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("NovaBankDemo!2027")).thenReturn("new-hash");

        service().changePassword(
                USER_ID,
                new ChangePasswordRequest("NovaBankDemo!2026", "NovaBankDemo!2027", "NovaBankDemo!2027"),
                "corr-5"
        );

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenService).revokeAllForUser(user);
        verify(auditService).record(USER_ID, "PASSWORD_CHANGED", "corr-5", "all refresh tokens revoked");
    }

    @Test
    void passwordChangeRejectsInvalidCurrentPassword() {
        var user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service().changePassword(
                USER_ID,
                new ChangePasswordRequest("wrong-password", "NovaBankDemo!2027", "NovaBankDemo!2027"),
                "corr-6"
        )).isInstanceOf(AuthException.class)
                .hasMessage("Current password is invalid");

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void customerRetrievesOnlyTheirLoginActivity() {
        var user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(loginAttemptRepository.findRecentByUsernameOrEmail(
                eq(List.of("demo.user", "demo.user@novabank.test")),
                any(Pageable.class)))
                .thenReturn(List.of(new LoginAttempt("demo.user", true, null, "192.168.10.50", "Mozilla/5.0 Windows Chrome")));

        var activity = service().loginActivity(USER_ID);

        assertThat(activity).hasSize(1);
        assertThat(activity.getFirst().eventType()).isEqualTo("LOGIN_SUCCESS");
        assertThat(activity.getFirst().maskedIpAddress()).isEqualTo("192.168.***.***");
        assertThat(activity.getFirst().deviceSummary()).isEqualTo("Chrome on Windows");
        var queryCaptor = ArgumentCaptor.forClass(List.class);
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(loginAttemptRepository).findRecentByUsernameOrEmail(queryCaptor.capture(), pageableCaptor.capture());
        assertThat(queryCaptor.getValue()).containsExactly("demo.user", "demo.user@novabank.test");
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
    }

    private AuthenticationService service() {
        return new AuthenticationService(
                userRepository,
                loginAttemptRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                auditService,
                eventPublisher
        );
    }

    private UserCredential user() {
        var user = new UserCredential();
        user.setId(USER_ID);
        user.setUsername("demo.user");
        user.setEmail("demo.user@novabank.test");
        user.setPasswordHash("old-hash");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }
}
