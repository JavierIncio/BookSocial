package com.booksocial.review.domain;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(String bookIsbn, Long userId) {
        super(String.format("Review not found for book ISBN: %s and user ID: %d", bookIsbn, userId));
    }
}
