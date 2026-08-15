package com.booksocial.user.repository;

import com.booksocial.user.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    Optional<Follow> findByFollowerIdAndFolloweeId(Long id, Long followeeId);
    List<Follow> findByFollowerId(Long followerId);
    List<Follow> findByFolloweeId(Long followeeId);
}
