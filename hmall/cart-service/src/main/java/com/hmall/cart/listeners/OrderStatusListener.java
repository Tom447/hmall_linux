package com.hmall.cart.listeners;

import com.hmall.cart.service.ICartService;
import com.hmall.common.constants.MqConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
@AllArgsConstructor
public class OrderStatusListener {


    private final ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "cart.clear.queue"),
            exchange = @Exchange(name = MqConstants.TRADE_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstants.ORDER_CREATE_KEY
    ))
    //TODO 清空 购物车监听器
    public void listenOrderCreate(Collection<Long> itemIds){
        log.warn("清空 购物车监听器， 清空{}", itemIds);
        itemIds.stream().forEach(itemId ->{
            System.out.println(itemId);
        });
        cartService.removeByItemIds(itemIds);
    }
}
