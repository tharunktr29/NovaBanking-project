package com.novabank.auth.web;

import com.novabank.auth.mapper.UserCredentialMapper;
import com.novabank.auth.repository.UserCredentialRepository;
import com.novabank.auth.service.AuthenticationService;
import com.novabank.auth.web.dto.*;
import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationService authenticationService;
    private final UserCredentialRepository userCredentialRepository;
    private final UserCredentialMapper userCredentialMapper;

    public AuthController(
            AuthenticationService authenticationService,
            UserCredentialRepository userCredentialRepository,
            UserCredentialMapper userCredentialMapper
    ) {
        this.authenticationService = authenticationService;
        this.userCredentialRepository = userCredentialRepository;
        this.userCredentialMapper = userCredentialMapper;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authenticationService.register(request, correlationId(servletRequest)));
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return authenticationService.login(
                request,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent"),
                correlationId(servletRequest)
        );
    }

    @PostMapping("/refresh")
    AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authenticationService.refresh(request);
    }

    @PostMapping("/logout")
    MessageResponse logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest servletRequest) {
        authenticationService.logout(request, correlationId(servletRequest));
        return new MessageResponse("Logout completed");
    }

    @PostMapping("/forgot-password")
    MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        authenticationService.forgotPassword(request, correlationId(servletRequest));
        return new MessageResponse("If an account exists for that email, reset instructions were sent.");
    }

    @PostMapping("/reset-password")
    MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest servletRequest) {
        authenticationService.resetPassword(request, correlationId(servletRequest));
        return new MessageResponse("Password reset completed");
    }

    @PostMapping("/mfa/verify")
    MessageResponse verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        authenticationService.verifyMockMfa(request);
        return new MessageResponse("MFA verification completed");
    }

    @PostMapping("/change-password")
    MessageResponse changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        authenticationService.changePassword(UUID.fromString(authentication.getName()), request, correlationId(servletRequest));
        return new MessageResponse("Password changed. Please sign in again.");
    }

    @GetMapping("/login-activity")
    java.util.List<LoginActivityResponse> loginActivity(Authentication authentication) {
        return authenticationService.loginActivity(UUID.fromString(authentication.getName()));
    }

    @GetMapping("/me")
    CurrentUserResponse me(Authentication authentication) {
        var userId = UUID.fromString(authentication.getName());
        return userRepositoryFind(userId);
    }

    private CurrentUserResponse userRepositoryFind(UUID userId) {
        return userCredentialRepository.findById(userId)
                .map(userCredentialMapper::toCurrentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String correlationId(HttpServletRequest request) {
        return request.getHeader(Correlation.HEADER_NAME);
    }
}
