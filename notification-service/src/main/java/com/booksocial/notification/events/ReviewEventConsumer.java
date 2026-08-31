package com.booksocial.notification.events;

import com.booksocial.notification.config.RabbitConfig;
import com.booksocial.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventConsumer {
    private final NotificationService notificationService;

    public ReviewEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.REVIEW_CREATED_QUEUE)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        notificationService.handleReviewCreated(event);
    }

}
