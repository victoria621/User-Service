package com.innowise.userService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

public record UserRequestDTO(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Surname is required")
        String surname,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) implements Serializable {
}
