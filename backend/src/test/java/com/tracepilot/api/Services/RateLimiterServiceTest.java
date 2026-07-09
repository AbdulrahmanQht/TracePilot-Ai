package com.tracepilot.api.Services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.bucket4j.Bucket;

class RateLimiterServiceTest {

    @Test
    void resolveBucket_allowsConsumptionUpToCapacity() {
        RateLimiterService<String> service = new RateLimiterService<>(3, Duration.ofMinutes(1));

        Bucket bucket = service.resolveBucket("client-a");

        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void resolveBucket_returnsSameBucketInstance_forSameKey() {
        RateLimiterService<String> service = new RateLimiterService<>(5, Duration.ofMinutes(1));

        Bucket first = service.resolveBucket("same-key");
        first.tryConsume(2);
        Bucket second = service.resolveBucket("same-key");

        assertThat(second).isSameAs(first);
        assertThat(second.getAvailableTokens()).isEqualTo(3);
    }

    @Test
    void resolveBucket_returnsIndependentBuckets_forDifferentKeys() {
        RateLimiterService<String> service = new RateLimiterService<>(2, Duration.ofMinutes(1));

        Bucket bucketA = service.resolveBucket("key-a");
        Bucket bucketB = service.resolveBucket("key-b");

        assertThat(bucketA.tryConsume(2)).isTrue();
        assertThat(bucketA.tryConsume(1)).isFalse();
        // bucketB should be unaffected by bucketA's consumption
        assertThat(bucketB.tryConsume(2)).isTrue();
    }

    @Test
    void resolveBucket_worksWithNonStringKeys() {
        RateLimiterService<java.util.UUID> service = new RateLimiterService<>(1, Duration.ofMinutes(1));
        java.util.UUID userId = java.util.UUID.randomUUID();

        Bucket bucket = service.resolveBucket(userId);

        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }
}
