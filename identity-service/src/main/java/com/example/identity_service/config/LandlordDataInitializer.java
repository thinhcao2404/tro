package com.example.identity_service.config;

import com.example.identity_service.entity.SysAccount;
import com.example.identity_service.enums.Role;
import com.example.identity_service.repository.SysAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LandlordDataInitializer implements ApplicationRunner {

    private final SysAccountRepository sysAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.landlord.username:landlord}")
    private String username;

    @Value("${app.seed.landlord.password:Landlord@123}")
    private String password;

    @Value("${app.seed.landlord.full-name:Chủ trọ}")
    private String fullName;

    @Value("${app.seed.landlord.email:landlord@qltro.local}")
    private String email;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (sysAccountRepository.existsByUsername(username)) {
            return;
        }

        SysAccount landlord = SysAccount.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(Role.LANDLORD)
                .email(email)
                .mustChangePassword(false)
                .isActive(true)
                .build();

        sysAccountRepository.save(landlord);
        log.info("Đã khởi tạo tài khoản chủ trọ mặc định: username={}", username);
    }
}
