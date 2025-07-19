package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    private RedisIdWorker redisIdWorker;

    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    public VoucherOrderServiceImpl(RedisIdWorker redisIdWorker, ISeckillVoucherService seckillVoucherService) {
        this.redisIdWorker = redisIdWorker;
        this.seckillVoucherService = seckillVoucherService;
    }

    /**
     * 下单秒杀券
     * @param voucherId 秒杀券的id
     * @return 如果下单成功，返回订单id
     */
    @Override
    public Result seckillVoucher(Long voucherId){
        // 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 已经结束
            return Result.fail("秒杀已经结束！");
        }
        // 判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足！");
        }
        // 扣减库存,乐观锁解决，在更新库存的时候检查库存是否>0防止超卖
        boolean success = seckillVoucherService.update()
                .setSql("stock= stock -1")
                .eq("voucher_id", voucherId).gt("stock",0).update();
        if (!success) {
            //扣减失败
            return Result.fail("库存不足！");
        }
        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 用户id
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        // 代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        return Result.ok(orderId);
    }
}
