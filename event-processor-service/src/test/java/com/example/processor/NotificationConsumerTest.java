package com.example.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.processor.notification.Notification;
import com.example.processor.notification.NotificationConsumer;
import com.example.processor.notification.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock NotificationRepository repository;

    private NotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(repository, new ObjectMapper().findAndRegisterModules(),
            new BigDecimal("666.66"));
    }

    private static String event(String id, String type, String amount) {
        return """
            {"eventId":"%s","eventType":"%s","transactionId":1,"fromAccount":"AAA","toAccount":"BBB",
             "amount":%s,"occurredAt":"2026-10-17T10:00:00Z"}""".formatted(id, type, amount);
    }

    @Test
    void transferNotifiesBothAccounts() {
        consumer.consume(event("e1", "TRANSFER", "100.00"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getAccountNumber)
            .containsExactlyInAnyOrder("AAA", "BBB");
    }

    @Test
    void duplicateDeliveryIsIgnored() {
        when(repository.existsByEventIdAndAccountNumber(any(), any())).thenReturn(true);

        consumer.consume(event("e1", "DEPOSIT", "50.00"));

        verify(repository, never()).save(any());
    }

    @Test
    void poisonAmountFailsSoTheErrorHandlerCanRetryAndDeadLetter() {
        assertThrows(IllegalStateException.class, () -> consumer.consume(event("e2", "DEPOSIT", "666.66")));

        verifyNoInteractions(repository);
    }

    @Test
    void malformedPayloadIsNonRetryableIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> consumer.consume("not json"));
    }
}
