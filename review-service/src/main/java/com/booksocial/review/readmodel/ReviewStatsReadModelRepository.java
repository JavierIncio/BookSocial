package com.booksocial.review.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReviewStatsReadModelRepository extends MongoRepository<ReviewStatsReadModel, String> {
    Optional<ReviewStatsReadModel> findByBookIsbn(String bookIsbn);
}
