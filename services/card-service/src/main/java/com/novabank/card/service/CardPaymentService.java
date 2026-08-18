package com.novabank.card.service;

import com.novabank.card.domain.CardPaymentApplication;
import com.novabank.card.domain.CardStatus;
import com.novabank.card.domain.CardType;
import com.novabank.card.dto.request.ApplyCreditCardPaymentRequest;
import com.novabank.card.dto.response.ApplyCreditCardPaymentResponse;
import com.novabank.card.exception.CardException;
import com.novabank.card.repository.BankCardRepository;
import com.novabank.card.repository.CardPaymentApplicationRepository;
import com.novabank.card.repository.CreditDetailsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CardPaymentService {
    private final BankCardRepository cardRepository;
    private final CreditDetailsRepository creditDetailsRepository;
    private final CardPaymentApplicationRepository applicationRepository;

    public CardPaymentService(
            BankCardRepository cardRepository,
            CreditDetailsRepository creditDetailsRepository,
            CardPaymentApplicationRepository applicationRepository
    ) {
        this.cardRepository = cardRepository;
        this.creditDetailsRepository = creditDetailsRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public ApplyCreditCardPaymentResponse apply(UUID customerId, ApplyCreditCardPaymentRequest request, String correlationId) {
        var existing = applicationRepository.findByBusinessOperationId(request.businessOperationId());
        if (existing.isPresent()) {
            var credit = creditDetailsRepository.findByCardId(existing.get().getCardId()).orElseThrow();
            return new ApplyCreditCardPaymentResponse(existing.get().getCardId(), existing.get().getBusinessOperationId(),
                    credit.getCurrentBalance(), credit.getAvailableCredit(), credit.getCurrency(), existing.get().getAppliedAt());
        }
        var card = cardRepository.findByIdAndCustomerId(request.cardId(), customerId)
                .orElseThrow(() -> new CardException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card was not found"));
        if (card.getCardType() != CardType.CREDIT || card.getStatus() != CardStatus.ACTIVE) {
            throw new CardException(HttpStatus.CONFLICT, "CARD_NOT_ELIGIBLE", "Credit card is not eligible for payment");
        }
        var credit = creditDetailsRepository.findByCardId(card.getId())
                .orElseThrow(() -> new CardException(HttpStatus.NOT_FOUND, "CREDIT_DETAILS_NOT_FOUND", "Credit details were not found"));
        if (!credit.getCurrency().equals(request.currency())) {
            throw new CardException(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH", "Payment currency must match the card currency");
        }
        if (request.amount().compareTo(credit.getCurrentBalance()) > 0) {
            throw new CardException(HttpStatus.CONFLICT, "CREDIT_CARD_OVERPAYMENT", "Payment cannot exceed the current card balance");
        }
        credit.setCurrentBalance(credit.getCurrentBalance().subtract(request.amount()));
        credit.setAvailableCredit(credit.getAvailableCredit().add(request.amount()));
        creditDetailsRepository.save(credit);

        var application = new CardPaymentApplication();
        application.setBusinessOperationId(request.businessOperationId());
        application.setCardId(card.getId());
        application.setAmount(request.amount());
        application.setCurrency(request.currency());
        application.setCorrelationId(parseCorrelation(correlationId));
        application = applicationRepository.save(application);
        return new ApplyCreditCardPaymentResponse(card.getId(), request.businessOperationId(), credit.getCurrentBalance(),
                credit.getAvailableCredit(), credit.getCurrency(), application.getAppliedAt());
    }

    private UUID parseCorrelation(String correlationId) {
        try {
            return UUID.fromString(correlationId);
        } catch (RuntimeException ex) {
            return UUID.randomUUID();
        }
    }
}
