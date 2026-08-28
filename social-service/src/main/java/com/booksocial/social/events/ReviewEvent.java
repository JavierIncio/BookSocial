package com.booksocial.social.events;

public sealed interface ReviewEvent
        permits ReviewCreatedEvent, ReviewUpdatedEvent {

    String bookIsbn();
    String title();
    String authorName();
    Integer rating();
    String comment();
}
