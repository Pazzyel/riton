package com.hmdp.lock;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    //锁的名称
    private final String name;

    private final StringRedisTemplate redisTemplate;
    //锁在redis的key前缀
    private static final String KEY_PREFIX = "lock:";
    //线程的id前缀
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";


    public SimpleRedisLock(String name, StringRedisTemplate redisTemplate) {
        this.name = name;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取锁
     * @param timeoutSec 锁持有的超时时间，持有锁超过这个时间后自动释放
     * @return
     */
    @Override
    public boolean tryLock(Long timeoutSec) {
        String threadId = ID_PREFIX + Thread.currentThread().getId();//记录上锁的线程
        Boolean res = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(res);
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        // 获取线程标示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中的标示
        String id = redisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 判断标示是否一致，不一致说明锁已经超时释放，现在的锁不是自己的
        if(threadId.equals(id)) {
            // 释放锁
            redisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
