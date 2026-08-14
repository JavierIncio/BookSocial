package com.booksocial.user.domain;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(Long userId) {
        super("Profile not found for userId " + userId);
    }
}
