package com.example.billing_service.repository;

import com.example.billing_service.entity.UtilityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilityConfigRepository extends JpaRepository<UtilityConfig, Long> {

    Optional<UtilityConfig> findByServiceCode(String serviceCode);

    boolean existsByServiceCode(String serviceCode);
}
