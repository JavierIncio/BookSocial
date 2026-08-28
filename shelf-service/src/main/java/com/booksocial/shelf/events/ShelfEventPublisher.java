package com.booksocial.shelf.events;

import com.booksocial.shelf.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShelfEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ShelfEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishChanged(ShelfChangedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.SHELF_CHANGED_KEY, event);
    }
}
