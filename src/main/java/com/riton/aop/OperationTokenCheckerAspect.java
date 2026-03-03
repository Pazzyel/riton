package com.riton.aop;

import com.riton.annotations.RequireTokenCheck;
import com.riton.constants.Constants;
import com.riton.constants.RedisConstants;
import com.riton.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class OperationTokenCheckerAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Before("execution(* com.riton.controller..*(..)) && @annotation(requireTokenCheck)")
    public void doBefore(JoinPoint joinPoint, RequireTokenCheck requireTokenCheck) {
        // 提取请求的token和uri
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String operationToken = request.getHeader(Constants.OPERATION_TOKEN_HEADER);
        String uri = request.getRequestURI();

        // 校验Token
        if (operationToken == null || operationToken.isEmpty()) {
            throw new BusinessException("无效的请求！");
        }
        String key = RedisConstants.OPERATION_TOKEN_KEY + operationToken;
        String savedURI = stringRedisTemplate.opsForValue().get(key);
        if (savedURI == null) {
            throw new BusinessException("请求重复或会话已过期，请重新打开页面！");
        }
        if (!savedURI.equals(uri)) {
            System.out.println("uri:" + uri + " savedURI:" + savedURI);
            throw new BusinessException("无效的请求！");
        }

        // 校验成功，删除token
        stringRedisTemplate.delete(key);
    }
}
