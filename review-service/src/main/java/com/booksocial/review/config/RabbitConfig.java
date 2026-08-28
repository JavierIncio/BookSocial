package com.booksocial.review.config;

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
    public static final String REVIEW_QUEUE = "review-service.books.created";
    public static final String REVIEW_KEY = "book.created";
    public static final String REVIEW_CREATED_KEY = "review.created";
    public static final String REVIEW_UPDATED_KEY = "review.updated";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue reviewQueue() {
        return new Queue(REVIEW_QUEUE, true);
    }

    @Bean
    public Binding reviewBinding() {
        return BindingBuilder.bind(reviewQueue()).to(eventsExchange()).with(REVIEW_KEY);
    }


    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("com.booksocial.review.events");
    }
}
