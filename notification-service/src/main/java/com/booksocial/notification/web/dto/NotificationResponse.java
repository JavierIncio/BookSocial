package com.booksocial.notification.web.dto;

import java.time.Instant;
import java.util.Map;

public record NotificationResponse(
        String id,
        String type,
        Map<String,Object> payload,
        boolean read,
        Instant occurredAt
) {}
