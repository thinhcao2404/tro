package com.example.tenant_service.service;

import com.example.tenant_service.dto.request.CreateTenantAccountRequest;
import com.example.tenant_service.dto.request.TenantRequest;
import com.example.tenant_service.dto.request.UpdateTenantAccountRequest;
import com.example.tenant_service.dto.response.PageResponse;
import com.example.tenant_service.dto.response.TenantResponse;
import com.example.tenant_service.entity.TenantProfile;
import com.example.tenant_service.exception.BadRequestException;
import com.example.tenant_service.exception.ExternalServiceException;
import com.example.tenant_service.exception.ResourceNotFoundException;
import com.example.tenant_service.exception.ServiceUnavailableException;
import com.example.tenant_service.mapper.TenantMapper;
import com.example.tenant_service.repository.TenantRepository;
import com.example.tenant_service.util.PageResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;


@Service
public class TenantService {
    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private RestClient contractRestClient;

    @Autowired
    private RestClient identityRestClient;

    public PageResponse<TenantResponse> getTenants(
            String search,
            String phone,
            String identityNumber,
            Boolean isDeleted,
            int page,
            int size,
            String sort) {

        if (page < 0 || size <= 0) {
            throw new BadRequestException(
                    "Page phải >= 0 và size phải > 0"
            );
        }

        // Xây dựng điều kiện lọc
        Specification<TenantProfile> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String keyword = "%" + search.trim().toLowerCase() + "%";

            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("fullName")),
                            keyword
                    )
            );
        }

        if (phone != null && !phone.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(
                            cb.lower(root.get("phone")),
                            phone.trim().toLowerCase()
                    )
            );
        }

        if (identityNumber != null ) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("identityNumber"), identityNumber)
            );
        }

        if (isDeleted != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isDeleted"), isDeleted)
            );
        }

        // Xác định trường sắp xếp
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0].trim();

        // Chỉ cho phép các trường được hỗ trợ
        sortField = switch (sortField) {
            case "fullName", "search" -> "fullName";
            case "phone" -> "phone";
            case "identityNumber" -> "identityNumber";
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
        Page<TenantProfile> tenantPage = tenantRepository.findAll(spec, pageable);
        Page<TenantResponse> responsePage = tenantPage.map(tenantMapper::toTenantResponse);
        return PageResponseUtil.from(responsePage);
    }

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

        try{
            identityRestClient.put()
                    .uri("/api/v1/accounts/tenants")
                    .body(updateTenantAccountRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new BadRequestException("Đồng bộ tài khoản thất bại: Thông tin gửi sang Identity Service không hợp lệ.");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new RuntimeException("Hệ thống Identity Service gặp sự cố.");
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e){
            throw new ServiceUnavailableException("Không thể kết nối đến Identity Service. Vui lòng thử lại sau.");
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException("Lỗi phản hồi từ Identity Service: " + e.getResponseBodyAsString());
        }
        return tenantMapper.toTenantResponse(savedTenant);
    }


    public TenantProfile findTenantById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách thuê."));
    }

    @Transactional
    public void deleteTenant(Long id){
        TenantProfile tenant = findTenantById(id);
        try{
            Boolean hasActiveContract = contractRestClient.get()
                    .uri("/api/v1/contracts/internal/check-active?tenantId={tenantId}",id)
                    .retrieve()
                    .body(Boolean.class);
            if (Boolean.TRUE.equals(hasActiveContract)) {
                throw new BadRequestException("Không thể xóa: Khách thuê này hiện vẫn còn hợp đồng đang hoạt động.");
            }

            identityRestClient.delete()
                    .uri("/api/v1/accounts/{tenantId}",id)
                    .retrieve()
                    .toBodilessEntity();

            tenant.setDeleted(true);

        }catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("Không thể kết nối đến hệ thống liên kết . Vui lòng thử lại sau.");
        }catch (RestClientResponseException e){
            throw new ExternalServiceException("Lỗi phản hồi từ dịch vụ liên kết: " + e.getResponseBodyAsString());
        }
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
        try{
            identityRestClient.post()
                    .uri("/api/v1/accounts/tenants")
                    .body(accountRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new BadRequestException("Tạo tài khoản thất bại: Thông tin gửi sang Identity Service không hợp lệ.");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new RuntimeException("Hệ thống Identity Service gặp sự cố.");
                    })
                    .toBodilessEntity();
        }catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("Không thể kết nối đến Identity Service. Vui lòng thử lại sau.");
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException("Lỗi phản hồi từ Identity Service: " + e.getResponseBodyAsString());
        }


        return tenantMapper.toTenantResponse(savedTenant);
    }
}
