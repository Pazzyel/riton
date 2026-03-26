package com.riton.config;

import com.uber.h3core.H3Core;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
public class H3Config {

    @Bean
    public H3Core h3Core() {
        try {
            return H3Core.newInstance();
        } catch (IOException e) {
            log.error("初始化H3Core失败, {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
