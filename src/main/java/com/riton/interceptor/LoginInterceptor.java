package com.riton.interceptor;

import com.riton.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 拦截器方法
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 第一步：校验登录状态。
        if (UserHolder.getUser() == null) {
            //没有用户信息
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 第二步：已登录请求放行。
        return true;
    }
}
