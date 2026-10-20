package com.example.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.processor.audit.AuditConsumer;
import com.example.processor.audit.AuditLog;
import com.example.processor.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditConsumerTest {

    private static final String PAYLOAD = """
        {"eventId":"e1","eventType":"WITHDRAWAL","transactionId":7,"fromAccount":"AAA","toAccount":null,
         "amount":25.50,"occurredAt":"2026-10-17T10:00:00Z","someFutureField":"ignored"}""";

    @Mock AuditLogRepository repository;

    private AuditConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuditConsumer(repository, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void storesNewEvent_andToleratesUnknownFields() {
        consumer.consume(PAYLOAD);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("e1");
        assertThat(captor.getValue().getEventType()).isEqualTo("WITHDRAWAL");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("25.50");
    }

    @Test
    void skipsEventThatWasAlreadyRecorded() {
        when(repository.existsByEventId("e1")).thenReturn(true);

        consumer.consume(PAYLOAD);

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsPayloadMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> consumer.consume("{\"foo\":\"bar\"}"));
    }
}
