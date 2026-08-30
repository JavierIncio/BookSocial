package com.booksocial.identity.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super("Invalid token: " + message);
    }
}
