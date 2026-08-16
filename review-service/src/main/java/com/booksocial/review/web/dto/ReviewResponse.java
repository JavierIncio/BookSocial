package com.booksocial.review.web.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id, String bookIsbn, Long userId,
        int rating, String comment,
        Instant createdAt, Instant updatedAt) {}