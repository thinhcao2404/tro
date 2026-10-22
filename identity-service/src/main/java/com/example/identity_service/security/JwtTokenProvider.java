package com.example.identity_service.security;

import com.example.identity_service.entity.SysAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms}")
    private long jwtAccessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long jwtRefreshTokenExpirationMs;

    private SecretKey getSigningKey(){
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    public String  generateAccessToken(SysAccount sysAccount) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtAccessTokenExpirationMs);

        return  Jwts.builder()
                .setSubject(sysAccount.getUsername())
                .claim("accountId", sysAccount.getId())
                .claim("role", sysAccount.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(SysAccount sysAccount) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshTokenExpirationMs);

        return Jwts.builder()
                .setSubject(sysAccount.getUsername())
                .claim("accountId",sysAccount.getId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(),SignatureAlgorithm.HS256)
                .compact();
    }

    public long getRefreshTokenExpirationMs() {
        return jwtRefreshTokenExpirationMs;
    }
}
