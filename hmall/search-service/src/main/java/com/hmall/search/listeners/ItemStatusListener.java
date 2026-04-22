package com.hmall.search.listeners;

import com.hmall.common.constants.MqConstants;
import com.hmall.search.service.ISearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItemStatusListener {

    private final ISearchService searchService;


    /*上架*/
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.item.up.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.ITEM_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstants.ITEM_UP_KEY
    ))
    public void listenItemUp(Long itemId){
        System.out.println("ItemStatusListener listenItemUp监听到了:" + itemId);
        searchService.saveItemById(itemId);
    }


    /*下架*/
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.item.down.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.ITEM_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstants.ITEM_DOWN_KEY
    ))
    public void listenItemDown(Long itemId){
        System.out.println("ItemStatusListener listenItemDown监听到了:" + itemId);
        searchService.deleteItemById(itemId);
    }
}
