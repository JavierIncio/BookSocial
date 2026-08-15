package com.booksocial.user.domain;

public class SelfFollowException extends RuntimeException {
    public SelfFollowException() {
        super("Cannot follow yourself");
    }
}
