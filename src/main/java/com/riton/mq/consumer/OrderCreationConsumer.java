package com.riton.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.riton.constants.OrderStatutesConstants;
import com.riton.domain.entity.VoucherOrder;
import com.riton.domain.entity.Voucher;
import com.riton.mapper.SeckillVoucherMapper;
import com.riton.mapper.VoucherOrderMapper;
import com.riton.constants.MQConstants;
import com.riton.mq.OrderCloseEvent;
import com.riton.mq.OrderCreationEvent;
import com.riton.constants.RedisConstants;
import com.riton.service.impl.VoucherServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Slf4j
@AllArgsConstructor
@RocketMQMessageListener(topic = MQConstants.ORDER_CREATE_TOPIC, consumerGroup = MQConstants.ORDER_CREATE_CONSUMER_GROUP, maxReconsumeTimes = 3)
public class OrderCreationConsumer implements RocketMQListener<OrderCreationEvent> {

    private final RedissonClient redissonClient;

    private final VoucherOrderMapper voucherOrderMapper;

    private final SeckillVoucherMapper seckillVoucherMapper;

    private final VoucherServiceImpl voucherService;

    private final RocketMQTemplate rocketMQTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT;

    static {
        SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("lua/seckill_one_limit_rollback.lua"));
        SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    private static final Long CLOSE_TIME_SECONDS = 1800L;

    @Override
    @Transactional
    public void onMessage(OrderCreationEvent orderCreationEvent) {
        // 第一步：读取券信息并确定订单所属店铺。
        Voucher voucher = voucherService.getById(orderCreationEvent.getVoucherId());
        if (voucher == null || voucher.getShopId() == null) {
            log.error("下单消息对应优惠券不存在或店铺为空, voucherId={}, orderId={}", orderCreationEvent.getVoucherId(), orderCreationEvent.getOrderId());
            rollbackIfSeckill(orderCreationEvent);
            return;
        }

        // 第二步：构建订单对象并设置 shopId。
        VoucherOrder order = VoucherOrder.builder()
                .id(orderCreationEvent.getOrderId())
                .userId(orderCreationEvent.getUserId())
                .voucherId(orderCreationEvent.getVoucherId())
                .shopId(voucher.getShopId())
                .status(OrderStatutesConstants.UNPAID)
                .build();

        // 第三步：按订单类型走对应创建流程。
        if (orderCreationEvent.getIsSeckillOrder()) {
            createSeckillVoucherOrder(order);
        } else {
            createCommonVoucherOrder(order);
        }
    }

    /**
     * 当订单消息因券信息异常无法落库时，回滚秒杀脚本侧扣减。
     *
     * @param orderCreationEvent 下单消息
     */
    private void rollbackIfSeckill(OrderCreationEvent orderCreationEvent) {
        // 第一步：仅秒杀下单走 Redis 回滚脚本。
        if (!Boolean.TRUE.equals(orderCreationEvent.getIsSeckillOrder())) {
            return;
        }

        // 第二步：回滚 Redis 库存和用户购买记录。
        stringRedisTemplate.execute(SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT,
                Collections.emptyList(),
                orderCreationEvent.getVoucherId().toString(),
                orderCreationEvent.getUserId().toString(),
                orderCreationEvent.getOrderId().toString());
    }

    /**
     * 数据库唯一id，确保幂等性
     * @param voucherOrder 订单
     */
    public void createCommonVoucherOrder(VoucherOrder voucherOrder) {
        try {
            voucherOrderMapper.insert(voucherOrder);
            sentOrderCloseEvent(voucherOrder.getId());
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
            long count = voucherOrderMapper.selectCount(new QueryWrapper<VoucherOrder>().eq("voucher_id", voucherId).eq("user_id", userId));
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
            voucherService.invalidateSingleVoucherCache(voucherId);
            // 保存订单
            voucherOrderMapper.insert(voucherOrder);
            sentOrderCloseEvent(voucherOrder.getId());
        } finally {
            lock.unlock();
        }
    }

    public void sentOrderCloseEvent(Long orderId) {
        Message<OrderCloseEvent> message = MessageBuilder.withPayload(
                OrderCloseEvent.builder().orderId(orderId).build()
        ).build();
        rocketMQTemplate.syncSendDelayTimeSeconds(MQConstants.ORDER_CLOSE_TOPIC,message,CLOSE_TIME_SECONDS);
    }
}
