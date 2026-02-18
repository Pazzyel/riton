package com.riton;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.riton.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class RitonApplication {

    public static void main(String[] args) {
        SpringApplication.run(RitonApplication.class, args);
    }

}
