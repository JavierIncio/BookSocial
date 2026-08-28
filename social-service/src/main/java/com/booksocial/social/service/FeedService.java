package com.booksocial.social.service;

import com.booksocial.social.domain.ActivityType;
import com.booksocial.social.events.ReviewCreatedEvent;
import com.booksocial.social.events.ReviewEvent;
import com.booksocial.social.events.ReviewUpdatedEvent;
import com.booksocial.social.events.ShelfChangedEvent;
import com.booksocial.social.readmodel.*;
import com.booksocial.social.web.dto.FeedItemResponse;
import com.booksocial.social.web.dto.FeedPageResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FeedService {
    private final ActivityItemReadModelRepository activityRepo;
    private final FeedEntryReadModelRepository feedEntryRepo;
    private final FollowerIndexReadModelRepository followerRepo;
    private final MongoTemplate mongoTemplate;

    public FeedService(ActivityItemReadModelRepository activityRepo,
                       FeedEntryReadModelRepository feedEntryRepo,
                       FollowerIndexReadModelRepository followerRepo,
                       MongoTemplate mongoTemplate) {
        this.activityRepo = activityRepo;
        this.feedEntryRepo = feedEntryRepo;
        this.followerRepo = followerRepo;
        this.mongoTemplate = mongoTemplate;
    }

    public FeedPageResponse getFeed(Long userId, String cursor, int limit) {
        Instant cutOffOccurredAt = null;
        String cutOffId = null;
        if (cursor != null && !cursor.isBlank()) {
            FeedEntryReadModel lastEntry = feedEntryRepo.findById(cursor).orElse(null);
            if (lastEntry != null) {
                cutOffOccurredAt = lastEntry.getOccurredAt();
                cutOffId = lastEntry.getId();
            }
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("feedUserId").is(userId));
        if (cutOffOccurredAt != null) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("occurredAt").lt(cutOffOccurredAt),
                    new Criteria().andOperator(
                            Criteria.where("occurredAt").is(cutOffOccurredAt),
                            Criteria.where("_id").lt(cutOffId))
            ));
        }
        query.with(Sort.by(Sort.Direction.DESC, "occurredAt", "_id"));
        query.limit(limit + 1);

        List<FeedEntryReadModel> items = mongoTemplate.find(query, FeedEntryReadModel.class);
        boolean hasNext = items.size() > limit;
        List<FeedEntryReadModel> pageEntries = hasNext ? items.subList(0, limit) : items;

        List<FeedItemResponse> feedItems = new ArrayList<>();
        for (FeedEntryReadModel entry : pageEntries) {
            activityRepo.findById(entry.getActivityId()).ifPresent(a -> {
                feedItems.add(new FeedItemResponse(a.getId(), a.getType().name(),
                        a.getActorId(), a.getPayload(), a.getOccurredAt()));
            });
        }

        String nextCursor = null;
        if (hasNext && !pageEntries.isEmpty())
            nextCursor = pageEntries.getLast().getId();

        return new FeedPageResponse(feedItems, nextCursor);
    }

    public void handleFollowed(Long followerId, Long followeeId) {
        FollowerIndexReadModel idx = followerRepo.findById(String.valueOf(followeeId))
                .orElse(new FollowerIndexReadModel(
                        String.valueOf(followeeId), followeeId, new ArrayList<>()));

        if (!idx.getFollowers().contains(followerId))
            idx.getFollowers().add(followerId);

        followerRepo.save(idx);

        String activityId = generateActivityId(
                ActivityType.FOLLOW,
                String.format("%s:%s", followeeId, followerId));

        ActivityItemReadModel item = new ActivityItemReadModel(
                        activityId, ActivityType.FOLLOW, followerId, Map.of("targetUserId", followeeId));

        activityRepo.save(item);

        fanout(followerId, activityId);
        fanoutToUser(followeeId, activityId);
    }

    public void handleUnfollowed(Long followerId, Long followeeId) {
        followerRepo.findById(String.valueOf(followeeId)).ifPresent(idx -> {
            idx.getFollowers().remove(followerId);
            followerRepo.save(idx);
        });
    }

    public void handleReviewCreated(ReviewCreatedEvent event) {
        String activityId = generateActivityId(
                ActivityType.REVIEW,
                String.valueOf(event.reviewId()));

        ActivityItemReadModel item = new ActivityItemReadModel(
                activityId, ActivityType.REVIEW, event.actorUserId(), buildReviewPayload(event));

        activityRepo.save(item);

        fanout(event.actorUserId(), activityId);
    }

    public void handleReviewUpdated(ReviewUpdatedEvent event) {
        String activityId = generateActivityId(
                ActivityType.REVIEW,
                String.valueOf(event.reviewId()));

        ActivityItemReadModel item = activityRepo.findById(activityId).orElse(null);

        if (item == null) {
            item = new ActivityItemReadModel(
                    activityId, ActivityType.REVIEW,
                    event.actorUserId(), buildReviewPayload(event));
            activityRepo.save(item);
            fanout(event.actorUserId(), activityId);

        } else {
            item.setPayload(buildReviewPayload(event));
            activityRepo.save(item);
        }
    }

    public void handleShelfChanged(ShelfChangedEvent event) {
        String activityId = generateActivityId(
                ActivityType.SHELF,
                String.format("%s:%s:%s", event.userId(), event.bookIsbn(), event.occurredAt()));

        ActivityItemReadModel item = new ActivityItemReadModel(
                activityId, ActivityType.SHELF, event.userId(), Map.of(
                "bookIsbn", event.bookIsbn(),
                "title", event.title(),
                "authorName", event.authorName(),
                "shelfStatus", event.status()
        ));

        activityRepo.save(item);
        fanout(event.userId(), activityId);
    }

    private void fanout(Long actorId, String activityId) {
        feedEntryRepo.save(new FeedEntryReadModel(null, actorId, activityId));

        followerRepo.findById(String.valueOf(actorId)).ifPresent(idx -> {
            idx.getFollowers().forEach(followerId -> {
                feedEntryRepo.save(new FeedEntryReadModel(null, followerId, activityId));
            });
        });
    }

    private void fanoutToUser(Long followeeId, String activityId) {
        feedEntryRepo.save(new FeedEntryReadModel(null, followeeId, activityId));
    }

    private String generateActivityId(ActivityType type, String key) {
        return type.name() + ":" + key;
    }

    private Map<String, Object> buildReviewPayload(ReviewEvent event) {
        return Map.of(
                "bookIsbn", event.bookIsbn(),
                "title", event.title(),
                "authorName", event.authorName(),
                "rating", event.rating(),
                "comment", event.comment()
        );
    }
}
