package com.hmall.api.client;

import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.fallback.ItemClientFallbackFactory;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@FeignClient(value = "item-service", fallbackFactory = ItemClientFallbackFactory.class)
public interface ItemClient {
    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids")Collection<Long> ids);

    @ApiOperation("批量扣减库存")
    @PutMapping("/items/stock/deduct")
    void deductStock(@RequestBody List<OrderDetailDTO> items);


    @ApiOperation("恢复商品库存")
    @PutMapping("/items/stock/recovery")
    public void recoveryStock(@RequestBody List<OrderDetailDTO> items);

    @ApiOperation("根据id查商品")
    @GetMapping("/items/{id}")
    public ItemDTO queryItemById(@PathVariable("id") Long id);
}
