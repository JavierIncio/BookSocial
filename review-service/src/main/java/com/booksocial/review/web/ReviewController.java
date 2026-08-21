package com.booksocial.review.web;

import com.booksocial.review.service.ReviewService;
import com.booksocial.review.web.dto.CreateReviewRequest;
import com.booksocial.review.web.dto.ReviewResponse;
import com.booksocial.review.web.dto.ReviewSummaryResponse;
import com.booksocial.review.web.dto.UpdateReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/{bookIsbn}")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String bookIsbn,
            @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(userId, bookIsbn, request);
    }

    @PutMapping("/{bookIsbn}")
    public ReviewResponse update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String bookIsbn,
            @Valid @RequestBody UpdateReviewRequest request) {
        return reviewService.update(userId, bookIsbn, request);
    }

    @GetMapping("/books/{bookIsbn}")
    public List<ReviewResponse> listByBook(@PathVariable String bookIsbn) {
        return reviewService.listByBook(bookIsbn);
    }

    @GetMapping("/books/{bookIsbn}/summary")
    public ReviewSummaryResponse summary(@PathVariable String bookIsbn) {
        return reviewService.summary(bookIsbn);
    }

    @GetMapping("/me")
    public List<ReviewResponse> myReviews(@RequestHeader("X-User-Id") Long userId) {
        return reviewService.listByUser(userId);
    }

    @GetMapping("/users/{userId}")
    public List<ReviewResponse> userReviews(@PathVariable Long userId) {
        return reviewService.listByUser(userId);
    }
}
