package com.example.tenant_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTenantAccountRequest {
    String username;
    Long tenantId;
    String fullName;
    String oldUsername;
}
