package com.example.bank.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event contract published to Kafka. eventId is unique per event.
 * eventType: DEPOSIT | WITHDRAWAL | TRANSFER
 */
public record TransactionEvent(
    String eventId,
    String eventType,
    Long transactionId,
    String fromAccount,
    String toAccount,
    BigDecimal amount,
    Instant occurredAt) {}
