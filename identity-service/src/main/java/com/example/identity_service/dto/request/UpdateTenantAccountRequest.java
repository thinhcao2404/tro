package com.example.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTenantAccountRequest {
    private Long tenantId;
    String oldUsername;
    @NotBlank(message = "Số điện thoại (username) không được để trống")
    String username;
    @NotBlank(message = "Họ tên không được để trống")
    String fullName;

}