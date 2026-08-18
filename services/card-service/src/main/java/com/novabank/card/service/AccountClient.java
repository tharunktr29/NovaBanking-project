package com.novabank.card.service;

import com.novabank.card.dto.internal.AccountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Component
public class AccountClient {
    private static final Logger log = LoggerFactory.getLogger(AccountClient.class);
    private final RestClient restClient;

    public AccountClient(RestClient accountRestClient) {
        this.restClient = accountRestClient;
    }

    public Optional<AccountDto> getAccount(UUID accountId, String authorizationHeader) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/accounts/{accountId}", accountId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader == null ? "" : authorizationHeader)
                    .retrieve()
                    .body(AccountDto.class));
        } catch (Exception ex) {
            log.warn("Unable to read linked account summary for card display");
            return Optional.empty();
        }
    }
}
