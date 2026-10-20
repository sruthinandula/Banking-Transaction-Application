package com.example.processor.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * This service's own copy of the event contract
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionEvent(
    String eventId,
    String eventType,
    Long transactionId,
    String fromAccount,
    String toAccount,
    BigDecimal amount,
    Instant occurredAt) {

    /** A malformed payload can never succeed, so it is reported as IllegalArgumentException (non-retryable). */
    public static TransactionEvent parse(ObjectMapper objectMapper, String payload) {
        try {
            TransactionEvent event = objectMapper.readValue(payload, TransactionEvent.class);
            if (event.eventId() == null || event.eventType() == null || event.amount() == null) {
                throw new IllegalArgumentException("Event is missing required fields");
            }
            return event;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed transaction event", e);
        }
    }
}
