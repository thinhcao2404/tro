package com.example.property_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomRequest {

    @NotBlank(message = "Số phòng không được để trống")
    @Size(min = 2, max = 20, message = "Số phòng phải từ 2 đến 20 ký tự")
    String roomNumber;

    @NotNull(message = "Tầng không được để trống")
    @Min(value = 1, message = "Tầng phải từ 1 trở lên")
    @Max(value = 50, message = "Tầng không được vượt quá 50")
    Integer floor;

    @NotNull(message = "Giá phòng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phòng phải lớn hơn 0")
    Double price;

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(AVAILABLE|OCCUPIED|MAINTENANCE)$", message = "Trạng thái chỉ có thể là AVAILABLE, OCCUPIED hoặc MAINTENANCE")
    String status;

    String description;
}