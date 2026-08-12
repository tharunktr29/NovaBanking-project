package com.novabank.transaction.service;

import com.novabank.transaction.domain.TransactionCategory;
import com.novabank.transaction.domain.TransactionDirection;
import com.novabank.transaction.domain.TransactionStatus;
import com.novabank.transaction.domain.TransactionType;
import com.novabank.transaction.dto.TransactionQuery;
import com.novabank.transaction.exception.TransactionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
public class TransactionQueryParser {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "authorizedAt", "authorizedAt",
            "postedAt", "postedAt",
            "amount", "amount",
            "merchant", "merchant.name",
            "status", "status"
    );

    public TransactionQuery parse(
            String accountId,
            String status,
            String direction,
            String type,
            String category,
            String merchant,
            String search,
            String dateFrom,
            String dateTo,
            String minAmount,
            String maxAmount,
            Integer page,
            Integer size,
            String sort
    ) {
        var parsedPage = page == null ? DEFAULT_PAGE : page;
        var parsedSize = size == null ? DEFAULT_SIZE : size;
        if (parsedPage < 0) {
            throw badRequest("page must be zero or greater");
        }
        if (parsedSize < 1 || parsedSize > MAX_SIZE) {
            throw badRequest("size must be between 1 and 100");
        }

        var parsedDateFrom = parseDate(dateFrom, "dateFrom");
        var parsedDateTo = parseDate(dateTo, "dateTo");
        if (parsedDateFrom != null && parsedDateTo != null && parsedDateFrom.isAfter(parsedDateTo)) {
            throw badRequest("dateFrom cannot be after dateTo");
        }

        var parsedMin = parseAmount(minAmount, "minAmount");
        var parsedMax = parseAmount(maxAmount, "maxAmount");
        if (parsedMin != null && parsedMax != null && parsedMin.compareTo(parsedMax) > 0) {
            throw badRequest("minAmount cannot exceed maxAmount");
        }

        var parsedSort = normalizeSort(sort);
        return new TransactionQuery(
                parseUuid(accountId, "accountId"),
                parseEnum(status, TransactionStatus.class, "status"),
                parseEnum(direction, TransactionDirection.class, "direction"),
                parseEnum(type, TransactionType.class, "type"),
                parseEnum(category, TransactionCategory.class, "category"),
                clean(merchant),
                clean(search),
                parsedDateFrom,
                parsedDateTo,
                parsedMin,
                parsedMax,
                parsedPage,
                parsedSize,
                parsedSort
        );
    }

    public String normalizeSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return "authorizedAt,desc";
        }
        var parts = sort.split(",");
        var property = parts[0].trim();
        if (!SORT_FIELDS.containsKey(property)) {
            throw badRequest("Unsupported sort property: " + property);
        }
        var direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw badRequest("Sort direction must be asc or desc");
        }
        return property + "," + direction;
    }

    public String toJpaSortProperty(String property) {
        return SORT_FIELDS.get(property);
    }

    private UUID parseUuid(String value, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw badRequest(field + " must be a UUID");
        }
    }

    private LocalDate parseDate(String value, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException ex) {
            throw badRequest(field + " must use ISO date format yyyy-MM-dd");
        }
    }

    private BigDecimal parseAmount(String value, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            var amount = new BigDecimal(value);
            if (amount.signum() < 0) {
                throw badRequest(field + " cannot be negative");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw badRequest(field + " must be numeric");
        }
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw badRequest("Unsupported " + field + ": " + value);
        }
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private TransactionException badRequest(String message) {
        return new TransactionException(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_QUERY", message);
    }
}
