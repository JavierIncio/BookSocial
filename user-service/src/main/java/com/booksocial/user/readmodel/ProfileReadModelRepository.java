package com.booksocial.user.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileReadModelRepository extends MongoRepository<ProfileReadModel, String> {
    Optional<ProfileReadModel> findByUserId(Long userId);
    List<ProfileReadModel> findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String displayName, String email);
}