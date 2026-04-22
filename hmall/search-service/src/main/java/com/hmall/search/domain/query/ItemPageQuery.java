package com.hmall.search.domain.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

@Data
@ApiModel(description = "商品分页查询条件")
public class ItemPageQuery{
    @ApiModelProperty("搜索关键字")
    private String key;
    @ApiModelProperty("商品分类")
    private String category;
    @ApiModelProperty("商品品牌")
    private String brand;
    @ApiModelProperty("价格最小值")
    private Integer minPrice;
    @ApiModelProperty("价格最大值")
    private Integer maxPrice;
    @ApiModelProperty("页码")
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNo = 1;
    @ApiModelProperty("页码")
    @Min(value = 1, message = "每页查询数量不能小于1")
    private Integer pageSize = 10;
    @ApiModelProperty("是否升序")
    private Boolean isAsc = true;
    @ApiModelProperty("排序方式")
    private String sortBy;
}
