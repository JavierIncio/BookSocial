package com.booksocial.review.events;

import java.time.Instant;

public record BookCreatedEvent(String bookIsbn, String title, String author, Instant occurredAt) {
}
