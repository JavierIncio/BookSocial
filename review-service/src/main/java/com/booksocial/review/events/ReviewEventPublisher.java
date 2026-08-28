package com.booksocial.review.events;

import com.booksocial.review.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ReviewEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCreated(ReviewCreatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.REVIEW_CREATED_KEY,
                event);
    }

    public void publishUpdated(ReviewUpdatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.REVIEW_UPDATED_KEY,
                event);
    }
}