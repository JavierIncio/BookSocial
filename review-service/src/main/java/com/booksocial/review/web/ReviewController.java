package com.booksocial.review.web;

import com.booksocial.review.service.ReviewService;
import com.booksocial.review.web.dto.CreateReviewRequest;
import com.booksocial.review.web.dto.ReviewResponse;
import com.booksocial.review.web.dto.ReviewSummaryResponse;
import com.booksocial.review.web.dto.UpdateReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ReviewResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String bookIsbn,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(userId, bookIsbn, request));
    }

    @PutMapping("/{bookIsbn}")
    public ResponseEntity<ReviewResponse> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String bookIsbn,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.update(userId, bookIsbn, request));
    }

    @GetMapping("/books/{bookIsbn}")
    public ResponseEntity<List<ReviewResponse>> listByBook(@PathVariable String bookIsbn) {
        return ResponseEntity.ok(reviewService.listByBook(bookIsbn));
    }

    @GetMapping("/books/{bookIsbn}/summary")
    public ResponseEntity<ReviewSummaryResponse> summary(@PathVariable String bookIsbn) {
        return ResponseEntity.ok(reviewService.summary(bookIsbn));
    }

}
