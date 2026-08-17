package com.booksocial.shelf.readmodel;

import com.booksocial.shelf.domain.Shelf;
import com.booksocial.shelf.domain.ShelfStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "shelves")
public class ShelfReadModel {
    @Id
    private String id;
    private Long userId;
    private String bookIsbn;
    private String title;
    private String author;
    private ShelfStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public ShelfReadModel() {}

    public ShelfReadModel(Shelf shelf, BookRefReadModel book) {
        this.id = shelf.getUserId() + ":" + shelf.getBookIsbn();
        this.userId = shelf.getUserId();
        this.bookIsbn = shelf.getBookIsbn();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.status = shelf.getStatus();
        this.createdAt = shelf.getCreatedAt();
        this.updatedAt = shelf.getUpdatedAt();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBookIsbn() {
        return bookIsbn;
    }

    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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
