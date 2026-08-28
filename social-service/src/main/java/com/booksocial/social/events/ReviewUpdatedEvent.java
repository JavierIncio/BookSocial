package com.booksocial.social.events;

import java.time.Instant;

public record ReviewUpdatedEvent(
        Long reviewId,
        String bookIsbn,
        String title,
        String authorName,
        Integer rating,
        String comment,
        Long actorUserId,
        Instant occurredAt
) implements ReviewEvent {}
