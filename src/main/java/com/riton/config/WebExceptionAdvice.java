package com.riton.config;

import com.riton.dto.Result;
import com.riton.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        if (e instanceof BaseException) {
            return Result.fail(e.getMessage());
        } else {
            log.error(e.toString(), e);
            return Result.fail("服务器异常");
        }
    }
}
