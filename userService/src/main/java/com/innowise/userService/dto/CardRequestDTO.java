package com.innowise.userService.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;

import java.io.Serializable;
import java.time.LocalDate;

public record CardRequestDTO (
        @NotBlank(message = "Number is required")
        @Pattern(regexp = "\\d{16}")
        String number,
        @NotBlank(message = "Holder is required")
        String holder,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Future
        LocalDate expirationDate
) implements Serializable {
}
