package com.example.property_service.service;

import com.example.property_service.dto.request.RoomRequest;
import com.example.property_service.dto.response.PageResponse;
import com.example.property_service.entity.Room;
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

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PropertyService {
    @Autowired
    private PropertyMapper propertyMapper;
    @Autowired
    private PropertyRepository propertyRepository;

    public void deleteRoom(Long id) {
        propertyRepository.deleteById(id);
    }

    public Room createRoom(RoomRequest request) throws ResourceNotFoundException {
        Room room = propertyMapper.toRoom(request);
        return propertyRepository.save(room);
    }

    public Room updateRoom(Long id,RoomRequest request) throws ResourceNotFoundException {
        Room room = findRoomById(id);
        propertyMapper.updateRoomFromRequest(request, room);
        return propertyRepository.save(room);
    }

    public Room updateStatus(Long id,RoomRequest request) throws ResourceNotFoundException {
        Room room = findRoomById(id);
        room.setStatus(request.getStatus().toUpperCase());
        return propertyRepository.save(room);
    }

    public Room findRoomById(Long id) throws ResourceNotFoundException {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Phòng có ID " + id + " không tồn tại"
                ));
    }
    public Room patchRoom(
            Long id,
            Map<String, Object> updates)
            throws ResourceNotFoundException {

        Room room = findRoomById(id);

        if (updates.containsKey("status")) {
            Object status = updates.get("status");

            if (status != null && !(status instanceof String)) {
                throw new BadRequestException(
                        "Status phải là chuỗi"
                );
            }

            room.setStatus((String) status);
        }

        if (updates.containsKey("price")) {
            Object price = updates.get("price");

            if (price != null) {
                room.setPrice(new BigDecimal(price.toString()));
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
            spec = spec.and((root, query, cb) ->
                    cb.equal(
                            cb.lower(root.get("status")),
                            status.trim().toLowerCase()
                    )
            );
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

        // Chuyển sang response dùng chung
        return PageResponseUtil.from(roomPage);
    }
}
