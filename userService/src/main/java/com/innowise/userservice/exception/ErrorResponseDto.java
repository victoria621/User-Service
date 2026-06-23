package com.innowise.userservice.exception;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        String message,
        int statusCode,
        String error,
        String path,
        LocalDateTime timestamp
) {
}
