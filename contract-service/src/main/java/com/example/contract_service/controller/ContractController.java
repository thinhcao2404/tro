package com.example.contract_service.controller;

import com.example.contract_service.dto.*;
import com.example.contract_service.dto.response.ContractResponse;
import com.example.contract_service.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {
    @Autowired
    private ContractService contractService;

    @PatchMapping("/{id}/checkout")
    public ResponseEntity<ContractResponse> cancelContract(@PathVariable Long id){
        ContractResponse response = contractService.cancelContract(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-active")
    public ResponseEntity<Boolean> getActiveContract(@RequestParam("tenantId") Long tenantId){
       boolean hasActive = contractService.hasActiveContract(tenantId);
       return ResponseEntity.ok(hasActive);
    }


//    @PostMapping
//    public ResponseEntity<?> createContract(
//            @RequestBody ContractCreateRequest request,
//            @RequestParam(required = false, defaultValue = "false") boolean testSlow) {
//
//        RoomDTO room;
//
//        try {
//            room = restClient.get()
//                    .uri(uriBuilder -> uriBuilder
//                            .path("/api/v1/rooms/{id}")
//                            .queryParam("slow", testSlow)
//                            .build(request.getRoomId()))
//                    .retrieve()
//                    .body(RoomDTO.class);
//
//        } catch (HttpClientErrorException.NotFound e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
//                    "status", 404,
//                    "error", "Not Found",
//                    "message", "Không thể tạo hợp đồng vì Room ID " + request.getRoomId() + " không tồn tại!"
//            ));
//
//        } catch (ResourceAccessException e) {
//            if (e.getCause() instanceof SocketTimeoutException) {
//                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Map.of(
//                        "status", 504,
//                        "error", "Gateway Timeout",
//                        "message", "Property Service phản hồi quá 3 giây. Yêu cầu bị hủy (Timeout)!"
//                ));
//            }
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
//                    "status", 503,
//                    "error", "Service Unavailable",
//                    "message", "Không thể kết nối đến Property Service (Dịch vụ chưa bật hoặc bị sập)!"
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
//                    "status", 500,
//                    "error", "Internal Server Error",
//                    "message", "Lỗi không xác định: " + e.getMessage()
//            ));
//        }
//
//        if (room == null || !"AVAILABLE".equalsIgnoreCase(room.getStatus())) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
//                    "status", 400,
//                    "error", "Bad Request",
//                    "message", "Phòng " + request.getRoomId() + " hiện đã có người thuê (" + (room != null ? room.getStatus() : "NULL") + ")!"
//            ));
//        }
//
//        // Lưu hợp đồng
//        Long contractId = System.currentTimeMillis();
//        ContractAgreement agreement = new ContractAgreement(
//                contractId,
//                request.getRoomId(),
//                request.getTenantId(),
//                request.getDeposit(),
//                request.getStartDate(),
//                request.getEndDate(),
//                "ACTIVE"
//        );
//        contractDb.put(contractId, agreement);
//
//        // Đổi trạng thái phòng sau khi lưu
//        try {
//            restClient.put()
//                    .uri("/api/v1/rooms/{id}/status", request.getRoomId())
//                    .body(Map.of("status", "OCCUPIED"))
//                    .retrieve()
//                    .toBodilessEntity();
//        } catch (Exception ignored) {
//        }
//
//        // Bắn message sang RabbitMQ
//        ContractCreatedEvent event = new ContractCreatedEvent(
//                contractId,
//                request.getRoomId(),
//                request.getTenantId(),
//                request.getDeposit(),
//                request.getStartDate()
//        );
//
//        String messagePayload = String.format(
//                "{\"event\":\"ContractCreatedEvent\",\"contractId\":%d,\"roomId\":%d,\"tenantId\":%d,\"deposit\":%.0f}",
//                contractId, request.getRoomId(), request.getTenantId(), request.getDeposit()
//        );
//
//        rabbitTemplate.convertAndSend(AppConfig.CONTRACT_QUEUE, messagePayload);
//        System.out.println(messagePayload);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
//                "message", "Tạo hợp đồng thành công!",
//                "contract", agreement,
//                "roomDetail", room
//        ));
//    }
}