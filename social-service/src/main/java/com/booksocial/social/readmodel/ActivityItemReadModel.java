package com.booksocial.social.readmodel;

import com.booksocial.social.domain.ActivityType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "activity_items")
public class ActivityItemReadModel {
    @Id
    private String id;
    private ActivityType type;
    private Long actorId;
    private Map<String, Object> payload;  // title, authorName, rating, status, targetUserId...
    private Instant occurredAt;

    public ActivityItemReadModel() {}

    public ActivityItemReadModel(String id, ActivityType type, Long actorId,
                                 Map<String, Object> payload) {
        this.id = id;
        this.type = type;
        this.actorId = actorId;
        this.payload = payload;
        this.occurredAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}

