package com.booksocial.identity.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
public class RateLimitService {

    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, byte[]> connection;
    private final ProxyManager<String> proxyManager;

    private final int requests;
    private final Duration period;

    public RateLimitService(@Value("${spring.data.redis.host:localhost}") String host,
                            @Value("${spring.data.redis.port:6379}") int port,
                            @Value("${app.rate-limit.requests:5}") int requests,
                            @Value("${app.rate-limit.period-seconds:60}") int periodSeconds) {
        this.redisClient = RedisClient.create(
                RedisURI.builder().withHost(host).withPort(port).build());

        this.connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        this.proxyManager = Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(5)))
                .build();

        this.requests = requests;
        this.period = Duration.ofSeconds(periodSeconds);
    }

    private final class RateLimitConfig implements Supplier<BucketConfiguration> {
        @Override
        public BucketConfiguration get() {
            return BucketConfiguration.builder()
                    .addLimit(limit ->
                            limit.capacity(requests).refillGreedy(requests, period))
                    .build();
        }
    }

    public boolean tryConsume(String key) {
        Bucket bucket = proxyManager.builder().build(key, new RateLimitConfig());
        return bucket.tryConsume(1);
    }

    @PreDestroy
    public void close() {
        connection.close();
        redisClient.shutdown();
    }
}