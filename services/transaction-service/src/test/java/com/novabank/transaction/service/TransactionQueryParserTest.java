package com.novabank.transaction.service;

import com.novabank.transaction.domain.TransactionCategory;
import com.novabank.transaction.domain.TransactionDirection;
import com.novabank.transaction.domain.TransactionStatus;
import com.novabank.transaction.exception.TransactionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionQueryParserTest {
    private final TransactionQueryParser parser = new TransactionQueryParser();

    @Test
    void parsesFiltersPaginationAndApprovedSort() {
        var query = parser.parse(
                UUID.randomUUID().toString(),
                "posted",
                "debit",
                null,
                "groceries",
                "Maple",
                "household",
                "2026-08-01",
                "2026-08-11",
                "10.00",
                "100.00",
                1,
                50,
                "amount,asc"
        );

        assertThat(query.status()).isEqualTo(TransactionStatus.POSTED);
        assertThat(query.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(query.category()).isEqualTo(TransactionCategory.GROCERIES);
        assertThat(query.dateFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(query.minAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(50);
        assertThat(query.sort()).isEqualTo("amount,asc");
    }

    @Test
    void defaultsPageSizeAndNewestSort() {
        var query = parser.parse(null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.sort()).isEqualTo("authorizedAt,desc");
    }

    @Test
    void rejectsUnsupportedSortAndInvalidRanges() {
        assertThatThrownBy(() -> parser.parse(null, null, null, null, null, null, null, null, null, null, null, null, null, "sourceReference,asc"))
                .isInstanceOf(TransactionException.class)
                .hasMessageContaining("Unsupported sort property");
        assertThatThrownBy(() -> parser.parse(null, null, null, null, null, null, null, "2026-08-11", "2026-08-01", null, null, null, null, null))
                .isInstanceOf(TransactionException.class)
                .hasMessageContaining("dateFrom cannot be after dateTo");
        assertThatThrownBy(() -> parser.parse(null, null, null, null, null, null, null, null, null, "100", "10", null, null, null))
                .isInstanceOf(TransactionException.class)
                .hasMessageContaining("minAmount cannot exceed maxAmount");
    }
}
