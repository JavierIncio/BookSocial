package com.booksocial.user.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 60) String displayName,
        @Size(max = 200) String bio,
        @Size(max = 80) String location,
        @Size(max = 500) String avatarUrl
) {
}
