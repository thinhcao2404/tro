package com.example.property_service.repository;

import com.example.property_service.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Room,Long>, JpaSpecificationExecutor<Room> {
    Optional<Room> findById(Long id);
}
