package com.example.tenant_service.repository;

import com.example.tenant_service.entity.TenantProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TenantRepository  extends JpaRepository <TenantProfile,Long>,
        JpaSpecificationExecutor<TenantProfile> {
}
