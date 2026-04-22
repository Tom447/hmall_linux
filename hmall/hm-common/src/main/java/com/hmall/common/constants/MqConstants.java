package com.hmall.common.constants;

public interface MqConstants {
    //用于创建订单时，购物车情况时的配置
    String TRADE_EXCHANGE_NAME = "trade.topic";
    String ORDER_CREATE_KEY = "order.create";
    //用于延迟消息，订单状态改变的配置
    String DELAY_EXCHANGE = "trade.delay.topic";
    String DELAY_ORDER_QUEUE = "trade.order.delay.queue";
    String DELAY_ORDER_ROUTING_KEY = "order.query";

    /*商品上下架*/
    String ITEM_EXCHANGE_NAME = "items.topic";
    String ITEM_UP_KEY = "item.up";
    String ITEM_DOWN_KEY = "item.down";
}
