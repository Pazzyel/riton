package com.riton.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissionConfig {
    @Value("redis://" + "${spring.redis.host}" + ":" + "${spring.redis.port}")
    private String host;
    @Value("${spring.redis.password}")
    private String password;

    @Bean
    public RedissonClient createRedissonClient() {
        //配置Redisson分布式锁
        Config config = new Config();
        config.useSingleServer().setAddress(host).setPassword(password);
        RedissonClient r = Redisson.create(config);
        return r;
    }
}
