package com.example.processor.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByEventIdAndAccountNumber(String eventId, String accountNumber);
    List<Notification> findTop50ByOrderByIdDesc();
}
