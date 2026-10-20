package com.example.processor.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {}

    public Notification(String eventId, String accountNumber, String message) {
        this.eventId = eventId;
        this.accountNumber = accountNumber;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getAccountNumber() { return accountNumber; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
