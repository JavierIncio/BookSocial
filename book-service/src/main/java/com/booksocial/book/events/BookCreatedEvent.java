package com.booksocial.book.events;

import java.time.Instant;

public record BookCreatedEvent(String bookIsbn, String title, String author, Instant occurredAt) {
    public BookCreatedEvent(String bookIsbn, String title, String author) {
        this(bookIsbn, title, author, Instant.now());
    }
}
