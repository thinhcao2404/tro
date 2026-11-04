package com.example.contract_service.repository;

import com.example.contract_service.entity.ContractAgreement;
import com.example.contract_service.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ContractRepository extends JpaSpecificationExecutor<ContractAgreement>,JpaRepository<ContractAgreement,Long> {
    boolean existsByTenantIdAndStatus(Long tenantId, ContractStatus status);

    @Query("SELECT c FROM ContractAgreement c " +
            "WHERE c.tenantId = :tenantId " +
            "AND (:status IS NULL OR c.status = :status) ")
    List<ContractAgreement> findByTenantIdAndOptionalStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") ContractStatus status
    );
}
