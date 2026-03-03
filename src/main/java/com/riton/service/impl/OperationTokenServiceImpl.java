package com.riton.service.impl;

import com.riton.constants.RedisConstants;
import com.riton.domain.dto.Result;
import com.riton.service.OperationTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OperationTokenServiceImpl implements OperationTokenService {

    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    public OperationTokenServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result getOperationToken(String requestPath) {
        // 构建redis Key
        String token = UUID.randomUUID().toString();
        String key = RedisConstants.OPERATION_TOKEN_KEY + token;

        // 插入带过期时间的key
        stringRedisTemplate.opsForValue().set(key,requestPath);
        stringRedisTemplate.expire(key, RedisConstants.OPERATION_TOKEN_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
    }
}
