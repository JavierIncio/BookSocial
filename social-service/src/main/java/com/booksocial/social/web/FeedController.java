package com.booksocial.social.web;

import com.booksocial.social.service.FeedService;
import com.booksocial.social.web.dto.FeedPageResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public FeedPageResponse getFeed(@RequestHeader("X-User-Id") Long userId,
                                    @RequestParam(required = false) String cursor,
                                    @RequestParam(defaultValue = "10") int limit) {
        return feedService.getFeed(userId, cursor, limit);
    }
}
