package com.example.identity_service.service;

import com.example.identity_service.dto.event.AccountCreatedEmailEvent;
import com.example.identity_service.dto.request.CreateTenantAccountRequest;
import com.example.identity_service.dto.request.UpdateTenantAccountRequest;
import com.example.identity_service.entity.SysAccount;
import com.example.identity_service.enums.Role;
import com.example.identity_service.exception.BadRequestException;
import com.example.identity_service.exception.ResourceNotFoundException;
import com.example.identity_service.mapper.IdentifyMapper;
import com.example.identity_service.repository.SysAccountRepository;
import com.example.identity_service.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final SysAccountRepository sysAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IdentifyMapper identifyMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public String createTenantAccount(CreateTenantAccountRequest request) {
        if (sysAccountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại.");
        }

        SysAccount sysAccount = identifyMapper.toSysAccount(request);
        sysAccount.setEmail(request.getEmail());
        sysAccount.setRole(Role.TENANT);
        sysAccount.setIsActive(true);
        sysAccount.setMustChangePassword(true);

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        sysAccount.setPasswordHash(passwordEncoder.encode(tempPassword));

        SysAccount savedAccount = sysAccountRepository.save(sysAccount);

        applicationEventPublisher.publishEvent(AccountCreatedEmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .tenantId(savedAccount.getTenantId())
                .username(savedAccount.getUsername())
                .fullName(savedAccount.getFullName())
                .email(savedAccount.getEmail())
                .tempPassword(tempPassword)
                .build());

        return tempPassword;
    }

    @Transactional
    public void updateTenantAccount(UpdateTenantAccountRequest request) {

        SysAccount account = sysAccountRepository.findByUsername(request.getOldUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));

        if (!request.getOldUsername().equals(request.getUsername())
                && sysAccountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập mới đã được sử dụng bởi tài khoản khác.");
        }
        account.setFullName(request.getFullName());
        account.setUsername(request.getUsername());
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            account.setEmail(request.getEmail());
        }

        sysAccountRepository.save(account);
    }

    @Transactional
    public void deleteTenantAccount(Long id) throws ResourceNotFoundException {
        SysAccount account = sysAccountRepository.findByTenantId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));
        account.setIsActive(false);
        sysAccountRepository.save(account);
        refreshTokenRepository.deleteByAccount(account);
    }
}
