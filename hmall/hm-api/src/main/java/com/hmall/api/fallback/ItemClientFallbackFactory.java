package com.hmall.api.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {


    @Override
    public ItemClient create(Throwable cause) {
        log.error("远程调用异常", cause);
        return new ItemClient() {

            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                //查询异常, 返回空集合
                log.error("远程调用异常，异常原因:{}",cause);
                return Collections.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                //不抛出异常的话，用户啥也不知道
                throw new BizIllegalException(cause);
            }

            @Override
            public void recoveryStock(List<OrderDetailDTO> items) {
                //不抛出异常的话，用户啥也不知道
                throw new BizIllegalException(cause);
            }
        };
    }
}
