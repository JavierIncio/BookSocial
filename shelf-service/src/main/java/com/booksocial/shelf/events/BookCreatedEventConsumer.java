package com.booksocial.shelf.events;

import com.booksocial.shelf.config.RabbitConfig;
import com.booksocial.shelf.readmodel.BookRefReadModel;
import com.booksocial.shelf.readmodel.BookRefReadModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BookCreatedEventConsumer {
    public static final Logger log = LoggerFactory.getLogger(BookCreatedEventConsumer.class);

    private final BookRefReadModelRepository readModelRepository;

    public BookCreatedEventConsumer(BookRefReadModelRepository readModelRepository) {
        this.readModelRepository = readModelRepository;
    }

    @RabbitListener(queues = RabbitConfig.SHELF_QUEUE)
    public void onBookCreated(BookCreatedEvent event) {
        readModelRepository.save(new BookRefReadModel(event.bookIsbn(), event.title(), event.authorName(), event.authorId()));
        log.info("Processed BookCreatedEvent: {} - {}", event.bookIsbn(), event.title());
    }
}
