package com.example.tenant_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantResponse {
    Long id;
    String fullName;
    String phone;
    String identityNumber;
    LocalDateTime createdAt;
}