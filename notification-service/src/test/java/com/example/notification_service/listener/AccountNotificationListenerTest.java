package com.example.notification_service.listener;

import com.example.notification_service.dto.AccountCreatedEmailEvent;
import com.example.notification_service.entity.NotificationLog;
import com.example.notification_service.enums.NotificationStatus;
import com.example.notification_service.repository.NotificationLogRepository;
import com.example.notification_service.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountNotificationListenerTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AccountNotificationListener listener;

    @Test
    void handleAccountCreatedEmail_sendsAndLogsSent() {
        AccountCreatedEmailEvent event = AccountCreatedEmailEvent.builder()
                .eventId("evt-1")
                .tenantId(10L)
                .username("0912345678")
                .fullName("Nguyen Van A")
                .email("a@example.com")
                .tempPassword("pass1234")
                .build();

        when(notificationLogRepository.existsByEventIdAndStatus("evt-1", NotificationStatus.SENT)).thenReturn(false);
        when(notificationLogRepository.findByEventId("evt-1")).thenReturn(Optional.empty());

        listener.handleAccountCreatedEmail(event);

        verify(emailService).sendAccountCredentials(event);
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertEquals(NotificationStatus.SENT, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getSentAt());
    }

    @Test
    void handleAccountCreatedEmail_skipsDuplicateSentEvent() {
        AccountCreatedEmailEvent event = AccountCreatedEmailEvent.builder()
                .eventId("evt-dup")
                .username("0912345678")
                .fullName("Nguyen Van A")
                .email("a@example.com")
                .tempPassword("pass1234")
                .build();

        when(notificationLogRepository.existsByEventIdAndStatus("evt-dup", NotificationStatus.SENT)).thenReturn(true);

        listener.handleAccountCreatedEmail(event);

        verifyNoInteractions(emailService);
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void handleAccountCreatedEmail_rejectsInvalidPayloadWithoutRequeue() {
        AccountCreatedEmailEvent event = AccountCreatedEmailEvent.builder()
                .eventId("evt-bad")
                .username("0912345678")
                .email("")
                .tempPassword("pass1234")
                .build();

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.handleAccountCreatedEmail(event));
        verifyNoInteractions(emailService);
    }
}
