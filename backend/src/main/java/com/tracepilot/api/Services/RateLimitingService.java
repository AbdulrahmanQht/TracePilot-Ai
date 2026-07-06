package com.tracepilot.api.Services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {
    // Cache to hold token buckets for each active user
    private final Map<UUID, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(UUID userId) {
        return cache.computeIfAbsent(userId, this::createNewBucket);
    }

    private Bucket createNewBucket(UUID userId) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofDays(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

}
