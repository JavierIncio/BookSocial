package com.booksocial.social.readmodel;

import com.booksocial.social.events.FollowedEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "followers")
public class FollowerIndexReadModel {
    @Id
    private String id;
    private Long userId;
    private List<Long> followers;

    public FollowerIndexReadModel() {}

    public FollowerIndexReadModel(String id, Long userId, List<Long> followers) {
        this.id = id;
        this.userId = userId;
        this.followers = followers;
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

    public List<Long> getFollowers() {
        return followers;
    }

    public void setFollowers(List<Long> followers) {
        this.followers = followers;
    }
}
