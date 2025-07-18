package com.hmdp.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 拦截器方法
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取session
        HttpSession session = request.getSession();
        //获取用户
        Object user = session.getAttribute("user");
        if (user == null) {
            //没有用户信息
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        //有信息，保存到ThreadLocal
        UserHolder.saveUser((UserDTO) user);
        return true;
    }
}
