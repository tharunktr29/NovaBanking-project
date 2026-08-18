package com.novabank.card.service;

import com.novabank.card.domain.CardStatus;
import com.novabank.card.domain.CardType;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public record CardQuery(
        CardType cardType,
        CardStatus status,
        UUID accountId,
        int page,
        int size,
        Sort sort,
        String sortText
) {
}
