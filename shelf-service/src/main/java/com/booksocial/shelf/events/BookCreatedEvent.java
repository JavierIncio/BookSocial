package com.booksocial.shelf.events;

import java.time.Instant;

public record BookCreatedEvent(String bookIsbn,
                               String title,
                               String author,
                               Instant occurredAt) {}
