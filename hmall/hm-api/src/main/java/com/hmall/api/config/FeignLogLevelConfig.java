package com.hmall.api.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;

//注意这里没有@configuration
public class FeignLogLevelConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
