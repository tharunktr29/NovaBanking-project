package com.novabank.card.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.card.domain.OutboxStatus;
import com.novabank.card.repository.OutboxEventRepository;
import com.novabank.shared.events.BankingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, BankingEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxRepository, KafkaTemplate<String, BankingEvent> kafkaTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${CARD_OUTBOX_PUBLISH_DELAY_MS:5000}")
    @Transactional
    public void publishDueEvents() {
        var due = outboxRepository.findDueForUpdate(
                List.of(OutboxStatus.PENDING.name(), OutboxStatus.FAILED.name()),
                Instant.now(),
                25
        );
        for (var outbox : due) {
            try {
                var payload = new LinkedHashMap<>(objectMapper.readValue(outbox.getPayload(), PAYLOAD_TYPE));
                var customerId = UUID.fromString(String.valueOf(payload.remove("_customerId")));
                var event = BankingEvent.v1(outbox.getEventType(), outbox.getCorrelationId(), customerId, outbox.getAggregateId(), payload);
                kafkaTemplate.send(topic(outbox.getEventType()), customerId.toString(), event).get(10, TimeUnit.SECONDS);
                outbox.setStatus(OutboxStatus.PUBLISHED);
                outbox.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                outbox.setAttempts(outbox.getAttempts() + 1);
                outbox.setStatus(outbox.getAttempts() >= MAX_ATTEMPTS ? OutboxStatus.ABANDONED : OutboxStatus.FAILED);
                outbox.setNextAttemptAt(Instant.now().plus(backoff(outbox.getAttempts())));
                log.warn("Unable to publish card outbox event id={} type={} attempts={} status={}",
                        outbox.getId(), outbox.getEventType(), outbox.getAttempts(), outbox.getStatus());
            }
        }
    }

    private Duration backoff(int attempts) {
        return Duration.ofSeconds(Math.min(300, Math.max(5, attempts * attempts * 5L)));
    }

    private String topic(String eventType) {
        return switch (eventType) {
            case "CardCreated" -> "card.created";
            case "CardActivated" -> "card.activated";
            case "CardLocked" -> "card.locked";
            case "CardUnlocked" -> "card.unlocked";
            case "CardReplacementRequested" -> "card.replacement-requested";
            case "CardControlsUpdated" -> "card.controls-updated";
            default -> throw new IllegalArgumentException("Unsupported outbox event type " + eventType);
        };
    }
}
