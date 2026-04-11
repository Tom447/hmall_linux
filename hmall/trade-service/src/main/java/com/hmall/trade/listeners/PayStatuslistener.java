package com.hmall.trade.listeners;


import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class PayStatuslistener {
    private final IOrderService orderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "mark.order.pay.queue",durable = "true"),
            exchange = @Exchange(name = "pay.topic",type = ExchangeTypes.TOPIC),
            key = "pay.success"
    ))
    public void listenOrderPay(Long orderId){
        //1.查询订单
        Order order = orderService.getById(orderId);
        //2.判断订单状态是否为未支付
        /*
        *  order.getStatus() == 2，不写这个的原因是，比如用户是已退款的状态，那么
        * 直接判断 ==2 ？结果！= 2，那么就会改为已支付。那么就把退款的订单又变成已退款，那么就再退一次钱
        * 那么就再退一次钱。所以改为order.getStatus() != 1
        * */
        if (order == null || order.getStatus() != 1){
            //订单不存在，或者状态异常
            /*
            * 注意这里不要抛异常，一旦抛异常，mq就要重新投递，那么就死循环了，所以直接return即可
            * */
            return;
        }
        //3.如果未支付，再去标记成已支付。
        System.out.println("PayStatuslistener监听到了:" + orderId);
        orderService.markOrderPaySuccess(orderId);

        /*
        * 整个过程非常类似乐观锁
        * 1. 我先查询订单
        * 2. 我期待这个订单的状态是 1
        * 3. 你不是1了我就不管了，你不是1了我就再去做mark
        *
        * 但是这里有并发安全问题：
        *   假如说连续收到了2条消息是一起来的（注意是同一个订单号的两条消息），查的那一刻，订单状态是未支付。
        *   假如有两个线程，都来判断，都发现是未支付，于是都往下走，然后就会并行修改。
        *
        *所以就出现了两个并发线程修改同一个订单的问题：
        *   两个线程都进入了修改逻辑。虽然最终结果都是“已支付”，
        *   但这违背了“幂等性”的初衷（比如如果后面有“加积分”的逻辑，积分就会被加两次）。
        * 注意：这里谈的并发安全问题：这 2 条消息的内容（Payload）通常是一模一样的，包含的也是同一个订单号。
        * */

        //update order set status = 2 where id = ？ and status = 1 那么这一行代码就等于上面的三行, 就保证了业务幂等
        orderService.lambdaUpdate()
                .set(Order::getStatus, 2)
                .set(Order::getUpdateTime, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)
                .update();
    }
}
