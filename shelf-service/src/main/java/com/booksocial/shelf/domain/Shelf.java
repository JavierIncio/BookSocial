package com.booksocial.shelf.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "shelves", uniqueConstraints = @UniqueConstraint(columnNames = "book_isbn, user_id"))
public class Shelf {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_isbn", nullable = false, length = 20)
    private String bookIsbn;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private ShelfStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public Shelf() {}

    public Shelf(String bookIsbn, Long userId, ShelfStatus status) {
        this.bookIsbn = bookIsbn;
        this.userId = userId;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookIsbn() {
        return bookIsbn;
    }

    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ShelfStatus getStatus() {
        return status;
    }

    public void setStatus(ShelfStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
