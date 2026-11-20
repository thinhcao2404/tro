package com.example.notification_service.entity;

import com.example.notification_service.enums.NotificationChannel;
import com.example.notification_service.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_event_id", columnNames = "event_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    String eventId;

    @Column(name = "tenant_id")
    Long tenantId;

    @Column(name = "recipient", nullable = false, length = 120)
    String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    NotificationChannel channel;

    @Column(name = "template_type", nullable = false, length = 50)
    String templateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    NotificationStatus status;

    @Column(name = "error_message", length = 500)
    String errorMessage;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
