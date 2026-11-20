package com.example.payment_service.dto.request;

import com.example.payment_service.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentCreateRequest {

    @NotNull(message = "ID hóa đơn không được để trống")
    Long invoiceId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    PaymentMethod method;

    @Size(max = 20, message = "Mã ngân hàng tối đa 20 ký tự")
    String bankCode;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    String note;
}
