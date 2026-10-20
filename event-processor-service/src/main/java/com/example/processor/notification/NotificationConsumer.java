package com.example.processor.notification;

import com.example.processor.event.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group "notification-service". "Sending" is simulated by logging and storing a notification row.
 * Throwing an exception here triggers the retry policy and, finally, the dead-letter topic (see KafkaConfig).
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;
    private final BigDecimal poisonAmount;

    public NotificationConsumer(NotificationRepository repository, ObjectMapper objectMapper,
                                @Value("${app.notification.poison-amount:666.66}") BigDecimal poisonAmount) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.poisonAmount = poisonAmount;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "notification-service", concurrency = "3")
    public void consume(String payload) {
        TransactionEvent event = TransactionEvent.parse(objectMapper, payload);

        if (event.amount().compareTo(poisonAmount) == 0) {
            // Stand-in for "the SMS/email provider is down".
            throw new IllegalStateException("Simulated notification provider failure for event " + event.eventId());
        }

        for (Notification notification : buildNotifications(event)) {
            if (repository.existsByEventIdAndAccountNumber(notification.getEventId(),
                    notification.getAccountNumber())) {
                log.info("Notification: duplicate for event {} / account {} ignored",
                    notification.getEventId(), notification.getAccountNumber());
                continue;
            }
            repository.save(notification);
            log.info("Notification -> account {}: {}", notification.getAccountNumber(), notification.getMessage());
        }
    }

    private List<Notification> buildNotifications(TransactionEvent e) {
        return switch (e.eventType()) {
            case "DEPOSIT" -> List.of(new Notification(e.eventId(), e.toAccount(),
                "Deposit of %s credited to account %s".formatted(e.amount(), e.toAccount())));
            case "WITHDRAWAL" -> List.of(new Notification(e.eventId(), e.fromAccount(),
                "Withdrawal of %s debited from account %s".formatted(e.amount(), e.fromAccount())));
            case "TRANSFER" -> List.of(
                new Notification(e.eventId(), e.fromAccount(),
                    "%s debited from account %s (transfer to %s)".formatted(e.amount(), e.fromAccount(), e.toAccount())),
                new Notification(e.eventId(), e.toAccount(),
                    "%s credited to account %s (transfer from %s)".formatted(e.amount(), e.toAccount(), e.fromAccount())));
            default -> throw new IllegalArgumentException("Unknown event type: " + e.eventType());
        };
    }
}
