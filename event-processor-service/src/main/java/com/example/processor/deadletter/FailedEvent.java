package com.example.processor.deadletter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "failed_events")
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_topic", length = 200)
    private String sourceTopic;

    @Column(name = "failed_consumer_group", length = 200)
    private String failedConsumerGroup;

    @Column(name = "message_key", length = 100)
    private String messageKey;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt = Instant.now();

    protected FailedEvent() {}

    public FailedEvent(String sourceTopic, String failedConsumerGroup, String messageKey,
                       String payload, String errorMessage) {
        this.sourceTopic = sourceTopic;
        this.failedConsumerGroup = failedConsumerGroup;
        this.messageKey = messageKey;
        this.payload = payload;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public String getSourceTopic() { return sourceTopic; }
    public String getFailedConsumerGroup() { return failedConsumerGroup; }
    public String getMessageKey() { return messageKey; }
    public String getPayload() { return payload; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getFailedAt() { return failedAt; }
}
