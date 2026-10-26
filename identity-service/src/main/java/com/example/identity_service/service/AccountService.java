package com.example.identity_service.service;

import com.example.identity_service.dto.request.CreateTenantAccountRequest;
import com.example.identity_service.dto.request.UpdateTenantAccountRequest;
import com.example.identity_service.entity.SysAccount;
import com.example.identity_service.enums.Role;
import com.example.identity_service.exception.BadRequestException;
import com.example.identity_service.exception.ResourceNotFoundException;
import com.example.identity_service.mapper.IdentifyMapper;
import com.example.identity_service.repository.IdentifyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {
    @Autowired
    private IdentifyRepository identifyRepository;

    @Autowired
    private IdentifyMapper identifyMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public String createTenantAccount(CreateTenantAccountRequest request) {
        if (identifyRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại.");
        }

        SysAccount sysAccount = identifyMapper.toSysAccount(request);
        sysAccount.setRole(Role.TENANT);
        sysAccount.setIsActive(true);

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        sysAccount.setPasswordHash(passwordEncoder.encode(tempPassword));

        identifyRepository.save(sysAccount);

        return tempPassword;
    }

    @Transactional
    public void updateTenantAccount(UpdateTenantAccountRequest request) {

        SysAccount account = identifyRepository.findByUsername(request.getOldUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));

        account.setFullName(request.getFullName());
        account.setUsername(request.getUsername());

        identifyRepository.save(account);
    }

}
