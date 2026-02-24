package com.riton.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.riton.constants.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);//线程池，用于加开线程

    @Autowired
    public CacheClient(RedissonClient redissonClient, StringRedisTemplate redisTemplate) {
        this.redissonClient = redissonClient;
        this.stringRedisTemplate = redisTemplate;
    }

    /**
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
     * @param key 储存于Redis的key
     * @param value 储存于Redis的value，以json字符串的形式
     * @param time 过期时间
     * @param unit 过期时间单位
     */
    private void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JsonUtils.writeValueAsString(value), time, unit);
    }

    /**
     * 直接移除某个key对应的缓存
     * @param key 缓存key
     */
    private void remove(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 使某个key的缓存无效
     * @param keyPrefix key前缀
     * @param id key的id
     * @param <ID> id类型，例如Long
     */
    public <ID> void invalidate(String keyPrefix, ID id) {
        String key = keyPrefix + id;
        remove(key);
    }

    /**
     * 逻辑过期解决缓存击穿
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
     * @param key 储存于Redis的key
     * @param value 储存于Redis的value，以json字符串的形式
     * @param time 逻辑过期时间
     * @param unit 过期时间单位
     */
    //原saveShop2Redis方法
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JsonUtils.writeValueAsString(redisData));
    }

    /**
     * 查询操作，缓存不命中时查询数据库并建立缓存
     * 根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
     * @param keyPrefix 缓存key的前缀
     * @param id 缓存条目的id
     * @param type 查询数据的类型
     * @param dbFallback 进行数据库调用的方法
     * @param time 过期时间
     * @param unit 过期时间单位
     * @return 查询结果
     * @param <R> 查询数据的类型泛型
     * @param <ID> id的类型泛型
     */
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, TypeReference<R> type,
                                         Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)){
            //已经缓存且有数据，直接返回
            return JsonUtils.readValue(json, type);
        }
        //如果命中的是空数据
        if(json != null){
            //json不是null说明redis已经缓存，是blank说明是空数据
            //返回已被缓存的空数据
            return null;
        }

        //没有命中，回调数据库操作函数
        R r = dbFallback.apply(id);
        if(r == null){
            //不存在这条数据，返回null
            //向redis存入空数据
            stringRedisTemplate.opsForValue().set(key, "",
                    RedisConstants.CACHE_NULL_TTL + RedisConstants.randomTTL(), TimeUnit.MINUTES);
            return null;
        }
        //有数据，缓存入redis
        stringRedisTemplate.opsForValue().set(key, JsonUtils.writeValueAsString(r), time, unit);
        return r;
    }

    /**
     * 根据指定的key查询缓存，并反序列化为指定类型，需要利用互斥锁解决缓存击穿问题
     * @param keyPrefix 缓存key的前缀
     * @param id 缓存条目的id
     * @param type 查询数据的类型
     * @param dbFallback 进行数据库调用的方法
     * @param time 过期时间
     * @param unit 过期时间单位
     * @return 查询结果
     * @param <R> 查询数据的类型泛型
     * @param <ID> id的类型泛型
     */
    public <ID,R> R queryWithMutex(String keyPrefix, ID id, TypeReference<R> type,
                                   Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String resultJSON = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(resultJSON)){//Redis命中
            return JsonUtils.readValue(resultJSON, type);
        }
        //判断value是否为空
        if(resultJSON != null){
            //返回缓存的空数据
            return null;
        }

        //缓存未命中，先尝试获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;//对应同一个商铺id，需要同一把锁
        RLock lock = redissonClient.getLock(lockKey);
        R r = null;//查询结果
        try {
            if (!lock.tryLock(time, unit)) {
                //获取锁失败
                Thread.sleep(50);
                return queryWithMutex(keyPrefix,id,type,dbFallback,time,unit);//递归尝试获取锁
            }

            //双锁检测防止重复重建
            resultJSON = stringRedisTemplate.opsForValue().get(key);
            if(StrUtil.isNotBlank(resultJSON)){//Redis命中
                return JsonUtils.readValue(resultJSON, type);
            }
            //判断value是否为空
            if(resultJSON != null){
                //返回缓存的空数据
                return null;
            }

            //成功抢到锁
            r = dbFallback.apply(id);//不命中，从数据库查询
            if (r == null) {
                //缓存穿透，我们缓存空数据
                stringRedisTemplate.opsForValue().set(key, "",
                        RedisConstants.CACHE_NULL_TTL + RedisConstants.randomTTL(), TimeUnit.MINUTES);
                return null;
            }
            //有数据，存入redis
            this.set(key, r, time, unit);//缓存到Redis，设置过期时间
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return r;//返回查询的数据
    }

    /**
     * 逻辑过期主要用于热点数据，会在redis预先缓存这些热点key
     * 根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
     * WARN: 此方法可能无法适应List<T>类型的数据
     * @param keyPrefix 缓存key的前缀
     * @param id 缓存条目的id
     * @param type 查询数据的类型
     * @param dbFallback 进行数据库调用的方法
     * @param time 过期时间
     * @param unit 过期时间单位
     * @return 查询结果
     * @param <R> 查询数据的类型泛型
     * @param <ID> id的类型泛型
     */
    public <ID,R> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type,
                                           Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String resultJSON = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(resultJSON)){
            //redis未命中说明不是热点数据
            return null;
        }

        //命中，反序列化为对象
        RedisData redisData = JSONUtil.toBean(resultJSON, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);

        //判断有效期
        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            //还没有过期，直接返回新数据
            return r;
        }

        //已经过期，尝试获取锁重建缓存
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        RLock lock = redissonClient.getLock(lockKey);
        boolean getLock = lock.tryLock();
        if(getLock){
            //在线程池中加入重建缓存的线程
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R newR = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, newR, time, unit);//Lambda表达式的this是外围类的实例
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            });//使用Lambda代替Runnable匿名内部类
        }
        //返回过期信息
        return r;
    }

    /**
     * 利用Redis实现互斥锁
     * @param lockKey 互斥锁的key
     * @return 是否获取到锁
     */
    private boolean tryLock(String lockKey) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放对应的key的锁
     * @param lockKey 互斥锁的key
     */
    private void unlock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }
}
