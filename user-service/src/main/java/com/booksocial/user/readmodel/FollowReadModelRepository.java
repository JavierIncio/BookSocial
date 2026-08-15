package com.booksocial.user.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FollowReadModelRepository extends MongoRepository<FollowReadModel,String> {
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    Optional<FollowReadModel> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    List<FollowReadModel> findByFollowerId(Long followerId);
    List<FollowReadModel> findByFolloweeId(Long followeeId);
    long countByFollowerId(Long followerId);
    long countByFolloweeId(Long followeeId);
}
