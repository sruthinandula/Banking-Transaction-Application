package com.example.processor.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    boolean existsByEventId(String eventId);
    List<AuditLog> findTop50ByOrderByIdDesc();
}
