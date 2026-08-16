package com.booksocial.review.domain;

public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException(String bookIsbn, Long userId) {
        super(String.format("Review already exists for book ISBN: %s and user ID: %d", bookIsbn, userId));
    }
}
