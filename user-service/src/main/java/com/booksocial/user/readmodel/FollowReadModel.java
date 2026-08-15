package com.booksocial.user.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "follows")
public class FollowReadModel {
    @Id
    private String id;

    private Long followerId;
    private Long followeeId;
    private Instant createdAt;

    public FollowReadModel() {
    }

    public FollowReadModel(Long followerId, Long followeeId) {
        this.id = followerId + ":"  + followeeId;
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public void setFollowerId(Long followerId) {
        this.followerId = followerId;
    }

    public Long getFolloweeId() {
        return followeeId;
    }

    public void setFolloweeId(Long followeeId) {
        this.followeeId = followeeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
