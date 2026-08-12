package com.novabank.auth.web.dto;

import java.util.UUID;

public record CurrentUserResponse(UUID customerId, String username, String email, String role) {
}
