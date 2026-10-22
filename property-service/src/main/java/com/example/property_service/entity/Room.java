package com.example.property_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "prop_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "room_number", nullable = false, unique = true, length = 20)
    String roomNumber;

    @Column(name = "floor", nullable = false)
    Integer floor;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    BigDecimal price;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    String status = "AVAILABLE";

    @Column(name = "description", length = 255)
    String description;

}