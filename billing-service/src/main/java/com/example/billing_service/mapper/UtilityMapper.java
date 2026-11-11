package com.example.billing_service.mapper;

import com.example.billing_service.dto.request.UtilityConfigRequest;
import com.example.billing_service.entity.UtilityConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UtilityMapper {
    @Mapping(target = "id", ignore = true)
    void utilityUtilityConfig(UtilityConfigRequest request,
                              @MappingTarget UtilityConfig utilityConfig);
}
