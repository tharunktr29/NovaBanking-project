package com.novabank.card.mapper;

import com.novabank.card.domain.*;
import com.novabank.card.dto.internal.AccountDto;
import com.novabank.card.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CardMapper {
    public CardResponse toResponse(
            BankCard card,
            CardControls controls,
            Optional<CreditDetails> creditDetails,
            Optional<AccountDto> account
    ) {
        return new CardResponse(
                card.getId(),
                card.getAccountId(),
                card.getCardType(),
                card.getCardNetwork(),
                card.getStatus(),
                card.getCardholderName(),
                card.getMaskedCardNumber(),
                card.getLastFour(),
                card.getExpirationMonth(),
                card.getExpirationYear(),
                card.getActivatedAt(),
                card.getExpiresAt(),
                controls == null ? null : toControlsSummary(controls),
                creditDetails.map(this::toCreditSummary).orElse(null),
                card.getCardType() == CardType.DEBIT ? debitSummary(account) : null,
                account.map(value -> new LinkedAccountSummaryResponse(
                        value.id(),
                        value.nickname(),
                        value.accountType(),
                        value.status(),
                        false
                )).orElse(new LinkedAccountSummaryResponse(card.getAccountId(), null, null, null, true))
        );
    }

    public CardControlsSummaryResponse toControlsSummary(CardControls controls) {
        return new CardControlsSummaryResponse(
                controls.isOnlinePurchasesEnabled(),
                controls.isContactlessEnabled(),
                controls.isInternationalPurchasesEnabled(),
                controls.isAtmWithdrawalsEnabled(),
                controls.getDailyPurchaseLimit(),
                controls.getDailyAtmLimit(),
                controls.getCurrency()
        );
    }

    public CardControlsResponse toControlsResponse(CardControls controls) {
        return new CardControlsResponse(
                controls.getCardId(),
                controls.isOnlinePurchasesEnabled(),
                controls.isContactlessEnabled(),
                controls.isInternationalPurchasesEnabled(),
                controls.isAtmWithdrawalsEnabled(),
                controls.getDailyPurchaseLimit(),
                controls.getDailyAtmLimit(),
                controls.getCurrency(),
                controls.getUpdatedAt()
        );
    }

    public CreditSummaryResponse toCreditSummary(CreditDetails details) {
        return new CreditSummaryResponse(
                details.getCreditLimit(),
                details.getCurrentBalance(),
                details.getAvailableCredit(),
                details.getMinimumPaymentDue(),
                details.getPaymentDueDate(),
                details.getCurrency(),
                details.getUpdatedAt()
        );
    }

    public CardHistoryResponse toHistoryResponse(CardLifecycleHistory history) {
        return new CardHistoryResponse(
                history.getAction(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getReasonCode(),
                history.getOccurredAt()
        );
    }

    private DebitBalanceSummaryResponse debitSummary(Optional<AccountDto> account) {
        return account
                .filter(value -> value.balance() != null)
                .map(value -> new DebitBalanceSummaryResponse(
                        value.balance().currentBalance(),
                        value.balance().availableBalance(),
                        value.balance().pendingDebitAmount(),
                        value.balance().pendingCreditAmount(),
                        value.currency(),
                        value.balance().asOf(),
                        false
                ))
                .orElse(new DebitBalanceSummaryResponse(null, null, null, null, null, null, true));
    }
}
