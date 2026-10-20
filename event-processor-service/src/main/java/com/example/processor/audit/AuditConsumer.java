package com.example.processor.audit;

import com.example.processor.event.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group "audit-service": records every transaction event.
 */
@Component
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditConsumer(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "audit-service", concurrency = "3")
    public void consume(String payload) {
        TransactionEvent event = TransactionEvent.parse(objectMapper, payload);

        // Make handling idempotent.
        if (repository.existsByEventId(event.eventId())) {
            log.info("Audit: duplicate event {} ignored", event.eventId());
            return;
        }
        repository.save(new AuditLog(event.eventId(), event.eventType(), event.transactionId(),
            event.fromAccount(), event.toAccount(), event.amount(), payload));
        log.info("Audit: recorded {} event {} (tx {})", event.eventType(), event.eventId(), event.transactionId());
    }
}
