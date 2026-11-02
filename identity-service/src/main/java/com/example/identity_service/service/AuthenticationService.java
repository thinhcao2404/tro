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
    private RefeshTokeRepository  refeshTokenRepository;

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
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new BadRequestException("Refresh Token bị lỗi cấu trúc hoặc chữ ký không hợp lệ.");
        }

        String usernameFromToken = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());

        RefreshToken refreshToken = refeshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(()-> new BadRequestException("Refresh Token không hợp lệ hoặc không tồn tại"));

        if (Boolean.TRUE.equals(refreshToken.getIsRevoked())) {
            throw new BadRequestException("Refresh Token đã bị thu hồi. Vui lòng đăng nhập lại.");
        }

        if(refreshToken.getExpiryDate().isBefore(Instant.now())){
            refeshTokenRepository.delete(refreshToken);
            throw new BadRequestException("Refresh Token đã hết hạn.");
        }

        SysAccount sysAccount = refreshToken.getAccount();

        if (!sysAccount.getUsername().equals(usernameFromToken)) {
            throw new BadRequestException("Thông tin định danh trong Refresh Token không hợp lệ.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(sysAccount);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(sysAccount);

        refeshTokenRepository.delete(refreshToken);
        saveRefreshToken(sysAccount,newRefreshToken);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private void saveRefreshToken(SysAccount sysAccount,String refreshToken){
        refeshTokenRepository.deleteByAccount(sysAccount);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .account(sysAccount)
                .token(refreshToken)
                .isRevoked(false)
                .createdAt(LocalDateTime.now())
                .expiryDate(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .build();
        refeshTokenRepository.save(newRefreshToken);
    }

    @Transactional
    public void logout(String refreshTokenStr){
        RefreshToken refreshToken = refeshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new BadRequestException("Refresh Token không hợp lệ."));

        refreshToken.setIsRevoked(true);
        refeshTokenRepository.save(refreshToken);
    }
}
