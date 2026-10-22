package com.example.identity_service.service;

import com.example.identity_service.dto.request.CreateTenantAccountRequest;
import com.example.identity_service.entity.SysAccount;
import com.example.identity_service.enums.Role;
import com.example.identity_service.exception.BadRequestException;
import com.example.identity_service.mapper.IdentifyMapper;
import com.example.identity_service.repository.IdentifyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    @Autowired
    public IdentifyRepository identifyRepository;

    @Autowired
    public IdentifyMapper identifyMapper;

    @Autowired
    public PasswordEncoder passwordEncoder;

    @Transactional
    public void createTenantAccount(CreateTenantAccountRequest request) {
        if(identifyRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại.");
        }
        SysAccount sysAccount = identifyMapper.toSysAccount(request);
         sysAccount.setRole(Role.TENANT);
         sysAccount.setIsActive(true);
         sysAccount.setPasswordHash(passwordEncoder.encode(request.getDefaultPassword()));
         identifyRepository.save(sysAccount);
    }

}
