package com.example.identity_service.repository;

import com.example.identity_service.entity.SysAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentifyRepository extends JpaRepository<SysAccount,Long> {
    boolean existsByUsername(String username);
    Optional<SysAccount> findByUsername(String username);
}
