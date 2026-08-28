package com.booksocial.social.web.dto;

import java.time.Instant;
import java.util.Map;

public record FeedItemResponse(
        String activityId,
        String type,
        Long actorId,
        Map<String, Object> payload,
        Instant occurredAt
) {
}
