package com.booksocial.review.service;

import com.booksocial.review.domain.BookNotInCatalogException;
import com.booksocial.review.domain.Review;
import com.booksocial.review.domain.ReviewAlreadyExistsException;
import com.booksocial.review.domain.ReviewNotFoundException;
import com.booksocial.review.readmodel.*;
import com.booksocial.review.repository.ReviewRepository;
import com.booksocial.review.web.dto.CreateReviewRequest;
import com.booksocial.review.web.dto.ReviewResponse;
import com.booksocial.review.web.dto.ReviewSummaryResponse;
import com.booksocial.review.web.dto.UpdateReviewRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


@Service
@Transactional
public class ReviewService {
    private final BookRefReadModelRepository bookRefRepo;
    private final ReviewRepository reviewRepo;
    private final ReviewReadModelRepository readModelRepo;
    private final ReviewStatsReadModelRepository statsRepo;

    public ReviewService(BookRefReadModelRepository bookRefRepo, ReviewRepository reviewRepo, ReviewReadModelRepository readModelRepo, ReviewStatsReadModelRepository statsRepo) {
        this.bookRefRepo = bookRefRepo;
        this.reviewRepo = reviewRepo;
        this.readModelRepo = readModelRepo;
        this.statsRepo = statsRepo;
    }

    public ReviewResponse create(Long userId, String bookIsbn, CreateReviewRequest req) {
        if (!bookRefRepo.existsById(bookIsbn))
            throw new BookNotInCatalogException(bookIsbn);
        if (readModelRepo.existsByBookIsbnAndUserId(bookIsbn, userId))
            throw new ReviewAlreadyExistsException(bookIsbn, userId);
        Review review = reviewRepo.save(new Review(bookIsbn, userId, req.rating(), req.comment()));
        readModelRepo.save(new ReviewReadModel(review));  // upsert
        syncStats(bookIsbn);
        return toResponse(review);
    }

    public ReviewResponse update(Long userId, String bookIsbn, UpdateReviewRequest req) {
        Review review = reviewRepo.findByBookIsbnAndUserId(bookIsbn, userId)
                .orElseThrow(() -> new ReviewNotFoundException(bookIsbn, userId));
        if (req.rating() != null) review.setRating(req.rating());
        if (req.comment() != null) review.setComment(req.comment());
        review.setUpdatedAt(Instant.now());
        reviewRepo.save(review);
        readModelRepo.save(new ReviewReadModel(review));
        syncStats(bookIsbn);
        return toResponse(review);
    }

    public List<ReviewResponse> listByBook(String bookIsbn) {
        return readModelRepo.findByBookIsbnOrderByCreatedAtDesc(bookIsbn)
                .stream().map(this::toResponse).toList();
    }

    public ReviewSummaryResponse summary(String bookIsbn) {
        return statsRepo.findByBookIsbn(bookIsbn)
                .map(s -> new ReviewSummaryResponse(s.getBookIsbn(), s.getRatingCount(), s.getAverageRating()))
                .orElse(new ReviewSummaryResponse(bookIsbn, 0, 0.0));
    }

    private void syncStats(String bookIsbn) {
        List<ReviewReadModel> reviews = readModelRepo.findByBookIsbnOrderByCreatedAtDesc(bookIsbn);
        double avg = reviews.stream().mapToInt(ReviewReadModel::getRating).average().orElse(0.0);
        statsRepo.save(new ReviewStatsReadModel(bookIsbn, reviews.size(), avg));
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(), review.getBookIsbn(), review.getUserId(),
                review.getRating(), review.getComment(),
                review.getCreatedAt(), review.getUpdatedAt()
        );
    }

    private ReviewResponse toResponse(ReviewReadModel rm) {
        return new ReviewResponse(
                null, rm.getBookIsbn(), rm.getUserId(),
                rm.getRating(), rm.getComment(),
                rm.getCreatedAt(), rm.getUpdatedAt()
        );
    }
}