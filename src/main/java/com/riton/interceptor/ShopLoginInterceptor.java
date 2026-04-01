package com.riton.interceptor;

import com.riton.utils.ShopHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 店铺账号登录校验拦截器。
 */
public class ShopLoginInterceptor implements HandlerInterceptor {

    /**
     * 校验店铺账号是否已登录。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 第一步：读取店铺账号上下文。
        if (ShopHolder.getShop() == null) {
            // 第二步：未登录直接拦截。
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }
}
