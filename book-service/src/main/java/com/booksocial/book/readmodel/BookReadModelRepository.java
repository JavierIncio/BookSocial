package com.booksocial.book.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookReadModelRepository extends MongoRepository<BookReadModel,String> {
    List<BookReadModel> findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase(String title, String authorName);
}
