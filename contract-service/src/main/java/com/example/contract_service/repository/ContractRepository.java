package com.example.contract_service.repository;

import com.example.contract_service.entity.ContractAgreement;
import com.example.contract_service.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<ContractAgreement,Long> {
    boolean existsByTenantIdAndStatus(Long tenantId, ContractStatus status);
}
