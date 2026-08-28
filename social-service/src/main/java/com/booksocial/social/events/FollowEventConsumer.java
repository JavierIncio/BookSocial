package com.booksocial.social.events;

import com.booksocial.social.config.RabbitConfig;
import com.booksocial.social.service.FeedService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FollowEventConsumer {
    private final FeedService feedService;

    public FollowEventConsumer(FeedService feedService) {
        this.feedService = feedService;
    }

    @RabbitListener(queues = RabbitConfig.FOLLOWED_QUEUE)
    public void handleFollowed(FollowedEvent event) {
        feedService.handleFollowed(event.followerId(), event.followeeId());
    }

    @RabbitListener(queues = RabbitConfig.UNFOLLOWED_QUEUE)
    public void handleUnfollowed(UnfollowedEvent event) {
        feedService.handleUnfollowed(event.followerId(), event.followeeId());
    }
}
