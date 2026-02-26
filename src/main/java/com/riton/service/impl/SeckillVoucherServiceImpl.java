package com.riton.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.riton.bloom.BloomFilterFactory;
import com.riton.constants.Constants;
import com.riton.constants.RedisConstants;
import com.riton.domain.dto.Result;
import com.riton.domain.entity.SeckillVoucher;
import com.riton.domain.entity.Shop;
import com.riton.mapper.SeckillVoucherMapper;
import com.riton.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.riton.utils.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
@Slf4j
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

    private final CacheClient cacheClient;

    private final StringRedisTemplate stringRedisTemplate;

    private final BloomFilterFactory bloomFilterFactory;

    private final TypeReference<SeckillVoucher> type = new TypeReference<>() {};

    private final SeckillVoucherMapper seckillVoucherMapper;

    private static final Cache<Long, SeckillVoucher> SECKILL_VOUCHER_LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Autowired
    public SeckillVoucherServiceImpl(CacheClient cacheClient, StringRedisTemplate stringRedisTemplate, BloomFilterFactory bloomFilterFactory, SeckillVoucherMapper seckillVoucherMapper) {
        this.cacheClient = cacheClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.bloomFilterFactory = bloomFilterFactory;
        this.seckillVoucherMapper = seckillVoucherMapper;
    }

    @Override
    public SeckillVoucher getById(Serializable sid){
        Long id = (Long) sid;
        SeckillVoucher localSeckillVoucher = SECKILL_VOUCHER_LOCAL_CACHE.getIfPresent(id);
        if (localSeckillVoucher != null) {
            log.info("查询秒杀券 本地缓存命中 商铺 : {}", localSeckillVoucher);
            // 这里查出来的库存无效，库存必须在Redis判断
            localSeckillVoucher.setStock(Integer.valueOf(
                    Objects.requireNonNull(stringRedisTemplate.opsForValue().get(RedisConstants.SECKILL_STOCK_KEY + id))));
            return localSeckillVoucher;
        }

        // 布隆过滤器判断空值
        if (!bloomFilterFactory.getBloomFilter(Constants.BLOOM_FILTER_HANDLER_SECKILL_VOUCHER).contains(String.valueOf(id))) {
            log.info("查询秒杀券 布隆过滤器判断不存在 秒杀券id : {}",id);
            return null;
        }

        // 互斥锁解决缓存击穿
        SeckillVoucher seckillVoucher = cacheClient
                .queryWithMutex(RedisConstants.CACHE_SECKILL_VOUCHERS_KEY, id, type, seckillVoucherMapper::selectById, RedisConstants.CACHE_SECKILL_VOUCHERS_TTL, TimeUnit.MINUTES);

        if (seckillVoucher == null) {
            return null;
        }

        // 这里查出来的库存无效，库存必须在Redis判断
        seckillVoucher.setStock(Integer.valueOf(
                Objects.requireNonNull(stringRedisTemplate.opsForValue().get(RedisConstants.SECKILL_STOCK_KEY + id))));

        // 存到Caffeine
        SECKILL_VOUCHER_LOCAL_CACHE.put(id, seckillVoucher);

        // 7.返回
        return seckillVoucher;
    }

    @Override
    public boolean updateById(SeckillVoucher entity) {
        boolean r = super.updateById(entity);
        if (r) {
            invalidateCache(entity.getVoucherId());
        }
        return r;
    }

    @Override
    public boolean removeById(SeckillVoucher entity) {
        boolean r = super.removeById(entity);
        if (r) {
            invalidateCache(entity.getVoucherId());
        }
        return r;
    }

    private void invalidateCache(Long id) {
        SECKILL_VOUCHER_LOCAL_CACHE.invalidate(id);
        cacheClient.invalidate(RedisConstants.CACHE_SECKILL_VOUCHERS_KEY, id);
    }
}
