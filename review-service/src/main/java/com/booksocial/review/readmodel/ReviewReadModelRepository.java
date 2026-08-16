package com.booksocial.review.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewReadModelRepository extends MongoRepository<ReviewReadModel, String> {
    List<ReviewReadModel> findByBookIsbnOrderByCreatedAtDesc(String bookIsbn);
    Optional<ReviewReadModel> findByBookIsbnAndUserId(String bookIsbn, Long userId);
    boolean existsByBookIsbnAndUserId(String bookIsbn, Long userId);
}
