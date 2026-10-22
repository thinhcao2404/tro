package com.example.contract_service.mapper;

import com.example.contract_service.dto.response.ContractResponse;
import com.example.contract_service.entity.ContractAgreement;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContractMapper {
    ContractResponse contractToContractResponse(ContractAgreement contract);
}
