package com.booksocial.user.events;

import com.booksocial.user.config.RabbitConfig;
import com.booksocial.user.readmodel.FollowReadModel;
import com.booksocial.user.readmodel.FollowReadModelRepository;
import com.booksocial.user.readmodel.ProfileReadModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FollowEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(FollowEventConsumer.class);

    private final FollowReadModelRepository followReadModelRepository;
    private final ProfileReadModelRepository profileReadModelRepository;

    public FollowEventConsumer(FollowReadModelRepository followReadModelRepository,
                               ProfileReadModelRepository profileReadModelRepository) {
        this.followReadModelRepository = followReadModelRepository;
        this.profileReadModelRepository = profileReadModelRepository;
    }

    @RabbitListener(queues = RabbitConfig.FOLLOWED_QUEUE)
    public void onFollowed(FollowedEvent event) {
        if (!followReadModelRepository.existsByFollowerIdAndFolloweeId(event.followerId(), event.followeeId())) {
            followReadModelRepository.save(new FollowReadModel(event.followerId(), event.followeeId()));
        }
        syncCounters(event.followerId(), event.followeeId());
        log.info("Processed FollowedEvent: {} -> {}", event.followerId(), event.followeeId());
    }

    @RabbitListener(queues = RabbitConfig.UNFOLLOWED_QUEUE)
    public void onUnfollowed(UnfollowedEvent event) {
        followReadModelRepository.findByFollowerIdAndFolloweeId(event.followerId(), event.followeeId())
                .ifPresent(followReadModelRepository::delete);
        syncCounters(event.followerId(), event.followeeId());
        log.info("Processed UnfollowedEvent: {} -> {}", event.followerId(), event.followeeId());
    }

    private void syncCounters(Long followerId, Long followeeId) {
        profileReadModelRepository.findByUserId(followerId).ifPresent(p -> {
            p.setFollowingCount(followReadModelRepository.countByFollowerId(followerId));
            profileReadModelRepository.save(p);
        });
        profileReadModelRepository.findByUserId(followeeId).ifPresent(p -> {
            p.setFollowersCount(followReadModelRepository.countByFolloweeId(followeeId));
            profileReadModelRepository.save(p);
        });
    }
}