package com.booksocial.notification.events;

import com.booksocial.notification.config.RabbitConfig;
import com.booksocial.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FollowEventConsumer {
    private final NotificationService notificationService;

    public FollowEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.FOLLOWED_QUEUE)
    public void handleFollowed(FollowedEvent event) {
        notificationService.createFollowNotification(event.followerId(), event.followeeId());
    }
}
