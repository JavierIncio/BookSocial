package com.booksocial.shelf.domain;

public class ShelfAlreadyExistsException extends RuntimeException {
    public ShelfAlreadyExistsException(String bookIsbn, Long userId) {
        super(String.format("Shelf already exists for book ISBN: %s and user ID: %d", bookIsbn, userId));
    }
}
