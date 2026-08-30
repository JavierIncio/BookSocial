package com.booksocial.identity.exception;

public class ExpiredTokenException extends RuntimeException {
    public ExpiredTokenException(String message) {
        super("Token has expired: " + message);
    }
}
