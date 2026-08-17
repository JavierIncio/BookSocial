package com.booksocial.shelf.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "booksocial.events";
    public static final String SHELF_QUEUE = "shelf-service.books.created";
    public static final String SHELF_KEY = "book.created";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue shelfQueue() {
        return new Queue(SHELF_QUEUE, true);
    }

    @Bean
    public Binding shelfBinding() {
        return BindingBuilder.bind(shelfQueue()).to(eventsExchange()).with(SHELF_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter("com.booksocial.shelf.events");
    }
}
