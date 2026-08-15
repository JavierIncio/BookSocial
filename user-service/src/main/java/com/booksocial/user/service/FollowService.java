package com.booksocial.user.service;

import com.booksocial.user.domain.*;
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
    private final ProfileReadModelRepository profileReadModelRepository;

    public FollowService(FollowRepository followRepository,
                         FollowReadModelRepository followReadModelRepository,
                         ProfileReadModelRepository profileReadModelRepository) {
        this.followRepository = followRepository;
        this.followReadModelRepository = followReadModelRepository;
        this.profileReadModelRepository = profileReadModelRepository;
    }

    public FollowResponse follow(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new SelfFollowException();
        }
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, targetUserId)) {
            throw new AlreadyFollowingException(followerId, targetUserId);
        }
        Follow follow = followRepository.save(new Follow(followerId, targetUserId));
        followReadModelRepository.save(new FollowReadModel(followerId, targetUserId));
        adjustCounters(followerId, targetUserId, 1);
        return toResponse(follow.getFollowerId(), follow.getFolloweeId(), follow.getCreatedAt());
    }

    public void unfollow(Long followerId, Long targetUserId) {
        Follow follow = followRepository.findByFollowerIdAndFolloweeId(followerId, targetUserId)
                .orElseThrow(() -> new NotFollowingException(followerId, targetUserId));
        followRepository.delete(follow);
        followReadModelRepository.findByFollowerIdAndFolloweeId(followerId, targetUserId)
                .ifPresent(followReadModelRepository::delete);
        adjustCounters(followerId, targetUserId, -1);
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

    private void adjustCounters(Long followerId, Long followeeId, int delta) {
        profileReadModelRepository.findByUserId(followerId).ifPresent(p -> {
            p.setFollowingCount(Math.max(0, p.getFollowingCount() + delta));
            profileReadModelRepository.save(p);
        });
        profileReadModelRepository.findByUserId(followeeId).ifPresent(p -> {
            p.setFollowersCount(Math.max(0, p.getFollowersCount() + delta));
            profileReadModelRepository.save(p);
        });
    }

    private FollowResponse toResponse(Long followerId, Long followeeId, Instant createdAt) {
        return new FollowResponse(followerId, followeeId, createdAt);
    }
}
