package com.riton.bloom;

import lombok.Getter;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.util.StringUtils;

public class BloomFilter {

    private static final String FILTER_KEY_PREFIX = "bloom:filter:";

    @Getter
    private final String name;
    private final RBloomFilter<String> bloomFilter;

    public BloomFilter(RedissonClient redissonClient, String name, long expectedInsertions, double falseProbability) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Bloom filter name must not be blank");
        }
        this.name = name;
        this.bloomFilter = redissonClient.getBloomFilter(FILTER_KEY_PREFIX + name);
        if (!this.bloomFilter.isExists()) {
            this.bloomFilter.tryInit(expectedInsertions, falseProbability);
        }
    }

    public boolean add(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return bloomFilter.add(value);
    }

    public boolean contains(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return bloomFilter.contains(value);
    }
}
