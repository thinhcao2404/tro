package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final RouteValidator routeValidator;
    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Kiểm tra xem endpoint có cần xác thực hay không
        if (routeValidator.isSecured(request)) {
            // 2. Kiểm tra header Authorization
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || authHeader.isBlank()) {
                return onError(exchange, "Thiếu header Authorization", HttpStatus.UNAUTHORIZED);
            }

            if (!authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Header Authorization phải có tiền tố 'Bearer '", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // 3. Xác thực chữ ký và thời hạn token
            if (!jwtUtil.validateToken(token)) {
                return onError(exchange, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED);
            }

            // 4. Giải mã claims và chuyển tiếp thông tin user qua header cho downstream services
            try {
                Claims claims = jwtUtil.getClaims(token);
                String username = claims.getSubject();
                Object accountId = claims.get("accountId");
                Object role = claims.get("role");
                Object tenantId = claims.get("tenantId");

                ServerHttpRequest.Builder builder = request.mutate();

                if (username != null) {
                    builder.header("X-Username", username);
                }
                if (accountId != null) {
                    builder.header("X-Account-Id", String.valueOf(accountId));
                }
                if (role != null) {
                    builder.header("X-User-Role", String.valueOf(role));
                }
                if (tenantId != null) {
                    builder.header("X-Tenant-Id", String.valueOf(tenantId));
                }

                ServerHttpRequest mutatedRequest = builder.build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.error("Lỗi khi xử lý JWT Token: {}", e.getMessage());
                return onError(exchange, "Không thể giải mã thông tin token", HttpStatus.UNAUTHORIZED);
            }
        }

        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                Instant.now().toString(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                message
        );

        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
