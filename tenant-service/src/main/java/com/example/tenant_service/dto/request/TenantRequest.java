package com.example.tenant_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRequest {

    @NotBlank(message = "Họ và tên khách thuê không được để trống")
    String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không đúng định dạng")
    String phone;

    @NotBlank(message = "Số CCCD/CMND không được để trống")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "Số CCCD/CMND phải từ 9 đến 12 chữ số")
    String identityNumber;
}