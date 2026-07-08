package com.tracepilot.api.Services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterService<K> {

    private final Map<K, Bucket> cache = new ConcurrentHashMap<>();

    private final int capacity;
    private final Duration refillDuration;

    public RateLimiterService(int capacity, Duration refillDuration) {
        this.capacity = capacity;
        this.refillDuration = refillDuration;
    }

    public Bucket resolveBucket(K key) {
        return cache.computeIfAbsent(key, this::createBucket);
    }

    private Bucket createBucket(K key) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, refillDuration)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}