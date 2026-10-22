package com.example.tenant_service.mapper;

import com.example.tenant_service.dto.request.TenantRequest;
import com.example.tenant_service.dto.response.TenantResponse;
import com.example.tenant_service.entity.TenantProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {
    TenantProfile toTenant(TenantRequest request);
    TenantResponse toTenantResponse(TenantProfile profile);
}
