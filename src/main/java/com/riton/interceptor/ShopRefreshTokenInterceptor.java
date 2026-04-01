package com.riton.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.riton.constants.RedisConstants;
import com.riton.domain.dto.ShopAccountDTO;
import com.riton.utils.ShopHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 店铺账号刷新 token 拦截器。
 */
public class ShopRefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public ShopRefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 解析并刷新店铺账号 token。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 第一步：获取并校验 token。
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }

        // 第二步：读取 Redis 中的店铺账号信息。
        String tokenKey = RedisConstants.LOGIN_SHOP_KEY + token;
        Map<Object, Object> shopMap = stringRedisTemplate.opsForHash().entries(tokenKey);
        if (shopMap.isEmpty()) {
            return true;
        }

        // 第三步：保存上下文并刷新 token 有效期。
        ShopAccountDTO shopAccountDTO = BeanUtil.fillBeanWithMap(shopMap, new ShopAccountDTO(), false);
        ShopHolder.saveShop(shopAccountDTO);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_SHOP_TTL, TimeUnit.MINUTES);
        return true;
    }

    /**
     * 请求完成后清理店铺账号上下文。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ShopHolder.removeShop();
    }
}
