package com.booksocial.shelf.events;

import java.time.Instant;

public record BookCreatedEvent(String bookIsbn,
                               String title,
                               String authorName,
                               String authorId,
                               Instant occurredAt) {}
