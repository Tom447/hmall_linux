package com.hmall.common.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.retry.enabled", havingValue = "true")
public class ErrorConfiguration {

    // 注入当前微服务的名称，例如：hmall-item
    @Value("${spring.application.name}")
    private String serviceName;

    /**
     * 定义错误消息交换机
     * 注意：交换机通常可以是全局共用的，也可以根据需求拼接 serviceName
     */
    @Bean
    public DirectExchange errorExchange() {
        return new DirectExchange("error.direct");
    }

    /**
     * 定义错误消息队列
     * 修改点：使用 serviceName 拼接队列名称，实现隔离
     */
    @Bean
    public Queue errorQueue() {
        // 格式：error.queue.微服务名称
        String queueName = "error.queue." + serviceName;
        log.debug("创建错误队列: {}", queueName);
        return new Queue(queueName);
    }

    /**
     * 将错误队列绑定到错误交换机
     * 修改点：RoutingKey 也可以拼接 serviceName，确保消息路由到正确的队列
     */
    @Bean
    public Binding errorBinding(Queue errorQueue, DirectExchange errorExchange) {
        // 这里使用拼接后的 routingKey，或者保持固定 key 取决于你的路由策略
        // 方案 A：每个服务独立 RoutingKey (推荐，避免冲突)
        String routingKey = "error." + serviceName;

        return BindingBuilder.bind(errorQueue)
                .to(errorExchange)
                .with(routingKey);
    }

    /**
     * 定义消息恢复器
     * 当重试耗尽后，将消息发往 error.direct 交换机
     */
    @Bean
    public RepublishMessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        log.debug("加载 RepublishMessageRecoverer");
        // 这里只需要指定交换机，RoutingKey 会在 Binding 中处理
        // 注意：RepublishMessageRecoverer 发送消息时也可以指定 routingKey
        // 如果希望所有失败消息都走上面的 binding 逻辑，这里可以写死或者动态获取
        return new RepublishMessageRecoverer(rabbitTemplate, "error.direct", "error." + serviceName);
    }
}