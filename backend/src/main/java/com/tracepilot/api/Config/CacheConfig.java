// package com.tracepilot.api.Config;

// import java.util.List;
// import java.util.concurrent.TimeUnit;

// import org.springframework.cache.CacheManager;
// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.cache.caffeine.CaffeineCacheManager;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import com.github.benmanes.caffeine.cache.Caffeine;

// @Configuration
// @EnableCaching
// public class CacheConfig {

//     @Bean
//     public CacheManager cacheManager() {
//         CaffeineCacheManager manager = new CaffeineCacheManager();
//         manager.setCacheNames(List.of("traceHashLookup", "oauthUserInfo", "reliabilityTrend"));
//         manager.setCaffeine(Caffeine.newBuilder().recordStats()); // default fallback spec
//         manager.registerCustomCache("traceHashLookup",
//             Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(30, TimeUnit.MINUTES).build());
//         manager.registerCustomCache("reliabilityTrend",
//             Caffeine.newBuilder().maximumSize(200).expireAfterWrite(5, TimeUnit.MINUTES).build());
//         return manager;
//     }
// }