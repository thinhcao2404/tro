package com.example.tenant_service.service;

import com.example.tenant_service.dto.request.CreateTenantAccountRequest;
import com.example.tenant_service.dto.request.TenantRequest;
import com.example.tenant_service.dto.request.UpdateTenantAccountRequest;
import com.example.tenant_service.dto.response.TenantResponse;
import com.example.tenant_service.entity.TenantProfile;
import com.example.tenant_service.exception.BadRequestException;
import com.example.tenant_service.exception.ResourceNotFoundException;
import com.example.tenant_service.mapper.TenantMapper;
import com.example.tenant_service.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
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

    @Transactional
    public TenantResponse updateTenant(Long id,TenantRequest request) {
        TenantProfile tenantProfile = findTenantById(id);
        if (tenantRepository.existsByPhoneAndIdNot(request.getPhone(), id)){
            throw new BadRequestException("Số điện thoại này đã được sử dụng bởi khách thuê khác.");
        }
        if (tenantRepository.existsByIdentityNumberAndIdNot(request.getIdentityNumber(),id)){
            throw new BadRequestException("Số CCCD này đã được sử dụng bởi khách thuê khác.");
        }
        String oldUserName = tenantProfile.getPhone();
        tenantProfile.setFullName(request.getFullName());
        tenantProfile.setPhone(request.getPhone());
        tenantProfile.setIdentityNumber(request.getIdentityNumber());
        TenantProfile savedTenant = tenantRepository.save(tenantProfile);

            UpdateTenantAccountRequest updateTenantAccountRequest = UpdateTenantAccountRequest.builder()
                    .username(request.getPhone())
                    .fullName(request.getFullName())
                    .tenantId(savedTenant.getId())
                    .oldUsername(oldUserName)
                    .build();

            restClient.put()
                    .uri("http://localhost:8080/api/v1/accounts/tenants")
                    .body(updateTenantAccountRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new BadRequestException("Đồng bộ tài khoản thất bại: Thông tin gửi sang Identity Service không hợp lệ.");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new RuntimeException("Hệ thống Identity Service gặp sự cố.");
                    })
                    .toBodilessEntity();
        return tenantMapper.toTenantResponse(savedTenant);
    }


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

    @Transactional
    public TenantResponse createTenant(TenantRequest request) {

        if (tenantRepository.existsByIdentityNumber(request.getIdentityNumber()) ||
                tenantRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Số điện thoại hoặc CCCD này đã tồn tại trong hệ thống.");
        }

        TenantProfile tenant = tenantMapper.toTenant(request);
        TenantProfile savedTenant = tenantRepository.save(tenant);

        CreateTenantAccountRequest accountRequest = CreateTenantAccountRequest.builder()
                .tenantId(savedTenant.getId())
                .username(savedTenant.getPhone())
                .fullName(savedTenant.getFullName())
                .build();

        restClient.post()
                .uri("http://localhost:8080/api/v1/accounts/tenants")
                .body(accountRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new BadRequestException("Tạo tài khoản thất bại: Thông tin gửi sang Identity Service không hợp lệ.");
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new RuntimeException("Hệ thống Identity Service gặp sự cố.");
                })
                .toBodilessEntity();

        return tenantMapper.toTenantResponse(savedTenant);
    }
}
