package com.hmall.trade.listeners;

import com.hmall.api.client.PayClient;
import com.hmall.common.constants.MqConstants;
import com.hmall.common.domain.MultiDelayMessage;
import com.hmall.common.mq.DelayMessageProcessor;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OrderStatusCheckListener {

    private final IOrderService orderService;
    private final RabbitTemplate rabbitTemplate;
    private final PayClient payClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.DELAY_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(value = MqConstants.DELAY_EXCHANGE, delayed = "true", type = ExchangeTypes.TOPIC),
            key = MqConstants.DELAY_ORDER_ROUTING_KEY
    ))
    public void listenOrderDelayMessage(MultiDelayMessage<Long> msg){
        //1. 查询订单状态
        Order order = orderService.getById(msg.getData());
        if (order == null || order.getStatus() == 2){
        //2 订单不存在，或者已支付
            return;
        }
        boolean isPay = payClient.isPayByOrderId(order.getId().toString());
        //3.1 如果已支付，标记订单状态为已支付
        if (isPay){
            orderService.markOrderPaySuccess(order.getId());
            System.out.println("已支付，标记订单状态为已支付");
            return;
        }

        //4. 判断是否存在，延迟时间
        if (msg.hasNextDelay()){
            //4.1存在， 重发延迟消息
            Long nextDelay = msg.removeNextDelay();
            rabbitTemplate.convertAndSend(
                    MqConstants.DELAY_EXCHANGE,
                    MqConstants.DELAY_ORDER_ROUTING_KEY,
                    msg,
                    new DelayMessageProcessor(nextDelay.intValue())
            );
            System.out.println("延迟时间存在，延迟时间:" + nextDelay);
            return;
        }
        //5 不存在，取消订单
        System.out.println("延迟时间不存在，延迟时间,取消订单:" + order.getId());
        orderService.cancleOrder(order.getId());

    }

}
