package com.example.bank.outbox;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           @Value("${app.kafka.topic}") String topic) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByPublishedFalseOrderByIdAsc();
        for (OutboxEvent event : pending) {
            try {
                // Block for the broker ack so we only mark rows published after Kafka has them,
                // and so events stay in order. On failure stop and retry on the next tick.
                kafkaTemplate.send(topic, event.getEventKey(), event.getPayload()).get(5, TimeUnit.SECONDS);
                event.markPublished();
                outboxRepository.save(event);
                log.info("Published event {} to {}", event.getEventId(), topic);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Kafka publish failed for event {}, will retry: {}", event.getEventId(), e.getMessage());
                return;
            }
        }
    }
}
