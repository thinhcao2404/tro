package com.example.property_service.service;

import com.example.property_service.dto.request.RoomRequest;
import com.example.property_service.dto.response.PageResponse;
import com.example.property_service.entity.Room;
import com.example.property_service.enums.RoomStatus;
import com.example.property_service.exception.BadRequestException;
import com.example.property_service.exception.ResourceNotFoundException;
import com.example.property_service.mapper.PropertyMapper;
import com.example.property_service.repository.PropertyRepository;
import com.example.property_service.util.PageResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PropertyService {
    @Autowired
    private PropertyMapper propertyMapper;

    @Autowired
    private PropertyRepository propertyRepository;

    @Transactional
    public void deleteRoom(Long id) {
        Room room = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng."));
        if(room.getStatus() == RoomStatus.OCCUPIED){
            throw new BadRequestException("Không thể xóa phòng đang có khách thuê. Vui lòng thanh lý hợp đồng trước.");
        }
        propertyRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean checkStatus(Long id){
        Room room = propertyRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Không tìm thấy phòng."));
        return room.getStatus() == RoomStatus.AVAILABLE;
    }

    @Transactional
    public Room createRoom(RoomRequest request) throws ResourceNotFoundException {
        if (propertyRepository.existsByRoomNumber(request.getRoomNumber())){
            throw new BadRequestException("Số phòng '" + request.getRoomNumber() + "' đã tồn tại.");
        }
        Room room = propertyMapper.toRoom(request);
        return propertyRepository.save(room);
    }

    @Transactional
    public Room updateRoom(Long id,RoomRequest request) throws ResourceNotFoundException {
        Room room = findRoomById(id);
        propertyMapper.updateRoomFromRequest(request, room);
        return propertyRepository.save(room);
    }

    public Room updateStatus(Long id,RoomRequest request) throws ResourceNotFoundException {
        Room room = findRoomById(id);
        room.setStatus(request.getStatus());
        return propertyRepository.save(room);
    }

    public Room findRoomById(Long id) throws ResourceNotFoundException {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng có ID " + id + " không tồn tại"));
    }

    @Transactional
    public Room patchRoom(Long id, Map<String, Object> updates) {
        Room room = findRoomById(id);

        if (updates.containsKey("status")) {
            Object statusObj = updates.get("status");
            if (statusObj != null) {
                try {
                    RoomStatus newStatus = RoomStatus.valueOf(statusObj.toString().trim().toUpperCase());
                    room.setStatus(newStatus);
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Trạng thái phòng không hợp lệ.");
                }
            }
        }

        if (updates.containsKey("price")) {
            Object priceObj = updates.get("price");
            if (priceObj != null) {
                try {
                    room.setPrice(new BigDecimal(priceObj.toString()));
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Giá phòng không đúng định dạng số.");
                }
            }
        }

        return propertyRepository.save(room);
    }

    public PageResponse<Room> getRooms(
            String search,
            String status,
            Integer floor,
            int page,
            int size,
            String sort) {

        if (page < 0 || size <= 0) {
            throw new BadRequestException(
                    "Page phải >= 0 và size phải > 0"
            );
        }

        // Xây dựng điều kiện lọc
        Specification<Room> spec = (root, query, cb) ->
                cb.conjunction();

        if (search != null && !search.isBlank()) {
            String keyword = "%" + search.trim().toLowerCase() + "%";

            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("roomNumber")),
                            keyword
                    )
            );
        }

        if (status != null && !status.isBlank()) {
            try {
                RoomStatus enumStatus = RoomStatus.valueOf(status.trim().toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), enumStatus));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái lọc '" + status + "' không hợp lệ.");
            }
        }

        if (floor != null && floor > 0) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("floor"), floor)
            );
        }

        // Xác định trường sắp xếp
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0].trim();

        // Chỉ cho phép các trường được hỗ trợ
        sortField = switch (sortField) {
            case "price", "roomNumber", "floor", "id" -> sortField;
            default -> "id";
        };

        Sort.Direction direction =
                sortParams.length > 1
                        && "desc".equalsIgnoreCase(sortParams[1].trim())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Sort sorting = Sort.by(direction, sortField);

        // Tạo thông tin phân trang
        Pageable pageable = PageRequest.of(page, size, sorting);

        // Database lọc, sắp xếp và phân trang
        Page<Room> roomPage = propertyRepository.findAll(spec, pageable);

        return PageResponseUtil.from(roomPage);
    }
}
