package com.hmall.search.controller;


import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.PageVO;
import com.hmall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {


    private final ISearchService searchService;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageVO<ItemDoc> search(ItemPageQuery query) throws IOException {
        return searchService.search(query);
    }

    @ApiOperation("查询搜索到的结果中的过滤项")
    @PostMapping("/filters")
    public Map<String, List<String>> getFilters(@RequestBody ItemPageQuery query) {
        return searchService.getFilters(query);
    }
}
