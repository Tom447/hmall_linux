package com.hmall.trade;

import com.hmall.api.config.FeignLogLevelConfig;
import com.hmall.common.config.JacksonConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@Import(JacksonConfig.class)
@EnableFeignClients(basePackages = "com.hmall.api.client", defaultConfiguration = FeignLogLevelConfig.class)
@MapperScan("com.hmall.trade.mapper")
@SpringBootApplication
public class TradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }
}
