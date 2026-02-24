package com.riton.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.riton.constants.RedisConstants;
import com.riton.constants.VoucherTypeConstants;
import com.riton.domain.dto.Result;
import com.riton.domain.entity.SeckillVoucher;
import com.riton.domain.entity.Voucher;
import com.riton.mapper.VoucherMapper;
import com.riton.service.ISeckillVoucherService;
import com.riton.service.IVoucherService;
import com.riton.utils.CacheClient;
import com.riton.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    private final VoucherMapper voucherMapper;
    private final ISeckillVoucherService seckillVoucherService;
    private final CacheClient cacheClient;
    private final StringRedisTemplate stringRedisTemplate;

    private final TypeReference<List<Long>> voucherIdListType = new TypeReference<List<Long>>() {};
    private final TypeReference<Voucher> voucherType = new TypeReference<Voucher>() {};

    private static final Cache<Long, List<Long>> SHOP_VOUCHER_IDS_LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final Cache<Long, Voucher> VOUCHER_LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Autowired
    public VoucherServiceImpl(ISeckillVoucherService seckillVoucherService, StringRedisTemplate stringRedisTemplate, VoucherMapper voucherMapper, CacheClient cacheClient) {
        this.seckillVoucherService = seckillVoucherService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.voucherMapper = voucherMapper;
        this.cacheClient = cacheClient;
    }

    /**
     * 根据shopId一次查出全部优惠券
     * @param shopId 商铺id
     * @return 所有优惠券列表，按照更新时间倒序
     */
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        List<Long> voucherIds = SHOP_VOUCHER_IDS_LOCAL_CACHE.getIfPresent(shopId);
        if (voucherIds != null) {
            log.info("本地缓存命中：查询商铺:{} 的所有优惠券{}", shopId, voucherIds);
        }
        if (voucherIds == null) {
            voucherIds = cacheClient.queryWithMutex(
                    RedisConstants.CACHE_SHOP_VOUCHERS_KEY,
                    shopId,
                    voucherIdListType,
                    voucherMapper::queryVoucherIdsOfShop,
                    RedisConstants.CACHE_SHOP_VOUCHERS_TTL,
                    TimeUnit.MINUTES
            );
            if (voucherIds == null || voucherIds.isEmpty()) {
                return Result.ok(Collections.emptyList());
            }
            SHOP_VOUCHER_IDS_LOCAL_CACHE.put(shopId, voucherIds);
        }

        List<Voucher> vouchers = queryVoucherByIdsWithMultiLevelCache(voucherIds);
        return Result.ok(vouchers);
    }

    /**
     * 查询单个优惠券，多级缓存
     * @param voucherId 优惠券id
     * @return 优惠券
     */
    @Override
    public Voucher queryVoucherByIdWithCache(Long voucherId) {
        Voucher localVoucher = VOUCHER_LOCAL_CACHE.getIfPresent(voucherId);
        if (localVoucher != null) {
            return localVoucher;
        }

        Voucher voucher = cacheClient.queryWithMutex(
                RedisConstants.CACHE_VOUCHER_KEY,
                voucherId,
                voucherType,
                voucherMapper::queryVoucherById,
                RedisConstants.CACHE_VOUCHER_TTL,
                TimeUnit.MINUTES
        );
        if (voucher != null) {
            VOUCHER_LOCAL_CACHE.put(voucherId, voucher);
        }
        return voucher;
    }

    /**
     * 保存秒杀优惠券
     * @param voucher 秒杀优惠券
     */
    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        voucher.setCreateTime(now);
        voucher.setUpdateTime(now);
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucher.setCreateTime(now);
        seckillVoucher.setUpdateTime(now);
        seckillVoucherService.save(seckillVoucher);
        invalidateShopVoucherCache(voucher.getShopId());
        invalidateSingleVoucherCache(voucher.getId());
        // 保存秒杀库存到Redis中
        stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + seckillVoucher.getVoucherId(), seckillVoucher.getStock().toString());
    }

    @Override
    public Result updateVoucher(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        voucher.setUpdateTime(now);
        voucherMapper.updateById(voucher);
        invalidateShopVoucherCache(voucher.getShopId());
        invalidateSingleVoucherCache(voucher.getId());
        return Result.ok(voucher.getId());
    }

    @Override
    @Transactional
    public Result updateSeckillVoucher(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        voucher.setUpdateTime(now);
        voucherMapper.updateById(voucher);
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucher.setUpdateTime(now);
        seckillVoucherService.updateById(seckillVoucher);
        invalidateShopVoucherCache(voucher.getShopId());
        invalidateSingleVoucherCache(voucher.getId());
        return Result.ok(voucher.getId());
    }

    @Override
    @Transactional
    public Result deleteVoucher(Long voucherId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.ok(voucherId);
        }

        voucherMapper.deleteById(voucherId);
        if (voucher.getType().equals(VoucherTypeConstants.SECKILL)) {
            seckillVoucherService.removeById(voucherId);
        }
        invalidateShopVoucherCache(voucher.getShopId());
        invalidateSingleVoucherCache(voucherId);
        return Result.ok(voucherId);
    }

    /**
     * 查询id列表对应的优惠券列表信息，先查Redis再查数据库
     * 我们倾向于直接从Redis一次全部查出，因此该函数不会加入本地缓存
     * @param voucherIds id列表
     * @return 优惠券列表
     */
    private List<Voucher> queryVoucherByIdsWithMultiLevelCache(List<Long> voucherIds) {
        List<String> keys = voucherIds.stream()
                .map(id -> RedisConstants.CACHE_VOUCHER_KEY + id)
                .collect(Collectors.toList());
        // Redis的MGET，结果的顺序就是key的顺序，没有的是null
        List<String> cachedJsonList = stringRedisTemplate.opsForValue().multiGet(keys);

        Map<Long, Voucher> voucherMap = new HashMap<>(voucherIds.size());
        // 有些id对应的优惠券还没被缓存到Redis
        List<Long> missIds = new ArrayList<>();

        for (int i = 0; i < voucherIds.size(); i++) {
            Long id = voucherIds.get(i);
            String voucherJson = cachedJsonList == null ? null : cachedJsonList.get(i);
            // 没有被缓存的部分要一个个查
            if (voucherJson == null) {
                missIds.add(id);
                continue;
            }
            if (voucherJson.isEmpty()) {
                continue;
            }

            // 已经缓存的部分直接解析Json
            try {
                Voucher voucher = JsonUtils.readValue(voucherJson, Voucher.class);
                voucherMap.put(id, voucher);
                //VOUCHER_LOCAL_CACHE.put(id, voucher);
            } catch (Exception e) {
                missIds.add(id);
            }
        }

        // 查询没有被缓存的优惠券
        for (Long missId : missIds) {
            Voucher voucher = queryVoucherByIdWithCache(missId);
            if (voucher != null) {
                voucherMap.put(missId, voucher);
                //VOUCHER_LOCAL_CACHE.put(missId, voucher);
            }
        }

        // 保证顺序和voucherIds的顺序一致
        List<Voucher> vouchers = new ArrayList<>(voucherIds.size());
        for (Long voucherId : voucherIds) {
            Voucher voucher = voucherMap.get(voucherId);
            if (voucher != null) {
                vouchers.add(voucher);
            }
        }
        return vouchers;
    }

    public void invalidateShopVoucherCache(Long shopId) {
        cacheClient.invalidate(RedisConstants.CACHE_SHOP_VOUCHERS_KEY, shopId);
        SHOP_VOUCHER_IDS_LOCAL_CACHE.invalidate(shopId);
    }

    public void invalidateSingleVoucherCache(Long voucherId) {
        cacheClient.invalidate(RedisConstants.CACHE_VOUCHER_KEY, voucherId);
        VOUCHER_LOCAL_CACHE.invalidate(voucherId);
    }
}
