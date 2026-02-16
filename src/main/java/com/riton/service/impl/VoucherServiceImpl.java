package com.riton.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.riton.dto.Result;
import com.riton.entity.Voucher;
import com.riton.mapper.VoucherMapper;
import com.riton.entity.SeckillVoucher;
import com.riton.service.ISeckillVoucherService;
import com.riton.service.IVoucherService;
import com.riton.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private ISeckillVoucherService seckillVoucherService;

    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    public VoucherServiceImpl(ISeckillVoucherService seckillVoucherService, StringRedisTemplate stringRedisTemplate) {
        this.seckillVoucherService = seckillVoucherService;
        this.stringRedisTemplate = stringRedisTemplate;
    }



    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
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
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        //保存秒杀库存到Redis中
        stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + seckillVoucher.getVoucherId(),seckillVoucher.getStock().toString());
    }
}
