package com.booksocial.notification.service;

import com.booksocial.notification.events.ReviewCreatedEvent;
import com.booksocial.notification.readmodel.FollowerIndexReadModel;
import com.booksocial.notification.readmodel.FollowerIndexReadModelRepository;
import com.booksocial.notification.readmodel.NotificationReadModel;
import com.booksocial.notification.readmodel.NotificationReadModelRepository;
import com.booksocial.notification.web.dto.NotificationResponse;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationReadModelRepository notificationRepo;
    private final FollowerIndexReadModelRepository followerIndexRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final MongoTemplate mongoTemplate;

    public NotificationService(NotificationReadModelRepository notificationRepo,
                               FollowerIndexReadModelRepository followerIndexRepo,
                               SimpMessagingTemplate messagingTemplate,
                               MongoTemplate mongoTemplate) {
        this.notificationRepo = notificationRepo;
        this.followerIndexRepo = followerIndexRepo;
        this.messagingTemplate = messagingTemplate;
        this.mongoTemplate = mongoTemplate;
    }

    public List<NotificationResponse> listNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByOccurredAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public long unreadCount(Long userId) {
        return notificationRepo.countByUserIdAndReadFalse(userId);
    }

    public void markAllAsRead(Long userId) {
        mongoTemplate.updateMulti(Query.query(
                        Criteria.where("userId").is(userId)
                                .and("read").is(false)),
                new Update().set("read", true),
                NotificationReadModel.class
        );
    }

    public void handleFollowed(Long followerId, Long followeeId) {
        createFollowNotification(followerId, followeeId);
        addFollower(followerId, followeeId);
    }

    public void handleUnfollowed(Long followerId, Long followeeId) {
        removeFollower(followerId, followeeId);
    }

    public void handleReviewCreated(ReviewCreatedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actorUserId", event.actorUserId());
        payload.put("bookIsbn", event.bookIsbn());
        payload.put("title", event.title());
        payload.put("authorName", event.authorName());
        payload.put("rating", event.rating());
        if (event.comment() != null) {
            payload.put("comment", event.comment());
        }

        followerIndexRepo.findById(String.valueOf(event.actorUserId()))
                .ifPresent(index ->
                    index.getFollowers().forEach(followerId ->
                        createReviewNotification(followerId, event.reviewId(), payload)));
    }

    private void createFollowNotification(Long followerId, Long followeeId) {
        String notificationId = "FOLLOW:" + followerId;
        NotificationReadModel n = new NotificationReadModel(
                notificationId, followeeId, "FOLLOW",
                Map.of("followerId", followerId), false);
        notificationRepo.save(n);
        messagingTemplate.convertAndSend("/topic/notifications/" + followeeId, toResponse(n));
    }

    private void createReviewNotification(Long followerId, Long reviewId, Map<String, Object> payload) {
        String notificationId = "REVIEW:" + reviewId;
        NotificationReadModel n = new NotificationReadModel(
                notificationId, followerId, "REVIEW", payload, false);
        notificationRepo.save(n);
        messagingTemplate.convertAndSend("/topic/notifications/" + followerId, toResponse(n));
    }

    private void addFollower(Long followerId, Long followeeId) {
        FollowerIndexReadModel index = followerIndexRepo
                .findById(String.valueOf(followeeId))
                .orElseGet(() -> new FollowerIndexReadModel(
                        String.valueOf(followeeId), followeeId, new ArrayList<>()));

        if (!index.getFollowers().contains(followerId))
            index.getFollowers().add(followerId);

        followerIndexRepo.save(index);
    }

    private void removeFollower(Long followerId, Long followeeId) {
        followerIndexRepo.findById(String.valueOf(followeeId))
                .ifPresent(index -> {
                    index.getFollowers().remove(followerId);
                    followerIndexRepo.save(index);
                });
    }

    private NotificationResponse toResponse(NotificationReadModel n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getPayload(),
                n.isRead(), n.getOccurredAt());
    }
}
