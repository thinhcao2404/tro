package com.example.property_service.mapper;

import com.example.property_service.dto.request.RoomRequest;
import com.example.property_service.entity.Room;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PropertyMapper {
    Room toRoom(RoomRequest request);
    void updateRoomFromRequest(
            RoomRequest request,
            @MappingTarget Room room
    );
}
