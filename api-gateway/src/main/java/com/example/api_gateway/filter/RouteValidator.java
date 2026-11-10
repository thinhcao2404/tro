package com.example.api_gateway.filter;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class RouteValidator {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Các endpoint công khai
    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/auth/**",
            "/eureka/**",
            "/actuator/**"
    );

    public boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();


        for (String openEndpoint : OPEN_API_ENDPOINTS) {
            if (pathMatcher.match(openEndpoint, path)) {
                return false;
            }
        }


        if (HttpMethod.GET.equals(method) && pathMatcher.match("/api/v1/rooms/**", path)) {
            return false;
        }

        return true;
    }
}
