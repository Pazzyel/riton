package com.riton.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissionConfig {
    @Value("redis://" + "${spring.data.redis.host}" + ":" + "${spring.data.redis.port}")
    private String host;
    @Value("${spring.data.redis.password}")
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
