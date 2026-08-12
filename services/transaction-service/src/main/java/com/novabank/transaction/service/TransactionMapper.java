package com.novabank.transaction.service;

import com.novabank.transaction.domain.BankTransaction;
import com.novabank.transaction.domain.Merchant;
import com.novabank.transaction.dto.MerchantResponse;
import com.novabank.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public TransactionResponse toResponse(BankTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getStatus(),
                transaction.getDirection(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCurrency(),
                merchant(transaction.getMerchant()),
                transaction.getAuthorizedAt(),
                transaction.getPostedAt()
        );
    }

    private MerchantResponse merchant(Merchant merchant) {
        if (merchant == null) {
            return null;
        }
        return new MerchantResponse(
                merchant.getId(),
                merchant.getName(),
                merchant.getMerchantCategoryCode(),
                merchant.getCity(),
                merchant.getState(),
                merchant.getCountry()
        );
    }
}
