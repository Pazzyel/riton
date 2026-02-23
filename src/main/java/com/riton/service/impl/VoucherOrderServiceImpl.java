package com.riton.service.impl;

import com.riton.constants.VoucherDailyLimitConstants;
import com.riton.domain.dto.Result;
import com.riton.domain.entity.SeckillVoucher;
import com.riton.domain.entity.Voucher;
import com.riton.domain.entity.VoucherOrder;
import com.riton.mapper.VoucherMapper;
import com.riton.mapper.VoucherOrderMapper;
import com.riton.constants.MQConstants;
import com.riton.mq.OrderCreationEvent;
import com.riton.service.ISeckillVoucherService;
import com.riton.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.riton.utils.RedisIdWorker;
import com.riton.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

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
@AllArgsConstructor
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    //全局唯一ID生成器
    private RedisIdWorker redisIdWorker;

    private ISeckillVoucherService seckillVoucherService;

    private VoucherMapper voucherMapper;

    private StringRedisTemplate stringRedisTemplate;

    private RocketMQTemplate rocketMQTemplate;

    //秒杀下单的lua脚本，包括检测库存是否充足，检测是否为同一个用户重复下单，扣减库存，添加订单到redis的SET结构，发送到消息队列stream.orders
    //参数： voucherId = ARGV[1] userId = ARGV[2] orderId = ARGV[3]
    //正常返回0，库存不足返回1，重复下单返回2
    private static final DefaultRedisScript<Long> VOUCHER_DAILY_LIMIT_SCRIPT;
    private static final DefaultRedisScript<Long> VOUCHER_DAILY_LIMIT_ROLLBACK_SCRIPT;
    static {
        VOUCHER_DAILY_LIMIT_SCRIPT = new DefaultRedisScript<>();
        VOUCHER_DAILY_LIMIT_SCRIPT.setLocation(new ClassPathResource("lua/voucher_daily_limit.lua"));
        VOUCHER_DAILY_LIMIT_SCRIPT.setResultType(Long.class);

        VOUCHER_DAILY_LIMIT_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        VOUCHER_DAILY_LIMIT_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("lua/voucher_daily_limit_rollback.lua"));
        VOUCHER_DAILY_LIMIT_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> SECKILL_ONE_LIMIT_SCRIPT;
    private static final DefaultRedisScript<Long> SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT;
    static {
        SECKILL_ONE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        SECKILL_ONE_LIMIT_SCRIPT.setLocation(new ClassPathResource("lua/seckill_one_limit.lua"));
        SECKILL_ONE_LIMIT_SCRIPT.setResultType(Long.class);

        SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("lua/seckill_one_limit_rollback.lua"));
        SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> VOUCHER_NO_LIMIT_SCRIPT;
    private static final DefaultRedisScript<Long> VOUCHER_NO_LIMIT_ROLLBACK_SCRIPT;
    static {
        VOUCHER_NO_LIMIT_SCRIPT = new DefaultRedisScript<>();
        VOUCHER_NO_LIMIT_SCRIPT.setLocation(new ClassPathResource("lua/voucher_no_limit.lua"));
        VOUCHER_NO_LIMIT_SCRIPT.setResultType(Long.class);

        VOUCHER_NO_LIMIT_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        VOUCHER_NO_LIMIT_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("lua/voucher_no_limit_rollback.lua"));
        VOUCHER_NO_LIMIT_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    /**
     * 下单秒杀券，注意秒杀券一个用户只能买一次，每日限购无效
     * @param voucherId 秒杀券的id
     * @return 如果下单成功，返回订单id
     */
    @Override
    public Result seckillVoucher(Long voucherId){
        // 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null){
            return Result.fail("秒杀券不存在！");
        }
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

        //以上部分改为消息队列异步实现，用lua脚本执行生产者操作
        Long userId = UserHolder.getUser().getId();
        //直接生成对应的orderId，创建订单逻辑由消费者线程异步执行
        long orderId = redisIdWorker.nextId("order");

        //执行lua脚本
        long result = stringRedisTemplate.execute(SECKILL_ONE_LIMIT_SCRIPT, Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId));
        if (result != 0){
            return Result.fail(result == 1 ? "库存不足！" : "不允许重复下单！");
        }

        OrderCreationEvent message = OrderCreationEvent.builder().voucherId(voucherId).orderId(orderId).userId(userId).isSeckillOrder(true).build();
        try {
            rocketMQTemplate.convertAndSend(MQConstants.ORDER_CREATE_TOPIC, message);
        } catch (MessagingException e) {
            log.warn("下单扣减完成Redis后发送掉消息队列失败，回滚Redis库存,，voucherId = {}, userId = {}, orderId = {}！",voucherId,userId,orderId);
            stringRedisTemplate.execute(SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT, Collections.emptyList(),
                    voucherId.toString(),
                    userId.toString(),
                    String.valueOf(orderId));
            return Result.fail("下单失败");
        }

        return Result.ok(orderId);
    }

    /**
     * 下单普通券，普通券具有每日限购要求
     * @param voucherId 普通券的id
     * @return 如果下单成功，返回订单id
     */
    @Override
    public Result voucher(Long voucherId) {
        // 查询优惠券
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null){
            return Result.fail("该券不存在！");
        }

        //以上部分改为消息队列异步实现，用lua脚本执行生产者操作
        Long userId = UserHolder.getUser().getId();
        //直接生成对应的orderId，创建订单逻辑由消费者线程异步执行
        long orderId = redisIdWorker.nextId("order");
        //执行lua脚本
        long result = 0;
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Long limit = voucher.getDailyLimit();
        if (limit >= 0) {
            result = stringRedisTemplate.execute(VOUCHER_DAILY_LIMIT_SCRIPT, Collections.emptyList() ,
                    voucherId.toString(),
                    userId.toString(),
                    String.valueOf(orderId),
                    formatter.format(now),
                    limit);
        } else if (limit.equals(VoucherDailyLimitConstants.ONLY_ONE_LIMIT)) {
            result = stringRedisTemplate.execute(SECKILL_ONE_LIMIT_SCRIPT, Collections.emptyList() ,
                    voucherId.toString(),
                    userId.toString(),
                    String.valueOf(orderId));
        } else if (limit.equals(VoucherDailyLimitConstants.NO_LIMIT)) {
            result = stringRedisTemplate.execute(VOUCHER_NO_LIMIT_SCRIPT, Collections.emptyList(),
                    voucherId.toString(),
                    userId.toString(),
                    String.valueOf(orderId));
        }

        if (result != 0){
            return Result.fail(result == 1 ? "库存不足！" : "下单量超过每日限额！");
        }

        OrderCreationEvent message = OrderCreationEvent.builder().orderId(orderId).userId(userId).voucherId(voucherId).isSeckillOrder(false).build();
        try {
            rocketMQTemplate.convertAndSend(MQConstants.ORDER_CREATE_TOPIC, message);
        } catch (MessagingException e) {
            log.warn("普通下单扣减完成Redis后发送消息队列失败，回滚Redis库存和购买限制，voucherId = {}, userId = {}, orderId = {}",
                    voucherId, userId, orderId);
            if (limit >= 0) {
                stringRedisTemplate.execute(VOUCHER_DAILY_LIMIT_ROLLBACK_SCRIPT, Collections.emptyList(),
                        voucherId.toString(),
                        userId.toString(),
                        String.valueOf(orderId),
                        formatter.format(now));
            } else if (limit.equals(VoucherDailyLimitConstants.ONLY_ONE_LIMIT)) {
                stringRedisTemplate.execute(SECKILL_ONE_LIMIT_ROLLBACK_SCRIPT, Collections.emptyList(),
                        voucherId.toString(),
                        userId.toString(),
                        String.valueOf(orderId));
            } else if (limit.equals(VoucherDailyLimitConstants.NO_LIMIT)) {
                stringRedisTemplate.execute(VOUCHER_NO_LIMIT_ROLLBACK_SCRIPT, Collections.emptyList(),
                        voucherId.toString(),
                        userId.toString(),
                        String.valueOf(orderId));
            }
            return Result.fail("下单失败");
        }

        return Result.ok(orderId);
    }
}
