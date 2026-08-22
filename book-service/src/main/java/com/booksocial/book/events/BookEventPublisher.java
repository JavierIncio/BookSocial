package com.booksocial.book.events;

import com.booksocial.book.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public BookEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishBookCreated(String bookIsbn, String title, String authorName, String authorId) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.BOOK_CREATED_KEY,
                new BookCreatedEvent(bookIsbn, title, authorName, authorId)
        );
    }
}
