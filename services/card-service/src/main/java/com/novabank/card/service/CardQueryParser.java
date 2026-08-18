package com.novabank.card.service;

import com.novabank.card.domain.CardStatus;
import com.novabank.card.domain.CardType;
import com.novabank.card.exception.CardException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class CardQueryParser {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "expiresAt", "cardType", "status");
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "createdAt", "createdAt",
            "expiresAt", "expiresAt",
            "cardType", "cardType",
            "status", "status"
    );

    public CardQuery parse(String cardType, String status, String accountId, Integer page, Integer size, String sort) {
        var parsedPage = page == null ? 0 : page;
        var parsedSize = size == null ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        if (parsedPage < 0 || parsedSize < 1) {
            throw new CardException(HttpStatus.BAD_REQUEST, "INVALID_PAGING", "Page must be zero or greater and size must be positive");
        }
        var sortText = StringUtils.hasText(sort) ? sort : "createdAt,desc";
        var springSort = parseSort(sortText);
        return new CardQuery(
                parseEnum(cardType, CardType.class, "cardType"),
                parseEnum(status, CardStatus.class, "status"),
                StringUtils.hasText(accountId) ? parseUuid(accountId, "accountId") : null,
                parsedPage,
                parsedSize,
                springSort,
                sortText
        );
    }

    private Sort parseSort(String sortText) {
        var parts = sortText.split(",");
        var property = parts[0].trim();
        if (!ALLOWED_SORTS.contains(property)) {
            throw new CardException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_SORT", "Unsupported card sort property");
        }
        var direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, SORT_PROPERTIES.get(property));
    }

    private UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException ex) {
            throw new CardException(HttpStatus.BAD_REQUEST, "INVALID_" + field.toUpperCase(), field + " must be a UUID");
        }
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> type, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new CardException(HttpStatus.BAD_REQUEST, "INVALID_" + field.toUpperCase(), field + " is not supported");
        }
    }
}
