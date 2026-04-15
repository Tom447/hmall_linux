package com.hmall.item.domain.dto;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class ItemDocDTO {
    /**
     * 商品id
     */
    private Long id;
    /**
     * SKU名称
     */
    private String name;
    /**
     * 价格（分）
     */
    private Integer price;
    /**
     * 库存数量
     */
    private Integer stock;
    /**
     * 商品图片
     */
    private String image;
    /**
     * 类目名称
     */
    private String category;
    /**
     * 品牌名称
     */
    private String brand;
    /**
     * 销量
     */
    private Integer sold;
    /**
     * 评论数
     */
    private Integer commentCount;
    /**
     * 是否是推广广告，true/false
     */
    @TableField("isAD")
    private Boolean isAD;
}
