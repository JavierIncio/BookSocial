package com.booksocial.shelf.domain;

public class ShelfNotFoundException extends RuntimeException {
    public ShelfNotFoundException(String bookIsbn, Long userId) {
        super(String.format("Shelf not found for book ISBN: %s and user ID: %d", bookIsbn, userId));
    }
}
