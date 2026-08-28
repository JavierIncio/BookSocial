package com.booksocial.social.web.dto;

import java.util.List;

public record FeedPageResponse(
        List<FeedItemResponse> items,
        String nextCursor
) {
}
