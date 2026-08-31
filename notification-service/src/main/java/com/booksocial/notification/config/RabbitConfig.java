package com.booksocial.notification.config;

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

    public static final String FOLLOWED_QUEUE = "notification-service.follows.followed";
    public static final String FOLLOWED_KEY = "follow.followed";
    public static final String UNFOLLOWED_QUEUE = "notification-service.follows.unfollowed";
    public static final String UNFOLLOWED_KEY = "follow.unfollowed";

    public static final String REVIEW_CREATED_QUEUE = "notification-service.reviews.created";
    public static final String REVIEW_CREATED_KEY = "review.created";


    // EXCHANGE
    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // FOLLOWS
    @Bean
    public Queue followedQueue() {
        return new Queue(FOLLOWED_QUEUE, true);
    }

    @Bean
    public Binding followedBinding() {
        return BindingBuilder.bind(followedQueue()).to(eventsExchange()).with(FOLLOWED_KEY);
    }

    @Bean
    public Queue unfollowedQueue() {
        return new Queue(UNFOLLOWED_QUEUE, true);
    }

    @Bean
    public Binding unfollowedBinding() {
        return BindingBuilder.bind(unfollowedQueue()).to(eventsExchange()).with(UNFOLLOWED_KEY);
    }

    // REVIEWS
    @Bean
    public Queue reviewCreatedQueue() {
        return new Queue(REVIEW_CREATED_QUEUE, true);
    }

    @Bean
    public Binding reviewCreatedBinding() {
        return BindingBuilder.bind(reviewCreatedQueue()).to(eventsExchange()).with(REVIEW_CREATED_KEY);
    }

    // Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter("com.booksocial.notification.events");
    }
}
