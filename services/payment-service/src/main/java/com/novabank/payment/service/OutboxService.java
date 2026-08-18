package com.novabank.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.payment.domain.OutboxEvent;
import com.novabank.payment.domain.PaymentOrder;
import com.novabank.payment.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;

@Service
public class OutboxService {
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void add(PaymentOrder order, String eventType) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("_customerId", order.getCustomerId().toString());
        payload.put("paymentId", order.getId().toString());
        payload.put("paymentReference", order.getPaymentReference());
        payload.put("paymentType", order.getPaymentType().name());
        payload.put("status", order.getStatus().name());
        payload.put("sourceAccountId", order.getSourceAccountId().toString());
        payload.put("destinationAccountId", order.getDestinationAccountId() == null ? null : order.getDestinationAccountId().toString());
        payload.put("destinationCardId", order.getDestinationCardId() == null ? null : order.getDestinationCardId().toString());
        payload.put("externalPayeeId", order.getExternalPayeeId() == null ? null : order.getExternalPayeeId().toString());
        payload.put("amount", order.getAmount().toPlainString());
        payload.put("currency", order.getCurrency());
        payload.put("occurredAt", Instant.now().toString());

        var outbox = new OutboxEvent();
        outbox.setAggregateId(order.getId());
        outbox.setEventType(eventType);
        outbox.setCorrelationId(order.getCorrelationId().toString());
        try {
            outbox.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize payment event payload", ex);
        }
        outboxRepository.save(outbox);
    }
}
