package com.example.billing_service.service;

import com.example.billing_service.dto.request.UtilityConfigRequest;
import com.example.billing_service.entity.UtilityConfig;
import com.example.billing_service.exception.BadRequestException;
import com.example.billing_service.exception.ResourceNotFoundException;
import com.example.billing_service.mapper.UtilityMapper;
import com.example.billing_service.repository.UtilityConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilityService {
    private final UtilityConfigRepository utilityConfigRepository;
    private final UtilityMapper utilityMapper;

    public List<UtilityConfig> findAll() {
        return utilityConfigRepository.findAll();
    }

    public UtilityConfig createUtilityConfig(UtilityConfigRequest request) {
        request.setServiceCode(request.getServiceCode().trim().toUpperCase());
        if (utilityConfigRepository.existsByServiceCode(request.getServiceCode())) {
            throw new BadRequestException("Dịch vụ đã tồn tại.");
        }
        UtilityConfig utilityConfig = new UtilityConfig();
        utilityMapper.utilityUtilityConfig(request, utilityConfig);

        return utilityConfigRepository.save(utilityConfig);
    }

    public UtilityConfig updateUtilityConfig(Long id, UtilityConfigRequest request) {
        request.setServiceCode(request.getServiceCode().trim().toUpperCase());
        UtilityConfig utilityConfig = utilityConfigRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Không tìm thấy dịch vụ."));
        if (!utilityConfig.getServiceCode().equalsIgnoreCase(request.getServiceCode()) &&
                utilityConfigRepository.existsByServiceCode(request.getServiceCode())) {
            throw new BadRequestException("Dịch vụ đã tồn tại.");
        }
        utilityMapper.utilityUtilityConfig(request, utilityConfig);

        return utilityConfigRepository.save(utilityConfig);
    }
}
