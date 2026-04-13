package com.hmall.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.constants.MqConstants;
import com.hmall.common.domain.MultiDelayMessage;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.mq.DelayMessageProcessor;
import com.hmall.common.mq.RelyUserInfoMessageProcessor;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.mapper.OrderMapper;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final RabbitTemplate rabbitTemplate;


    @Override
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
//        order.setUserId(1L);
        //在gateway没有完全搞定登录校验和授权之前userid设置为1，
        //若搞定之后，需要将userId设置为UserContext.getUser()
        order.setUserId(UserContext.getUser());
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

       /* // 3.清理购物车商品
        cartClient.deleteCartItemByIds(itemIds);*/

        // 3.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        //4.清理购物车
        try {
            rabbitTemplate.convertAndSend(
                    MqConstants.TRADE_EXCHANGE_NAME,
                    MqConstants.ORDER_CREATE_KEY,
                    itemIds,
                    new RelyUserInfoMessageProcessor()
            );
        } catch (AmqpException e) {
            log.error("清理购物车的消息发送异常", e);
        }
        //5.延迟检查订单状态的消息
        try {
          /*  MultiDelayMessage<Long> msg = MultiDelayMessage.of(
                    order.getId(),
                    10000L, 10000L, 10000L,
                    15000L, 15000L,
                    30000L, 30000L,
                    60000L, 60000L,
                    120000L,
                    300000L,
                    600000L,
                    600000L
            );*/
            //TODO 先延迟5s
            MultiDelayMessage<Long> msg = MultiDelayMessage.of(
                    order.getId(),
                    20000L,
                    20000L
            );
            System.out.println("发送延时消息，订单号是:" + order.getId());
            rabbitTemplate.convertAndSend(
                    MqConstants.DELAY_EXCHANGE,
                    MqConstants.DELAY_ORDER_ROUTING_KEY,
                    msg,
                    new DelayMessageProcessor(msg.removeNextDelay().intValue())
            );
        } catch (AmqpException e) {
            log.error("延迟消息检查异常", e);
        }
        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @GlobalTransactional
    public void cancleOrder(Long orderId) {
        // 取消订单
        lambdaUpdate()
                .set(Order::getStatus, 5)
                .set(Order::getCloseTime, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .update();
        //恢复库存
        //1.先得到需要恢复库存的商品id和对应的商品数量的OrderDetailDTO
        List<OrderDetail> orderDetailList = detailService.lambdaQuery().eq(OrderDetail::getOrderId, orderId).list();
        List<OrderDetailDTO> dtoList = orderDetailList.stream()
                .map(orderDetail -> BeanUtils.copyBean(orderDetail, OrderDetailDTO.class))
                .collect(Collectors.toList());
        itemClient.recoveryStock(dtoList);
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
