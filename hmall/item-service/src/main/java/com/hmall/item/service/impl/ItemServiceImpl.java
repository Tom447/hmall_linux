package com.hmall.item.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;

import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.mapper.ItemMapper;
import com.hmall.item.service.IItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    @Override
    @Transactional // 1. 加上事务，保证要么全成功，要么全失败
    public void deductStock(List<OrderDetailDTO> items) {
        // 2. 直接遍历调用 Mapper 方法，不要用 executeBatch 这种容易出错的底层操作

        for (OrderDetailDTO item : items) {
            // 调用你在 ItemMapper 中定义的 @Update 方法
            int updated = baseMapper.updateStock(item);

            // 3. 判断更新结果
            if (updated == 0) {
                // 如果影响行数为 0，说明 WHERE 条件没匹配上（库存不足）
                throw new BizIllegalException("库存不足！商品ID: " + item.getItemId());
            }
        }
    }

    @Override
    public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
        return BeanUtils.copyList(listByIds(ids), ItemDTO.class);
    }

    @Override
    @Transactional // 记得加事务
    public void recoveryStock(List<OrderDetailDTO> items) {
        for (OrderDetailDTO dto : items) {
            // 使用 UpdateWrapper 实现 SET stock = stock + num
            lambdaUpdate()
                    .eq(Item::getId, dto.getItemId()) // 条件：指定商品
                    .setSql("stock = stock + " + dto.getNum())      // 操作：库存累加 (MyBatis-Plus 不直接支持表达式，需用 setSql)
                    .update();
        }
    }
}