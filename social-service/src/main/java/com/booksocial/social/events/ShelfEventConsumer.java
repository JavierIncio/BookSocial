package com.booksocial.social.events;

import com.booksocial.social.config.RabbitConfig;
import com.booksocial.social.service.FeedService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ShelfEventConsumer {
    private final FeedService feedService;

    public ShelfEventConsumer(FeedService feedService) {
        this.feedService = feedService;
    }

    @RabbitListener(queues = RabbitConfig.SHELF_QUEUE)
    public void handleShelfEvent(ShelfChangedEvent event) {
        feedService.handleShelfChanged(event);
    }

}
