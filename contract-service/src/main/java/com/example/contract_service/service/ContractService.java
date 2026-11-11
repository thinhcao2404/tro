package com.example.contract_service.service;

import com.example.contract_service.dto.request.ContractCreateRequest;
import com.example.contract_service.dto.response.ContractResponse;
import com.example.contract_service.dto.response.PageResponse;
import com.example.contract_service.entity.ContractAgreement;
import com.example.contract_service.enums.ContractStatus;
import com.example.contract_service.exception.BadRequestException;
import com.example.contract_service.exception.ExternalServiceException;
import com.example.contract_service.exception.ResourceNotFoundException;
import com.example.contract_service.exception.ServiceUnavailableException;
import com.example.contract_service.mapper.ContractMapper;
import com.example.contract_service.repository.ContractRepository;
import com.example.contract_service.util.PageResponseUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ContractService {
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private RestClient restClient;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public ContractAgreement  findContractAgreementById(Long id){
        return contractRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng với ID: " + id));
    }
    public ContractResponse findContractById(Long id){
        ContractAgreement contract = contractRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng với ID: " + id));
        return contractMapper.contractToContractResponse(contract);
    }
    public List<ContractResponse> getContract(Long tenantId,ContractStatus contractStatus){
        List<ContractAgreement> contract = contractRepository.findByTenantIdAndOptionalStatus(tenantId,contractStatus);
        return contractMapper.contractListToContractResponseList(contract);
    }

    public PageResponse<ContractResponse> getContracts(
            String search,
            String status,
            BigDecimal deposit,
            int page,
            int size,
            String sort) {

        if (page < 0 || size <= 0) {
            throw new BadRequestException(
                    "Page phải >= 0 và size phải > 0"
            );
        }

        // Xây dựng điều kiện lọc
        Specification<ContractAgreement> spec = (root, query, cb) ->
                cb.conjunction();

        if (search != null && !search.isBlank()) {
            try {
                Long contractId = Long.parseLong(search.trim());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), contractId));
            } catch (NumberFormatException e) {
            }
        }

        if (status != null && !status.isBlank()) {
            try {
                ContractStatus enumStatus = ContractStatus.valueOf(status.trim().toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), enumStatus.name()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái hợp đồng '" + status + "' không hợp lệ.");
            }
        }

        if (deposit != null && deposit.compareTo(BigDecimal.ZERO) > 0) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("deposit"), deposit)
            );
        }

        // Xác định trường sắp xếp
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0].trim();

        // Chỉ cho phép các trường được hỗ trợ
        sortField = switch (sortField) {
            case "id", "deposit", "startDate", "endDate", "status", "createdAt", "roomId", "tenantId" -> sortField;
            default -> "id";
        };

        Sort.Direction direction =
                sortParams.length > 1
                        && "desc".equalsIgnoreCase(sortParams[1].trim())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Sort sorting = Sort.by(direction, sortField);

        // Tạo thông tin phân trang
        Pageable pageable = PageRequest.of(page, size, sorting);

        // Database lọc, sắp xếp và phân trang
        Page<ContractAgreement> contractPage = contractRepository.findAll(spec, pageable);
        Page<ContractResponse> responsePage = contractPage.map(contractMapper::contractToContractResponse);


        return PageResponseUtil.from(responsePage);
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
                    .uri("/api/v1/rooms/internal/{roomId}/status", contractAgreement.getRoomId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "AVAILABLE"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("Dịch vụ quản lý phòng hiện không khả dụng. Vui lòng thử lại sau.");
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

    @Transactional
    public ContractResponse createContract(ContractCreateRequest contractCreateRequest){
        Boolean isAvailable = false;

        try{
            isAvailable = restClient.get()
                    .uri("/api/v1/rooms/internal/check-status/{id}", contractCreateRequest.getRoomId())
                    .retrieve()
                    .body(Boolean.class);
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("Dịch vụ quản lý phòng hiện không khả dụng. Vui lòng thử lại sau.");
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException("Lỗi phản hồi từ property-service: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new ExternalServiceException("Lỗi giao tiếp với dịch vụ quản lý phòng");
        }

        if(!Boolean.TRUE.equals(isAvailable)){
            throw new BadRequestException("Phòng này hiện không sẵn sàng hoặc đã có người thuê.");
        }

        ContractAgreement contractAgreement = contractMapper.contractToContractAgreement(contractCreateRequest);
        contractAgreement.setStatus(ContractStatus.ACTIVE);
        ContractAgreement savedContract = contractRepository.save(contractAgreement);

        try {
            restClient.patch()
                    .uri("/api/v1/rooms/internal/{roomId}/status", contractAgreement.getRoomId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "OCCUPIED"))
                    .retrieve()
                    .toBodilessEntity();
        }catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("Dịch vụ quản lý phòng hiện không khả dụng. Vui lòng thử lại sau.");
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException("Lỗi phản hồi từ property-service: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new ExternalServiceException("Không thể cập nhật trạng thái phòng sang OCCUPIED.");
        }
        rabbitTemplate.convertAndSend(savedContract);
        return contractMapper.contractToContractResponse(savedContract);
    }
}
