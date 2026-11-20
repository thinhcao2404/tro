package com.example.payment_service.util;

import com.example.payment_service.dto.response.PageResponse;
import org.springframework.data.domain.Page;

public final class PageResponseUtil {

    private PageResponseUtil() {}

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
