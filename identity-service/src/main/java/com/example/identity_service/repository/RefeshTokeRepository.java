package com.example.identity_service.repository;

import com.example.identity_service.entity.RefreshToken;
import com.example.identity_service.entity.SysAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefeshTokeRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByToken(String token);
    public void deleteByAccount(SysAccount sysAccount);
}
