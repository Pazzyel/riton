package com.riton.redis;

import com.riton.enums.RedisKeyEnum;
import com.riton.utils.SpringUtils;

@Deprecated
public class RedisKeyBuilder {
    /**
     * 实际使用的key
     * */

    /**
     * 构建真实的key
     * @param redisKeyEnum key的枚举
     * @param args 占位符的值
     * */
    public static String createRedisKey(RedisKeyEnum redisKeyEnum, Object... args){
        String redisRelKey = String.format(redisKeyEnum.getKey(),args);
        return SpringUtils.getPrefixDistinctionName() + "-" + redisRelKey;
    }
}
