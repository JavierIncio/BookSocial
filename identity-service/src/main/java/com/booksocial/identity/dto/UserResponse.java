package com.booksocial.identity.dto;

import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Integer age,
        Set<String> roles
) {
}
