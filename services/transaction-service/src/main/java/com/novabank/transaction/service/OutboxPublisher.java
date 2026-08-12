package com.novabank.transaction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.shared.events.BankingEvent;
import com.novabank.transaction.domain.OutboxStatus;
import com.novabank.transaction.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, BankingEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxRepository, KafkaTemplate<String, BankingEvent> kafkaTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${TRANSACTION_OUTBOX_PUBLISH_DELAY_MS:5000}")
    @Transactional
    public void publishDueEvents() {
        var due = outboxRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                Instant.now(),
                PageRequest.of(0, 25)
        );
        for (var outbox : due) {
            try {
                var payload = objectMapper.readValue(outbox.getPayload(), PAYLOAD_TYPE);
                var customerId = UUID.fromString(String.valueOf(payload.get("customerId")));
                var event = BankingEvent.v1(outbox.getEventType(), outbox.getCorrelationId(), customerId, outbox.getAggregateId(), payload);
                kafkaTemplate.send(topic(outbox.getEventType()), customerId.toString(), event).get(10, TimeUnit.SECONDS);
                outbox.setStatus(OutboxStatus.PUBLISHED);
                outbox.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                outbox.setStatus(OutboxStatus.FAILED);
                outbox.setAttempts(outbox.getAttempts() + 1);
                outbox.setNextAttemptAt(Instant.now().plus(backoff(outbox.getAttempts())));
                log.warn("Unable to publish transaction outbox event id={} type={} attempts={}",
                        outbox.getId(), outbox.getEventType(), outbox.getAttempts());
            }
        }
    }

    private Duration backoff(int attempts) {
        return Duration.ofSeconds(Math.min(300, Math.max(5, attempts * attempts * 5L)));
    }

    private String topic(String eventType) {
        return switch (eventType) {
            case "TransactionCreated" -> "transaction.created";
            case "TransactionPosted" -> "transaction.posted";
            default -> throw new IllegalArgumentException("Unsupported outbox event type " + eventType);
        };
    }
}
