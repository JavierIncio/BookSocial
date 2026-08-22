package com.booksocial.review.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "book_refs")
public class BookRefReadModel {

    @Id
    private String isbn;

    private String title;
    private String authorName;
    private String authorId;

    public BookRefReadModel() {
    }

    public BookRefReadModel(String isbn, String title, String authorName, String authorId) {
        this.isbn = isbn;
        this.title = title;
        this.authorName = authorName;
        this.authorId = authorId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }
}
