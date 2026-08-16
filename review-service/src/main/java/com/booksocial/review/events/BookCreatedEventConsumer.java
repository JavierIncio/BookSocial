package com.booksocial.review.events;

import com.booksocial.review.config.RabbitConfig;
import com.booksocial.review.readmodel.BookRefReadModel;
import com.booksocial.review.readmodel.BookRefReadModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
public class BookCreatedEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(BookCreatedEventConsumer.class);

    private final BookRefReadModelRepository bookRefRepository;

    public BookCreatedEventConsumer(BookRefReadModelRepository bookRefRepository) {
        this.bookRefRepository = bookRefRepository;
    }

    @RabbitListener(queues = RabbitConfig.REVIEW_QUEUE)
    public void onBookCreated(BookCreatedEvent event) {
        bookRefRepository.save(new BookRefReadModel(event.bookIsbn(), event.title(), event.author()));
        log.info("Processed BookCreatedEvent: {} - {}", event.bookIsbn(), event.title());
    }
}
