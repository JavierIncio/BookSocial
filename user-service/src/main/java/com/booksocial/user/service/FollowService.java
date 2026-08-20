package com.booksocial.user.service;

import com.booksocial.user.domain.*;
import com.booksocial.user.events.FollowEventPublisher;
import com.booksocial.user.readmodel.FollowReadModel;
import com.booksocial.user.readmodel.FollowReadModelRepository;
import com.booksocial.user.readmodel.ProfileReadModelRepository;
import com.booksocial.user.repository.FollowRepository;
import com.booksocial.user.web.dto.FollowResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


@Service
@Transactional
public class FollowService {

    private final FollowRepository followRepository;
    private final FollowReadModelRepository followReadModelRepository;
    private final FollowEventPublisher eventPublisher;


    public FollowService(FollowRepository followRepository,
                         FollowReadModelRepository followReadModelRepository,
                         FollowEventPublisher eventPublisher) {
        this.followRepository = followRepository;
        this.followReadModelRepository = followReadModelRepository;
        this.eventPublisher = eventPublisher;
    }

    public FollowResponse follow(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) throw new SelfFollowException();
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, targetUserId))
            throw new AlreadyFollowingException(followerId, targetUserId);

        Follow follow = followRepository.save(new Follow(followerId, targetUserId));
        eventPublisher.publishFollowed(follow.getFollowerId(), follow.getFolloweeId());
        return toResponse(follow.getFollowerId(), follow.getFolloweeId(), follow.getCreatedAt());
    }

    public void unfollow(Long followerId, Long targetUserId) {
        Follow follow = followRepository.findByFollowerIdAndFolloweeId(followerId, targetUserId)
                .orElseThrow(() -> new NotFollowingException(followerId, targetUserId));
        followRepository.delete(follow);
        eventPublisher.publishUnfollowed(follow.getFollowerId(), follow.getFolloweeId());
    }

    public List<FollowResponse> followers(Long userId) {
        return followReadModelRepository.findByFolloweeId(userId).stream()
                .map(f -> toResponse(f.getFollowerId(), f.getFolloweeId(), f.getCreatedAt()))
                .toList();
    }

    public List<FollowResponse> following(Long userId) {
        return followReadModelRepository.findByFollowerId(userId).stream()
                .map(f -> toResponse(f.getFollowerId(), f.getFolloweeId(), f.getCreatedAt()))
                .toList();
    }

    private FollowResponse toResponse(Long followerId, Long followeeId, Instant createdAt) {
        return new FollowResponse(followerId, followeeId, createdAt);
    }
}
