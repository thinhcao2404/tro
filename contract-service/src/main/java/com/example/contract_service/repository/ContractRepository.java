package com.example.contract_service.repository;

import com.example.contract_service.entity.ContractAgreement;
import com.example.contract_service.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ContractRepository extends JpaSpecificationExecutor<ContractAgreement>,JpaRepository<ContractAgreement,Long> {
    boolean existsByTenantIdAndStatus(Long tenantId, ContractStatus status);
    Optional<ContractAgreement> findByTenantIdAndStatus(Long tenantId, ContractStatus status);
}
