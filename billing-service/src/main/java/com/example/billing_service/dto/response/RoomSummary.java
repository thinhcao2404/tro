package com.example.billing_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoomSummary(Long id, String roomNumber, BigDecimal price) {}
