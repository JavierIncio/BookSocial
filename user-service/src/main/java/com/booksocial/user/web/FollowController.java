package com.booksocial.user.web;

import com.booksocial.user.service.FollowService;
import com.booksocial.user.web.dto.FollowResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{targetUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FollowResponse follow(@RequestHeader("X-User-Id") Long followerId,
                                 @PathVariable Long targetUserId) {
        return followService.follow(followerId, targetUserId);
    }

    @DeleteMapping("/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@RequestHeader("X-User-Id") Long followerId,
                         @PathVariable Long targetUserId) {
        followService.unfollow(followerId, targetUserId);
    }

    @GetMapping("/{userId}/followers")
    public List<FollowResponse> followers(@PathVariable Long userId) {
        return followService.followers(userId);
    }

    @GetMapping("/{userId}/following")
    public List<FollowResponse> following(@PathVariable Long userId) {
        return followService.following(userId);
    }
}
