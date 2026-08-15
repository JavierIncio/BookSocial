package com.booksocial.user.events;

import com.booksocial.user.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class FollowEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public FollowEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishFollowed(Long followerId, Long followeeId) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.FOLLOWED_KEY,
                new FollowedEvent(followerId, followeeId));
    }

    public void publishUnfollowed(Long followerId, Long followeeId) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.UNFOLLOWED_KEY,
                new UnfollowedEvent(followerId, followeeId));
    }
}