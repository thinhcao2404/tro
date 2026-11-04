package com.example.identity_service.service;

import com.example.identity_service.dto.request.CreateTenantAccountRequest;
import com.example.identity_service.dto.request.UpdateTenantAccountRequest;
import com.example.identity_service.entity.SysAccount;
import com.example.identity_service.enums.Role;
import com.example.identity_service.exception.BadRequestException;
import com.example.identity_service.exception.ResourceNotFoundException;
import com.example.identity_service.mapper.IdentifyMapper;
import com.example.identity_service.repository.SysAccountRepository;
import com.example.identity_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {
    @Autowired
    private SysAccountRepository sysAccountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private IdentifyMapper identifyMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public String createTenantAccount(CreateTenantAccountRequest request) {
        if (sysAccountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại.");
        }

        SysAccount sysAccount = identifyMapper.toSysAccount(request);
        sysAccount.setRole(Role.TENANT);
        sysAccount.setIsActive(true);

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        sysAccount.setPasswordHash(passwordEncoder.encode(tempPassword));

        sysAccountRepository.save(sysAccount);

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
