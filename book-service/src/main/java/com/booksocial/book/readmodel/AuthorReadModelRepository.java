package com.booksocial.book.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuthorReadModelRepository extends MongoRepository<AuthorReadModel, String> {
    List<AuthorReadModel> findByNameContainingIgnoreCase(String name);
}
