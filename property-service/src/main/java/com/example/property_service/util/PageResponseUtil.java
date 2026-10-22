package com.example.property_service.util;

import com.example.property_service.dto.response.PageResponse;
import org.springframework.data.domain.Page;
public class PageResponseUtil {

    private PageResponseUtil() {
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}