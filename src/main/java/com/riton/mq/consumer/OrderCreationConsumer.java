package com.riton.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.riton.constants.OrderStatutesConstants;
import com.riton.domain.entity.VoucherOrder;
import com.riton.mapper.SeckillVoucherMapper;
import com.riton.mapper.VoucherOrderMapper;
import com.riton.constants.MQConstants;
import com.riton.mq.OrderCreationEvent;
import com.riton.constants.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
@RocketMQMessageListener(topic = MQConstants.ORDER_CREATE_TOPIC, consumerGroup = MQConstants.ORDER_CREATE_CONSUMER_GROUP, maxReconsumeTimes = 3)
public class OrderCreationConsumer implements RocketMQListener<OrderCreationEvent> {

    private final RedissonClient redissonClient;

    private final VoucherOrderMapper voucherOrderMapper;

    private final SeckillVoucherMapper seckillVoucherMapper;

    @Override
    @Transactional
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

    /**
     * 数据库唯一id，确保幂等性
     * @param voucherOrder 订单
     */
    public void createCommonVoucherOrder(VoucherOrder voucherOrder) {
        try {
            voucherOrderMapper.insert(voucherOrder);
        } catch (DuplicateKeyException e) {
            log.warn("同一个订单多次下单，可能是MQ多投消息！");
        }
    }

    /**
     * 秒杀订单一个用户只能下单一次，加锁+数据库乐观锁多重检测，防止MQ多读消息，确保幂等性
     * @param voucherOrder 订单
     */
    public void createSeckillVoucherOrder(VoucherOrder voucherOrder) {
        Long voucherId = voucherOrder.getVoucherId();
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + userId);
        if(!lock.tryLock()){
            log.error("不允许重复下单");
            return;
        }

        try {
            int count = voucherOrderMapper.selectCount(new QueryWrapper<VoucherOrder>().eq("voucher_id", voucherId).eq("user_id", userId));
            if (count > 0) {
                log.error("用户已经购买过一次！");
                return;
            }

            // 扣减库存,乐观锁解决，在更新库存的时候检查库存是否>0防止超卖
            boolean success = seckillVoucherMapper.deductStock(voucherId);
            if (!success) {
                //扣减失败
                log.error("出现超卖，orderId = {} 的订单在处理时发现库存不足！，请检查Redis的库存缓存和数据库的一致性！，或者同步缓存", voucherOrder.getId());
                return;
                // TODO 超卖失败可能需要更好的处理方式
            }
            // 保存订单
            voucherOrderMapper.insert(voucherOrder);
        } finally {
            lock.unlock();
        }
    }
}
