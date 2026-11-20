package com.example.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreatedEmailEvent {
    private String eventId;
    private Long tenantId;
    private String username;
    private String fullName;
    private String email;
    private String tempPassword;
}
