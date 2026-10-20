package com.example.processor.deadletter;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

/**
 * Reads the dead-letter topic and stores each failed event with the reason, so an operator can inspect it.
 */
@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private final FailedEventRepository repository;

    public DeadLetterConsumer(FailedEventRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.kafka.dlt-topic}", groupId = "dlt-service")
    public void consume(ConsumerRecord<String, String> record) {
        String group = header(record, KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP);
        String topic = header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC);
        String error = header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE);

        repository.save(new FailedEvent(topic, group, record.key(), record.value(), error));
        log.error("DEAD LETTER: consumer group '{}' failed on topic '{}' key={} reason={}",
            group, topic, record.key(), error);
    }

    static String header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
