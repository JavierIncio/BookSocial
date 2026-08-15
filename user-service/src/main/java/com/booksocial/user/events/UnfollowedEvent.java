package com.booksocial.user.events;

import java.time.Instant;

public record UnfollowedEvent(Long followerId, Long followeeId, Instant occurredAt) {
    public UnfollowedEvent(Long followerId, Long followeeId) {
        this(followerId, followeeId, Instant.now());
    }
}