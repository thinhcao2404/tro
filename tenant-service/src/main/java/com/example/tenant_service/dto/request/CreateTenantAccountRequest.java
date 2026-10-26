package com.example.tenant_service.dto.request;

import ch.qos.logback.core.joran.spi.DefaultClass;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateTenantAccountRequest {
    String username;
    String fullName;
    Long tenantId;
}