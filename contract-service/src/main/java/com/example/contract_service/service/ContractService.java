package com.example.contract_service.service;

import com.example.contract_service.dto.response.ContractResponse;
import com.example.contract_service.entity.ContractAgreement;
import com.example.contract_service.enums.ContractStatus;
import com.example.contract_service.exception.BadRequestException;
import com.example.contract_service.exception.ExternalServiceException;
import com.example.contract_service.exception.ResourceNotFoundException;
import com.example.contract_service.exception.ServiceUnavailableException;
import com.example.contract_service.mapper.ContractMapper;
import com.example.contract_service.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Map;

@Service
public class ContractService {
    @Autowired
    public ContractRepository contractRepository;
    @Autowired
    private RestClient restClient;
    @Autowired
    private ContractMapper contractMapper;

    public ContractAgreement  findContractAgreementById(Long id) throws ResourceNotFoundException {
        return contractRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hợp đồng với ID: " + id
                ));
    }

    @Transactional(rollbackFor = Exception.class)
    public ContractResponse cancelContract (Long id)throws ResourceNotFoundException {
        ContractAgreement contractAgreement = findContractAgreementById(id);
        if (!contractAgreement.getStatus().equals(ContractStatus.ACTIVE)){
            throw new BadRequestException("Hợp đồng này đã bị thanh lý hoặc không còn hiệu lực");
        }
        contractAgreement.setStatus(ContractStatus.TERMINATED);
        contractAgreement.setEndDate(LocalDate.now());
        contractRepository.save(contractAgreement);
        try {
            restClient.patch()
                    .uri("/api/v1/rooms/{roomId}/status", contractAgreement.getRoomId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "AVAILABLE"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("Dịch vụ quản lý phòng (property-service) hiện không khả dụng. Vui lòng thử lại sau.");
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException("Lỗi phản hồi từ property-service: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new ExternalServiceException("Lỗi giao tiếp với dịch vụ quản lý phòng");
        }
        return contractMapper.contractToContractResponse(contractAgreement);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveContract(Long id){
        return contractRepository.existsByTenantIdAndStatus(id,ContractStatus.ACTIVE);
    }
}
