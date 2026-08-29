package com.booksocial.notification.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "notifications")
public class NotificationReadModel {
    @Id
    private String id;
    private Long userId;
    private String type;
    private Map<String, Object> payload;
    private boolean read;
    private Instant occurredAt;

    public NotificationReadModel() {}

    public NotificationReadModel(String id, Long userId, String type, Map<String, Object> payload, boolean read) {
        this.id = userId + ":" + id;
        this.userId = userId;
        this.type = type;
        this.payload = payload;
        this.read = read;
        this.occurredAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
