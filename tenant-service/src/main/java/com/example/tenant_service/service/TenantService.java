package com.example.tenant_service.service;

import com.example.tenant_service.entity.TenantProfile;
import com.example.tenant_service.exception.BadRequestException;
import com.example.tenant_service.exception.ResourceNotFoundException;
import com.example.tenant_service.mapper.TenantMapper;
import com.example.tenant_service.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class TenantService {
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private RestClient restClient;

    public TenantProfile findTenantById(Long id) throws ResourceNotFoundException {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách thuê."));
    }
    @Transactional
    public void deleteTenant(Long id) throws ResourceNotFoundException {
        TenantProfile tenant = findTenantById(id);
        boolean hasActiveContract = restClient
                .get()
                .uri("/api/v1/contracts/check-out?tenantId={tenantId}",id)
                .retrieve()
                .body(Boolean.class);
        if (Boolean.TRUE.equals(hasActiveContract)) {
            throw new BadRequestException("Khách hàng này còn hợp đồng.");
        }
        tenantRepository.delete(tenant);
    }
}
