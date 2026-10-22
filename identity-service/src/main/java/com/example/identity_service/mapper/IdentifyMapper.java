package com.example.identity_service.mapper;

import com.example.identity_service.dto.request.CreateTenantAccountRequest;
import com.example.identity_service.dto.response.AuthResponse;
import com.example.identity_service.entity.SysAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface IdentifyMapper {
    SysAccount toSysAccount(CreateTenantAccountRequest request);
    @Mapping(source = "id",target = "accountId")
    AuthResponse toAuthResponse(SysAccount sysAccount);
}
