package com.riton.bloom;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class BloomFilterFactory {

    private static final long DEFAULT_EXPECTED_INSERTIONS = 1_000_000L;
    private static final double DEFAULT_FALSE_PROBABILITY = 0.03D;

    private final RedissonClient redissonClient;
    private final ConcurrentMap<String, BloomFilter> bloomFilterCache = new ConcurrentHashMap<>();

    @Autowired
    public BloomFilterFactory(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public BloomFilter getBloomFilter(String name) {
        return bloomFilterCache.computeIfAbsent(
                name,
                key -> new BloomFilter(redissonClient, key, DEFAULT_EXPECTED_INSERTIONS, DEFAULT_FALSE_PROBABILITY)
        );
    }
}
