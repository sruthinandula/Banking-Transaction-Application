package com.example.processor.web;

import com.example.processor.audit.AuditLog;
import com.example.processor.audit.AuditLogRepository;
import com.example.processor.deadletter.FailedEvent;
import com.example.processor.deadletter.FailedEventRepository;
import com.example.processor.notification.Notification;
import com.example.processor.notification.NotificationRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only endpoints to check what the consumers did. Internal service: deliberately unauthenticated. */
@RestController
@RequestMapping("/internal")
public class InspectionController {

    private final AuditLogRepository auditLogs;
    private final NotificationRepository notifications;
    private final FailedEventRepository failedEvents;

    public InspectionController(AuditLogRepository auditLogs, NotificationRepository notifications,
                                FailedEventRepository failedEvents) {
        this.auditLogs = auditLogs;
        this.notifications = notifications;
        this.failedEvents = failedEvents;
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> auditLogs() {
        return auditLogs.findTop50ByOrderByIdDesc();
    }

    @GetMapping("/notifications")
    public List<Notification> notifications() {
        return notifications.findTop50ByOrderByIdDesc();
    }

    @GetMapping("/dead-letters")
    public List<FailedEvent> deadLetters() {
        return failedEvents.findTop50ByOrderByIdDesc();
    }
}
