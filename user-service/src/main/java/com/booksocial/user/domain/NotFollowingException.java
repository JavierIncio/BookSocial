package com.booksocial.user.domain;

public class NotFollowingException extends RuntimeException {
    public NotFollowingException(Long followerId, Long followeeId) {
        super("User " + followerId + " does not follow " + followeeId);
    }
}
