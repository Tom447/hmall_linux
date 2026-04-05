package com.hmall.api.config;

import com.hmall.api.interceptors.UserInfoInterceptor;
import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

//注意这里没有@configuration
public class FeignLogLevelConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return new UserInfoInterceptor();
    }
}
