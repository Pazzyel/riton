package com.riton.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.riton.constants.OrderStatutesConstants;
import com.riton.entity.SeckillVoucher;
import com.riton.entity.VoucherOrder;
import com.riton.mapper.SeckillVoucherMapper;
import com.riton.mapper.VoucherOrderMapper;
import com.riton.mq.MQConstants;
import com.riton.mq.OrderCreationEvent;
import com.riton.utils.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@RocketMQMessageListener(topic = MQConstants.ORDER_CREATE_TOPIC, consumerGroup = MQConstants.ORDER_CREATE_CONSUMER_GROUP)
public class OrderCreationConsumer implements RocketMQListener<OrderCreationEvent> {

    private final RedissonClient redissonClient;

    private final VoucherOrderMapper voucherOrderMapper;

    private final SeckillVoucherMapper seckillVoucherMapper;

    @Override
    public void onMessage(OrderCreationEvent orderCreationEvent) {
        VoucherOrder order = VoucherOrder.builder()
                .id(orderCreationEvent.getOrderId())
                .userId(orderCreationEvent.getUserId())
                .voucherId(orderCreationEvent.getVoucherId())
                .status(OrderStatutesConstants.UNPAID)
                .build();
        if (orderCreationEvent.getIsSeckillOrder()) {
            createSeckillVoucherOrder(order);
        } else {
            createCommonVoucherOrder(order);
        }
    }

    public void createCommonVoucherOrder(VoucherOrder voucherOrder) {
        voucherOrderMapper.insert(voucherOrder);
    }

    /**
     * 秒杀订单一个用户只能下单一次，加锁+数据库乐观锁多重检测，防止MQ多读消息
     * @param voucherOrder 订单
     */
    public void createSeckillVoucherOrder(VoucherOrder voucherOrder) {
        Long voucherId = voucherOrder.getVoucherId();
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + userId);
        if(!lock.tryLock()){
            log.error("不允许重复下单");
        }

        try {
            int count = seckillVoucherMapper.selectCount(new QueryWrapper<SeckillVoucher>().eq("voucher_id", voucherId).eq("user_id", userId));
            if (count > 0) {
                log.error("用户已经购买过一次！");
            }

            // 扣减库存,乐观锁解决，在更新库存的时候检查库存是否>0防止超卖
            boolean success = seckillVoucherMapper.deductStock(voucherId);
            if (!success) {
                //扣减失败
                log.error("库存不足！");
            }
            // 保存订单
            voucherOrderMapper.insert(voucherOrder);
        } finally {
            lock.unlock();
        }
    }
}
