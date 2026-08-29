package com.booksocial.notification.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationReadModelRepository extends MongoRepository<NotificationReadModel,String> {
    List<NotificationReadModel> findByUserIdOrderByOccurredAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
}
