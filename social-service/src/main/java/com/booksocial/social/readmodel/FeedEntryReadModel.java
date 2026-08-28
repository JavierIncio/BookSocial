package com.booksocial.social.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "feed_entries")
public class FeedEntryReadModel {
    @Id
    private String id;
    private Long feedUserId;
    private String activityId;
    private Instant occurredAt;

    public FeedEntryReadModel() {}

    public FeedEntryReadModel(String id, Long feedUserId, String activityId) {
        this.id = feedUserId + ":" + activityId;
        this.feedUserId = feedUserId;
        this.activityId = activityId;
        this.occurredAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getFeedUserId() {
        return feedUserId;
    }

    public void setFeedUserId(Long feedUserId) {
        this.feedUserId = feedUserId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
