package com.hmall.pay.controller;

import com.hmall.common.exception.BizIllegalException;
import com.hmall.pay.domain.dto.PayApplyDTO;
import com.hmall.pay.domain.dto.PayOrderFormDTO;
import com.hmall.pay.enums.PayType;
import com.hmall.pay.service.IPayOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

@Api(tags = "支付相关接口")
@RestController
@RequestMapping("pay-orders")
@RequiredArgsConstructor
@Slf4j
public class PayController {

    private final IPayOrderService payOrderService;

    @ApiOperation("生成支付单")
    @PostMapping
    public String applyPayOrder(@RequestBody PayApplyDTO applyDTO){
        if(!PayType.BALANCE.equalsValue(applyDTO.getPayType())){
            // 目前只支持余额支付
            throw new BizIllegalException("抱歉，目前只支持余额支付");
        }
        return payOrderService.applyPayOrder(applyDTO);
    }

    @ApiOperation("尝试基于用户余额支付")
    @ApiImplicitParam(value = "支付单id", name = "id")
    @PostMapping("{id}")
    public void tryPayOrderByBalance(@PathVariable("id") Long id, @RequestBody PayOrderFormDTO payOrderFormDTO){
        payOrderFormDTO.setId(id);
        payOrderService.tryPayOrderByBalance(payOrderFormDTO);
    }

    @ApiOperation("是否已支付")
    @ApiImplicitParam(value = "订单id", name = "id")
    // 方式1：使用 @RequestParam，这样即使 Body 里有错数据也不会影响
    @GetMapping("{id}")
    public boolean isPayByOrderId(@Param("订单id") @PathVariable("id") String id){
        System.out.println(id);
        // 这里的 id 可能会带引号，也可能不带，为了保险，去除所有引号
        String cleanId = id.replace("\"", "");
        // 再转回 Long 给业务层使用
        Long orderId = Long.parseLong(cleanId);
        if (orderId == null) {
            // 这里可以打印一个 Warn 日志，或者直接返回 false
            log.warn("Invalid request parameter for isPay");
            return false;
        }
        return payOrderService.isPayByOrderId(orderId);
    }

}
