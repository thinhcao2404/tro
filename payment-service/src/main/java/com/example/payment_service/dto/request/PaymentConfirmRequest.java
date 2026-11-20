package com.example.payment_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentConfirmRequest {

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    String note;
}
