package com.booksocial.review.readmodel;

import com.booksocial.review.domain.Review;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "reviews")
public class ReviewReadModel {
    @Id
    private String id;
    private String bookIsbn;
    private Long userId;
    private int rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

    public ReviewReadModel() {}

    public ReviewReadModel(String id, String bookIsbn, Long userId, int rating, String comment, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.bookIsbn = bookIsbn;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ReviewReadModel(Review review) {
        this.id = review.getBookIsbn() + ":" + review.getUserId();
        this.bookIsbn = review.getBookIsbn();
        this.userId = review.getUserId();
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
