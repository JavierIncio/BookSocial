package com.booksocial.shelf.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookRefReadModelRepository extends MongoRepository<BookRefReadModel, String> {
}
