package com.example.notification_service.repository;

import com.example.notification_service.entity.NotificationLog;
import com.example.notification_service.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByEventIdAndStatus(String eventId, NotificationStatus status);
    Optional<NotificationLog> findByEventId(String eventId);
}
