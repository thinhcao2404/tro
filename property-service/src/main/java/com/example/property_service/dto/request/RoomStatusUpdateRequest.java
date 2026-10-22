package com.example.property_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatusUpdateRequest {

    @NotBlank(message = "Trạng thái phòng không được để trống")
    @Pattern(regexp = "AVAILABLE|OCCUPIED", message = "Trạng thái chỉ nhận AVAILABLE hoặc OCCUPIED")
    String status;
}