package com.example.processor.deadletter;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {
    List<FailedEvent> findTop50ByOrderByIdDesc();
}
