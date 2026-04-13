package com.hmall.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("pay-service")
public interface PayClient {

    // 路径必须与 Controller 的 @GetMapping("{id}") 拼接后一致
    // 假设 Controller 类上有 @RequestMapping("/pay-orders")
    @GetMapping("/pay-orders/{id}")
    // 重点修改：这里必须用 String 接收，以便 Feign 传递数据后，Controller 能进行 replace("\"", "") 处理
    public boolean isPayByOrderId(@PathVariable("id") String orderId);
}