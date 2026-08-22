package com.booksocial.book.events;

import java.time.Instant;

public record BookCreatedEvent(
        String bookIsbn,
        String title,
        String authorName,
        String authorId,
        Instant occurredAt) {
    public BookCreatedEvent(String bookIsbn, String title, String authorName, String authorId) {
        this(bookIsbn, title, authorName, authorId, Instant.now());
    }
}
