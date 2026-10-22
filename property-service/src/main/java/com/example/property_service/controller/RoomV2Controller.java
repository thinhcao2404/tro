package com.example.property_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/rooms")
public class RoomV2Controller {

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoomV2(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "roomCode", "ROOM-P." + id,
                "pricing", Map.of(
                        "amount", 4000000,
                        "currency", "VND",
                        "billingCycle", "MONTHLY"
                ),
                "status", "AVAILABLE"
        ));
    }
}