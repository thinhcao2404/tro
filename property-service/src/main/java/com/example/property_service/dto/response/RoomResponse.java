package com.example.property_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    Long id;
    String roomNumber;
    Integer floor;
    BigDecimal price;
    String status;
    String description;
}