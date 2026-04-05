package com.hmall.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigInteger;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        SimpleModule simpleModule = new SimpleModule();

        // 将 Long 类型序列化为 String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        // 将基本数据类型 long 也序列化为 String
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        // 将 BigInteger 也序列化为 String，以防万一
        simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);

        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }
}
