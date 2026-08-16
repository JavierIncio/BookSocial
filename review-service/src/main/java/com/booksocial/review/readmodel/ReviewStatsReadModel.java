package com.booksocial.review.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "review_stats")
public class ReviewStatsReadModel {
    @Id
    private String bookIsbn;
    private long ratingCount;
    private double averageRating;

    public ReviewStatsReadModel() {}

    public ReviewStatsReadModel(String bookIsbn, long ratingCount, double averageRating) {
        this.bookIsbn = bookIsbn;
        this.ratingCount = ratingCount;
        this.averageRating = averageRating;
    }

    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }
    public long getRatingCount() { return ratingCount; }
    public void setRatingCount(long ratingCount) { this.ratingCount = ratingCount; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
}
