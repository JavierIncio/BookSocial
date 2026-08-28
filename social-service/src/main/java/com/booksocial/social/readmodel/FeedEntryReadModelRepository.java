package com.booksocial.social.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FeedEntryReadModelRepository extends MongoRepository<FeedEntryReadModel, String> {
    Optional<FeedEntryReadModel> findByFeedUserIdOrderByOccurredAtDesc(Long feedUserId);

}
