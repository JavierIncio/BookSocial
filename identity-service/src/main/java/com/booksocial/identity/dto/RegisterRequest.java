package com.booksocial.identity.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotNull
        @Past
        LocalDate birthDate
) {
}
