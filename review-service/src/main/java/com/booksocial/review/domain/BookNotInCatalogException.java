package com.booksocial.review.domain;

public class BookNotInCatalogException extends RuntimeException {
    public BookNotInCatalogException(String isbn) {
        super(String.format("Book with ISBN: %s is not in the catalog", isbn));
    }
}
