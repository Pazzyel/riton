package com.riton.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.riton.constants.VoucherTypeConstants;
import com.riton.dto.Result;
import com.riton.entity.Voucher;
import com.riton.mapper.VoucherMapper;
import com.riton.entity.SeckillVoucher;
import com.riton.service.ISeckillVoucherService;
import com.riton.service.IVoucherService;
import com.riton.utils.CacheClient;
import com.riton.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    private final VoucherMapper voucherMapper;
    private final ISeckillVoucherService seckillVoucherService;
    private final CacheClient cacheClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final TypeReference<List<Voucher>> voucherListType = new TypeReference<List<Voucher>>() {};

    @Autowired
    public VoucherServiceImpl(ISeckillVoucherService seckillVoucherService, StringRedisTemplate stringRedisTemplate, VoucherMapper voucherMapper, CacheClient cacheClient) {
        this.seckillVoucherService = seckillVoucherService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.voucherMapper = voucherMapper;
        this.cacheClient = cacheClient;
    }



    @Override
    public Result queryVoucherOfShop(Long shopId) {

        List<Voucher> vouchers = cacheClient.queryWithMutex(RedisConstants.CACHE_SHOP_VOUCHERS_KEY,shopId,voucherListType,voucherMapper::queryVoucherOfShop,RedisConstants.CACHE_SHOP_VOUCHERS_TTL, TimeUnit.MINUTES);

        // 查询优惠券信息
        //List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
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
        //保存秒杀库存到Redis中
        stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + seckillVoucher.getVoucherId(),seckillVoucher.getStock().toString());
    }

    @Override
    public Result updateVoucher(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        voucher.setUpdateTime(now);
        voucherMapper.updateById(voucher);
        cacheClient.invalidate(RedisConstants.CACHE_SHOP_VOUCHERS_KEY,voucher.getShopId());
        return Result.ok(voucher.getId());
    }

    @Override
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
        cacheClient.invalidate(RedisConstants.CACHE_SHOP_VOUCHERS_KEY,voucher.getShopId());
        return Result.ok(voucher.getId());
    }

    @Override
    public Result deleteVoucher(Long voucherId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        voucherMapper.deleteById(voucherId);
        if (voucher.getType().equals(VoucherTypeConstants.SECKILL)) {
            seckillVoucherService.removeById(voucherId);
        }
        cacheClient.invalidate(RedisConstants.CACHE_SHOP_VOUCHERS_KEY,voucher.getShopId());
        return Result.ok(voucherId);
    }
}
