package com.novabank.payment.mapper;

import com.novabank.payment.domain.ExternalPayee;
import com.novabank.payment.domain.PaymentOrder;
import com.novabank.payment.domain.PaymentStatusHistory;
import com.novabank.payment.dto.response.PayeeResponse;
import com.novabank.payment.dto.response.PaymentHistoryResponse;
import com.novabank.payment.dto.response.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponse toResponse(PaymentOrder order) {
        return new PaymentResponse(
                order.getId(),
                order.getPaymentReference(),
                order.getPaymentType(),
                order.getStatus(),
                order.getSourceAccountId(),
                order.getDestinationAccountId(),
                order.getDestinationCardId(),
                order.getExternalPayeeId(),
                order.getAmount(),
                order.getCurrency(),
                order.getMemo(),
                order.getExecutionType(),
                order.getScheduledFor(),
                order.getProcessingStartedAt(),
                order.getCompletedAt(),
                order.getFailedAt(),
                order.getFailureCode(),
                order.getFailureMessage(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public PaymentHistoryResponse toResponse(PaymentStatusHistory history) {
        return new PaymentHistoryResponse(
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getReasonCode(),
                history.getOccurredAt()
        );
    }

    public PayeeResponse toResponse(ExternalPayee payee) {
        return new PayeeResponse(
                payee.getId(),
                payee.getPayeeReference(),
                payee.getNickname(),
                payee.getBankName(),
                payee.getAccountType(),
                payee.getMaskedAccountNumber(),
                payee.getStatus(),
                payee.getCreatedAt(),
                payee.getUpdatedAt()
        );
    }
}
