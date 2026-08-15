package com.booksocial.user.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "booksocial.events";
    public static final String FOLLOWED_QUEUE = "user-service.follows.followed";
    public static final String UNFOLLOWED_QUEUE = "user-service.follows.unfollowed";
    public static final String FOLLOWED_KEY = "follow.followed";
    public static final String UNFOLLOWED_KEY = "follow.unfollowed";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue followedQueue() {
        return new Queue(FOLLOWED_QUEUE, true);
    }

    @Bean
    public Queue unfollowedQueue() {
        return new Queue(UNFOLLOWED_QUEUE, true);
    }

    @Bean
    public Binding followedBinding() {
        return BindingBuilder.bind(followedQueue()).to(eventsExchange()).with(FOLLOWED_KEY);
    }

    @Bean
    public Binding unfollowedBinding() {
        return BindingBuilder.bind(unfollowedQueue()).to(eventsExchange()).with(UNFOLLOWED_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("com.booksocial.user.events");
    }
}
