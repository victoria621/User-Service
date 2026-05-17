package com.innowise.userService.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CardResponseDTO(
        @NotNull
        Long id,
        @NotBlank(message = "Number is required")
        @Pattern(regexp = "\\d{16}")
        String number,
        @NotBlank(message = "Holder is required")
        String holder,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate expirationDate,
        Boolean active,
        @NotNull
        Long userId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdAt,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime updatedAt
        ) {
}
