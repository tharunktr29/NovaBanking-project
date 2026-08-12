package com.novabank.auth.service;

import com.novabank.shared.events.BankingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class BankingEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(BankingEventPublisher.class);
    private final KafkaTemplate<String, BankingEvent> kafkaTemplate;

    public BankingEventPublisher(KafkaTemplate<String, BankingEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, BankingEvent event) {
        kafkaTemplate.send(topic, event.customerId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish event type={} topic={} correlationId={}",
                                event.eventType(), topic, event.correlationId());
                    }
                });
    }
}
