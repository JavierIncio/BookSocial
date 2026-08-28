package com.booksocial.social.events;

import java.time.Instant;

public record ShelfChangedEvent(
        Long userId,
        String bookIsbn,
        String title,
        String authorName,
        String status,
        Instant occurredAt) {}
