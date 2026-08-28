package com.booksocial.social.events;

import com.booksocial.social.config.RabbitConfig;
import com.booksocial.social.service.FeedService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventConsumer {
    private final FeedService feedService;

    public ReviewEventConsumer(FeedService feedService) {
        this.feedService = feedService;
    }

    @RabbitListener(queues = RabbitConfig.REVIEW_CREATED_QUEUE)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        feedService.handleReviewCreated(event);
    }

    @RabbitListener(queues = RabbitConfig.REVIEW_UPDATED_QUEUE)
    public void handleReviewUpdated(ReviewUpdatedEvent event) {
        feedService.handleReviewUpdated(event);
    }
}
