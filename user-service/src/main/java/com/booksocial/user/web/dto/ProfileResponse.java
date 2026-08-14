package com.booksocial.user.web.dto;

import java.time.Instant;

public record ProfileResponse(
        Long userId,
        String email,
        String displayName,
        String bio,
        String location,
        String avatarUrl,
        long followersCount,
        long followingCount,
        int postsCount,
        Instant createdAt,
        Instant updatedAt

) {}
