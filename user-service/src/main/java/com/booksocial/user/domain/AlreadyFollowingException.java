package com.booksocial.user.domain;

public class AlreadyFollowingException extends RuntimeException {
    public AlreadyFollowingException(Long followerId, Long followeeId) {
        super("User " + followerId + " already follows " + followeeId);
    }
}
