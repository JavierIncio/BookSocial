package com.booksocial.social.events;

import java.time.Instant;

public record FollowedEvent(Long followerId, Long followeeId, Instant occurredAt) {
    public FollowedEvent(Long followerId, Long followeeId) {
        this(followerId, followeeId, Instant.now());
    }
}
