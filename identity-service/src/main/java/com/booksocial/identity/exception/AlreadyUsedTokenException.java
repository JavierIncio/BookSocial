package com.booksocial.identity.exception;

public class AlreadyUsedTokenException extends RuntimeException {
    public AlreadyUsedTokenException(String message) {
        super("Token has already been used: " + message);
    }
}
