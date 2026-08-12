package com.novabank.customer.event;

import com.novabank.shared.events.BankingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomerEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(CustomerEventPublisher.class);
    private final KafkaTemplate<String, BankingEvent> kafkaTemplate;

    public CustomerEventPublisher(KafkaTemplate<String, BankingEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, BankingEvent event) {
        kafkaTemplate.send(topic, event.customerId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Unable to publish customer event type={} topic={}", event.eventType(), topic, ex);
                    }
                });
    }
}
