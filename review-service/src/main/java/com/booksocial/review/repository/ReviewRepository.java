package com.booksocial.review.repository;

import com.booksocial.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookIsbnAndUserId(String bookIsbn, Long userId);
}
