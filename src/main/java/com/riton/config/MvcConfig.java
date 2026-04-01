package com.riton.config;

import com.riton.interceptor.LoginInterceptor;
import com.riton.interceptor.RefreshTokenInterceptor;
import com.riton.interceptor.ShopLoginInterceptor;
import com.riton.interceptor.ShopRefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 注册拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns(
                        "/user/logout",
                        "/user/me",
                        "/user/sign",
                        "/user/sign/count",
                        "/user/password",
                        "/follow/**",
                        "/blog/**",
                        "/blog-comments/**",
                        "/voucher-order/**",
                        "/pay/**",
                        "/optoken/**"
                )
                .excludePathPatterns("/blog/hot")
                .order(1);
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);

        registry.addInterceptor(new ShopLoginInterceptor())
                .addPathPatterns(
                        "/shop/auth/me",
                        "/shop/auth/logout",
                        "/shop/manage/**"
                ).order(3);
        registry.addInterceptor(new ShopRefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/shop/**")
                .excludePathPatterns(
                        "/shop/auth/code",
                        "/shop/auth/login",
                        "/shop/auth/register"
                ).order(2);
    }
}
