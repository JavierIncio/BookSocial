package com.booksocial.notification.service;

import com.booksocial.notification.readmodel.NotificationReadModel;
import com.booksocial.notification.readmodel.NotificationReadModelRepository;
import com.booksocial.notification.web.dto.NotificationResponse;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationReadModelRepository notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final MongoTemplate mongoTemplate;

    public NotificationService(NotificationReadModelRepository notificationRepo,
                               SimpMessagingTemplate messagingTemplate,
                               MongoTemplate mongoTemplate) {
        this.notificationRepo = notificationRepo;
        this.messagingTemplate = messagingTemplate;
        this.mongoTemplate = mongoTemplate;
    }

    public NotificationResponse createFollowNotification(Long followerId, Long followeeId) {
        String notificationId = "FOLLOW:" + followerId;
        NotificationReadModel n = new NotificationReadModel(
                notificationId, followeeId, "FOLLOW",
                Map.of("followerId", followerId), false);
        notificationRepo.save(n);
        messagingTemplate.convertAndSend("/topic/notifications/" + followeeId, toResponse(n));
        return toResponse(n);
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
                        Criteria.where("userId").is(userId).and("read").is(false)),
                new Update().set("read", true),
                NotificationReadModel.class
        );
    }

    private NotificationResponse toResponse(NotificationReadModel n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getPayload(),
                n.isRead(),
                n.getOccurredAt()
        );
    }



}
