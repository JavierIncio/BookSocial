package com.booksocial.user.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "follows", uniqueConstraints = {@UniqueConstraint(columnNames = {"followerId", "followeeId"})})
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long followerId;

    @Column(nullable = false)
    private Long followeeId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Follow() {
    }

    public Follow(Long followerId, Long followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }

    public Long getId() {
        return id;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public Long getFolloweeId() {
        return followeeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
