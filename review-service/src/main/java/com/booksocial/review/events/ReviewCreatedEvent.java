package com.booksocial.review.events;

import java.time.Instant;

public record ReviewCreatedEvent(
        Long reviewId,
        String bookIsbn,
        String title,
        String authorName,
        Integer rating,
        String comment,
        Long actorUserId,
        Instant occurredAt
) {}
