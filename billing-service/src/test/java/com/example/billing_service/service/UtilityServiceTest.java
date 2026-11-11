package com.example.billing_service.service;

import com.example.billing_service.dto.request.UtilityConfigRequest;
import com.example.billing_service.entity.UtilityConfig;
import com.example.billing_service.exception.BadRequestException;
import com.example.billing_service.mapper.UtilityMapper;
import com.example.billing_service.repository.UtilityConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilityServiceTest {

    @Mock
    private UtilityConfigRepository utilityConfigRepository;
    @Mock
    private UtilityMapper utilityMapper;

    private UtilityService utilityService;

    @BeforeEach
    void setUp() {
        utilityService = new UtilityService(utilityConfigRepository, utilityMapper);
    }

    @Test
    void createUtilityConfig_normalizesServiceCodeAndRejectsDuplicate() {
        UtilityConfigRequest req = UtilityConfigRequest.builder()
                .serviceCode(" elec ").serviceName("Điện")
                .unitPrice(new BigDecimal("3500")).unitType("KWH")
                .build();

        when(utilityConfigRepository.existsByServiceCode("ELEC")).thenReturn(false);
        doAnswer(inv -> {
            UtilityConfig target = inv.getArgument(1);
            target.setServiceCode(req.getServiceCode());
            target.setUnitPrice(req.getUnitPrice());
            target.setIsActive(req.getIsActive());
            return null;
        }).when(utilityMapper).utilityUtilityConfig(eq(req), any(UtilityConfig.class));
        when(utilityConfigRepository.save(any(UtilityConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilityConfig saved = utilityService.createUtilityConfig(req);
        assertEquals("ELEC", saved.getServiceCode());
        assertTrue(saved.getIsActive());

        when(utilityConfigRepository.existsByServiceCode("ELEC")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> utilityService.createUtilityConfig(req));
    }

    @Test
    void updateUtilityConfig_allowsSameServiceCodeAndRejectsOtherDuplicate() {
        UtilityConfig existing = UtilityConfig.builder()
                .id(1L).serviceCode("ELEC").serviceName("Điện")
                .unitPrice(new BigDecimal("3500")).unitType("KWH").isActive(true)
                .build();
        when(utilityConfigRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utilityConfigRepository.save(existing)).thenReturn(existing);

        UtilityConfigRequest sameCodeReq = UtilityConfigRequest.builder()
                .serviceCode("ELEC").serviceName("Điện")
                .unitPrice(new BigDecimal("4000")).unitType("KWH").isActive(true)
                .build();

        utilityService.updateUtilityConfig(1L, sameCodeReq);
        verify(utilityConfigRepository, never()).existsByServiceCode(any());
        verify(utilityConfigRepository).save(existing);

        UtilityConfigRequest otherDupReq = UtilityConfigRequest.builder()
                .serviceCode("WATER").serviceName("Nước")
                .unitPrice(new BigDecimal("20000")).unitType("M3").isActive(true)
                .build();
        when(utilityConfigRepository.existsByServiceCode("WATER")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> utilityService.updateUtilityConfig(1L, otherDupReq));
    }
}
