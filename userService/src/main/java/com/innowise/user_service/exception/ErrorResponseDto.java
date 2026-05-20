package com.innowise.user_service.exception;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        String message,
        int statusCode,
        String error,
        String path,
        LocalDateTime timestamp
) {
}
