package com.example.property_service.controller;

import com.example.property_service.dto.response.PageResponse;
import com.example.property_service.entity.Room;
import com.example.property_service.dto.request.RoomRequest;
import com.example.property_service.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    @Autowired
    private PropertyService propertyService;

    @GetMapping
    public ResponseEntity<PageResponse<Room>> getRooms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer floor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(defaultValue = "id,asc") String sort){
        PageResponse<Room> response = propertyService.getRooms(search, status, floor, page, size, sort);
        return ResponseEntity.ok(response);
    }

    // Tra cứu chi tiết phòng theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getRoomById(
            @PathVariable Long id,
            @RequestParam(value = "slow",defaultValue = "false") boolean slow)
    {
        if(slow){
            try{
                Thread.sleep(10000);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
        Room room = propertyService.findRoomById(id);
        return ResponseEntity.ok(room);
    }

    // Tạo phòng mới
    @PostMapping
    public ResponseEntity<Room> createRoom(@Valid @RequestBody RoomRequest request) {
        Room room = propertyService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    //Cập nhật toàn bộ thông tin phòng
    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id,
                                           @Valid @RequestBody RoomRequest request) {
        Room room = propertyService.updateRoom(id, request);
        return ResponseEntity.ok(room);
    }

    // Cập nhật trạng thái phòng
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateRoomStatus(
            @PathVariable Long id,
            @RequestBody RoomRequest request) {
        Room room = propertyService.updateStatus(id,request);
        return ResponseEntity.ok(room);
    }

    // Cập nhật 1 phần thông tin phòng
    @PatchMapping("/{id}")
    public ResponseEntity<Room> patchRoom(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Room room = propertyService.patchRoom(id,updates);
        return ResponseEntity.ok(room);
    }

    // Xóa phòng
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        propertyService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}