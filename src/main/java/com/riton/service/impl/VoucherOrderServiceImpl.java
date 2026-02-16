package com.riton.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.riton.dto.Result;
import com.riton.entity.SeckillVoucher;
import com.riton.entity.VoucherOrder;
import com.riton.mapper.VoucherOrderMapper;
import com.riton.service.ISeckillVoucherService;
import com.riton.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.riton.utils.RedisIdWorker;
import com.riton.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    //全局唯一ID生成器
    private RedisIdWorker redisIdWorker;

    private ISeckillVoucherService seckillVoucherService;

    private StringRedisTemplate stringRedisTemplate;
    //Redission分布式锁客户端
    private RedissonClient redissonClient;
    //秒杀下单的lua脚本，包括检测库存是否充足，检测是否为同一个用户重复下单，扣减库存，添加订单到redis的SET结构，发送到消息队列stream.orders
    //参数： voucherId = ARGV[1] userId = ARGV[2] orderId = ARGV[3]
    //正常返回0，库存不足返回1，重复下单返回2
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    public VoucherOrderServiceImpl(RedisIdWorker redisIdWorker, ISeckillVoucherService seckillVoucherService, StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient) {
        this.redisIdWorker = redisIdWorker;
        this.seckillVoucherService = seckillVoucherService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
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

//        // 判断库存是否充足
//        if (voucher.getStock() < 1) {
//            // 库存不足
//            return Result.fail("库存不足！");
//        }
//        // 一人一单逻辑
//        Long userId = UserHolder.getUser().getId();
//
//        //ILock lock = new SimpleRedisLock("order:" + userId,stringRedisTemplate);
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        if(!lock.tryLock()) {
//            //没抢到锁说明重复下单
//            return Result.fail("不允许重复下单！");
//        }
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();//获取代理类
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }

        //以上部分改为消息队列异步实现，用lua脚本执行生产者操作
        Long userId = UserHolder.getUser().getId();
        //直接生成对应的orderId，创建订单逻辑由消费者线程异步执行
        long orderId = redisIdWorker.nextId("order");
        //执行lua脚本
        long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(),voucherId.toString(),userId.toString(),String.valueOf(orderId));
        if (result != 0){
            return Result.fail(result == 1 ? "库存不足！" : "不允许重复下单！");
        }
        //添加到消息队列的逻辑已经在lua脚本中执行
        return Result.ok(orderId);
    }

//    /**
//     * 根据秒杀券id尝试创建订单
//     * @param voucherId 秒杀券id
//     * @return 成功返回订单id
//     */
//    @Transactional
//    @Override
//    public Result createVoucherOrder(Long voucherId) {
//        Long userId = UserHolder.getUser().getId();
//        int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
//        if (count > 0) {
//            return Result.fail("用户已经购买过一次！");
//        }
//
//        // 扣减库存,乐观锁解决，在更新库存的时候检查库存是否>0防止超卖
//        boolean success = seckillVoucherService.update()
//                .setSql("stock= stock -1")
//                .eq("voucher_id", voucherId).gt("stock", 0).update();
//        if (!success) {
//            //扣减失败
//            return Result.fail("库存不足！");
//        }
//        // 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 订单id
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        // 用户id
//        voucherOrder.setUserId(userId);
//        // 代金券id
//        voucherOrder.setVoucherId(voucherId);
//        save(voucherOrder);
//
//        return Result.ok(orderId);
//    }

    //消费者线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * 在类加载时启动消费者组线程
     */
    @PostConstruct
    private void inti() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHanlder());
    }

    /**
     * 处理优惠券订单消息的异步线程
     */
    private class VoucherOrderHanlder implements Runnable {

        /**
         * 循环从消息队列读取订单并保存
         */
        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    //XGROUP CREATE stream.orders g1 $ MKSTREAM //必须事先创建对应的消费者组，不然会报错
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                    );//获取队列s1的组g1的消费者c1的1条消息，最多阻塞2s，最新的未读信息，手动ACK
                    //2.判断订单信息是否为空，如果是，则为没有消息，重新读取
                    if (list == null || list.isEmpty()) {
                        continue;
                    }
                    //解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> map = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(map, new VoucherOrder(), true);
                    //由于存入消息队列时已经把所有信息投入，这里解析的就是完整的订单类
                    //3.创建订单
                    createVoucherOrder(voucherOrder);
                    //4.确认消息XACK
                    stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());//确认队列s1的组g1对应ID的消息
                } catch (Exception e) {
                    log.error("处理订单异常：", e);
                    //处理异常信息
                    handlePendingList();
                }
            }
        }

        /**
         * 循环处理异常的订单
         */
        private void handlePendingList() {
            while(true) {
                try {
                    // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create("stream.orders", ReadOffset.from("0"))
                    );//获取队列s1的组g1的消费者c1的1条消息，最多阻塞2s，第一条信息，手动ACK
                    //2.判断订单信息是否为空，如果是，则为没有消息
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有异常消息，结束循环
                        break;
                    }
                    //解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> map = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(map, new VoucherOrder(), true);
                    //3.创建订单
                    createVoucherOrder(voucherOrder);
                    //4.确认消息XACK
                    stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());//确认队列s1的组g1对应ID的消息
                } catch (Exception e) {
                    log.error("处理pendding订单异常：", e);
                    //处理异常信息
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 根据消息队列的秒杀券消息保存到数据库
     * @param voucherOrder 待保存的优惠券订单消息
     */
    @Override
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long voucherId = voucherOrder.getVoucherId();
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        if(!lock.tryLock()){
            log.error("不允许重复下单");
        }

        try {
            int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
            if (count > 0) {
                log.error("用户已经购买过一次！");
            }

            // 扣减库存,乐观锁解决，在更新库存的时候检查库存是否>0防止超卖
            boolean success = seckillVoucherService.update()
                    .setSql("stock= stock -1")
                    .eq("voucher_id", voucherId).gt("stock", 0).update();
            if (!success) {
                //扣减失败
                log.error("库存不足！");
            }
            // 保存订单
            save(voucherOrder);
        } finally {
            lock.unlock();
        }
    }
}
