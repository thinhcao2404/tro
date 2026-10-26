package com.example.identity_service.service;

import com.example.identity_service.dto.request.LoginRequest;
import com.example.identity_service.dto.request.RefreshTokenRequest;
import com.example.identity_service.dto.response.AuthResponse;
import com.example.identity_service.dto.response.TokenRefreshResponse;
import com.example.identity_service.entity.RefreshToken;
import com.example.identity_service.entity.SysAccount;
import com.example.identity_service.exception.BadRequestException;
import com.example.identity_service.mapper.IdentifyMapper;
import com.example.identity_service.repository.IdentifyRepository;
import com.example.identity_service.repository.RefeshTokeRepository;
import com.example.identity_service.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class AuthenticationService {
    @Autowired
    private IdentifyRepository identifyRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private IdentifyMapper  identifyMapper;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RefeshTokeRepository  refeshTokeRepository;

    @Transactional
    public AuthResponse login(LoginRequest request){

        SysAccount sysAccount = identifyRepository.findByUsername(request.getUsername())
                .orElseThrow(()-> new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác. Vui lòng thử lại"));
        boolean isPasswordMatched = passwordEncoder.matches(request.getPassword(),sysAccount.getPasswordHash());
        if(!isPasswordMatched){
            throw new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác. Vui lòng thử lại");
        }
        if(!sysAccount.getIsActive()){
            throw new BadRequestException("Tài khoản này hiện đang bị khóa. Vui lòng liên hệ quản trị viên");
        }
        String accessToken = jwtTokenProvider.generateAccessToken(sysAccount);
        String refreshToken = jwtTokenProvider.generateRefreshToken(sysAccount);
        AuthResponse response = identifyMapper.toAuthResponse(sysAccount);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        saveRefreshToken(sysAccount,refreshToken);
        return response;
    }

    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request){
        RefreshToken refreshToken = refeshTokeRepository.findByToken(request.getRefreshToken())
                .orElseThrow(()-> new BadRequestException("Refresh Token không hợp lệ hoặc không tồn tại"));

        if(refreshToken.getExpiryDate().isBefore(Instant.now())){
            refeshTokeRepository.delete(refreshToken);
            throw new BadRequestException("Refresh Token đã hết hạn.");
        }

        SysAccount sysAccount = refreshToken.getAccount();
        String newAccessToken = jwtTokenProvider.generateAccessToken(sysAccount);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(sysAccount);

        saveRefreshToken(sysAccount,newRefreshToken);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private void saveRefreshToken(SysAccount sysAccount,String refreshToken){
        refeshTokeRepository.deleteByAccount(sysAccount);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .account(sysAccount)
                .token(refreshToken)
                .isRevoked(false)
                .createdAt(LocalDateTime.now())
                .expiryDate(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .build();
        refeshTokeRepository.save(newRefreshToken);
    }
}
